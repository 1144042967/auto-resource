package cn.sd.jrz.autoresource.compat.create;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 水车马达实体（仅当机械动力 Create 加载时使用）。
 * <p>
 * 作为 Create 动力源：转速由机内水车数量决定（每个水车 +1 RPM、每个大水车 +4 RPM），
 * 应力容量按水车数量累加（水车 256 SU/个、大水车 512 SU/个）。未放入水车时不产生转速。
 * 旋转方向开关、六面输出方向通过 GUI 完成并持久化到 NBT。
 */
public class WaterWheelMotorEntity extends GeneratingKineticBlockEntity implements MenuProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** 水车槽位数量（单个槽，可放一组水车/大水车） */
    public static final int SLOT_COUNT = 1;
    /** 每个水车增加的转速 */
    public static final int SMALL_WATER_WHEEL_SPEED = 1;
    /** 每个大水车增加的转速 */
    public static final int LARGE_WATER_WHEEL_SPEED = 4;
    /** 每个水车提供的应力容量 */
    public static final float WATER_WHEEL_SU = 256f;
    /** 每个大水车提供的应力容量 */
    public static final float LARGE_WATER_WHEEL_SU = 512f;

    /** 水车槽位（单个槽，仅接受水车/大水车，可放一组） */
    public final ItemStackHandler wheelSlots = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return isWaterWheel(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // 转速/应力容量变化：通知网络重新计算
            updateGeneratedRotation();
            // 兜底1：Create 的 updateGeneratedRotation 内部在 hasNetwork() && 转速≠0 的守卫下
            // 才调用 notifyStressCapacityChange 推送容量，网络重挂（detach/attach）后可能被跳过，
            // 导致后续放入/取出水车时应力容量不更新。这里显式把最新容量与应力同步到网络。
            if (level != null && !level.isClientSide && hasNetwork()) {
                KineticNetwork network = getOrCreateNetwork();
                network.updateCapacityFor(WaterWheelMotorEntity.this, totalCapacity());
                network.updateStressFor(WaterWheelMotorEntity.this, calculateStressApplied());
                network.updateStress();
            }
            // 兜底2：强制重建动力传播。
            // Create 的 updateGeneratedRotation → applyNewSpeed 在"已有网络、非0→非0"时走
            // detach/setSpeed/attach 分支，而 propagateNewSource 对网络 ID 相同的成员不重播转速
            // （同网络短路），因此水车数量增加后新转速只反映在 GUI、不传播到下游
            // （从无到有/改方向/改转向因网络状态不同而恰好生效）。这里把本机彻底脱离网络并清零
            // 自身速度，再调 updateGeneratedRotation 走 applyNewSpeed 的 previous==0 分支
            // （setSpeed + setNetwork + attachKinetics），令 propagateNewSource 判定本机与下游
            // 网络不同而重新传播最新转速。
            if (level != null && !level.isClientSide) {
                float generated = getGeneratedSpeed();
                LOGGER.info("[WaterWheelMotor] slot变化 slot={} count={} current={} generated={} last={} speedField={} hasNet={} net={}",
                        slot, wheelSlots.getStackInSlot(0).getCount(), currentSpeed(), generated,
                        lastPropagatedGenerated, getSpeed(), hasNetwork(), network);
                if (generated != lastPropagatedGenerated) {
                    lastPropagatedGenerated = generated;
                    detachKinetics();
                    setNetwork(null);
                    setSpeed(0);
                    updateGeneratedRotation();
                }
                // 兜底3：置位 updateSpeed，让下一 tick 的 attachKinetics 以最新转速重新传播到整个动力网络。
                updateSpeed = true;
            }
        }
    };
    /** 是否逆时针（默认顺时针=false） */
    public boolean counterClockwise = false;
    /**
     * 上次已成功传播到动力网络的生成速度（运行时缓存，用于检测水车槽内容变化）。
     * <p>
     * Create 的 {@code GeneratingKineticBlockEntity.updateGeneratedRotation} 在"已有网络、非0→非0"
     * 时由 applyNewSpeed 走 detach/setSpeed/attach 分支，但 propagateNewSource 对<b>网络 ID 相同</b>
     * 的成员不会重播转速（同网络短路），导致水车数量增加后新转速只反映在 GUI 显示、不传播到下游。
     * 因此槽内容变化时需强制把本机脱离网络并清零速度后重新走 applyNewSpeed 的 previous==0 分支。
     * 用本字段避免无实际变化的序列化读取（deserializeNBT 也会触发 onContentsChanged）造成多余重建。
     */
    private float lastPropagatedGenerated = 0;

    public WaterWheelMotorEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
    }

    /** 是否为水车或大水车 */
    public static boolean isWaterWheel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.is(CreateCompat.waterWheelItem()) || stack.is(CreateCompat.largeWaterWheelItem());
    }

    /** 当前输出方向（来自方块状态 FACING） */
    public Direction getOutputFace() {
        return getBlockState().getValue(WaterWheelMotorBlock.FACING);
    }

    /** 当前转速：由机内水车数量决定（每个小水车 +1、每个大水车 +4） */
    public int currentSpeed() {
        ItemStack stack = wheelSlots.getStackInSlot(0);
        if (stack.isEmpty()) {
            return 0;
        }
        int per = stack.is(CreateCompat.largeWaterWheelItem()) ? LARGE_WATER_WHEEL_SPEED : SMALL_WATER_WHEEL_SPEED;
        return stack.getCount() * per;
    }

    @Override
    public float getGeneratedSpeed() {
        if (!(getBlockState().getBlock() instanceof WaterWheelMotorBlock)) {
            return 0;
        }
        int current = currentSpeed();
        if (current <= 0) {
            return 0;
        }
        float sign = counterClockwise ? -1f : 1f;
        return convertToDirection(sign * current, getOutputFace());
    }

    @Override
    public float calculateAddedStressCapacity() {
        return totalCapacity();
    }

    /** 水车槽内所有水车/大水车累加的应力容量（按数量 × 单件 SU） */
    public float totalCapacity() {
        ItemStack stack = wheelSlots.getStackInSlot(0);
        if (stack.isEmpty()) {
            return 0;
        }
        float per = stack.is(CreateCompat.largeWaterWheelItem()) ? LARGE_WATER_WHEEL_SU : WATER_WHEEL_SU;
        return per * stack.getCount();
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed()) {
            updateGeneratedRotation();
        }
    }

    /** 切换旋转方向 */
    public void toggleDirection() {
        counterClockwise = !counterClockwise;
        updateGeneratedRotation();
        setChanged();
    }

    /** 设置输出面：改变方块状态 FACING 并重挂动力网络 */
    public void setOutputFace(Direction face) {
        if (level == null || level.isClientSide || face == getOutputFace()) {
            return;
        }
        detachKinetics();
        level.setBlock(worldPosition, getBlockState().setValue(WaterWheelMotorBlock.FACING, face), 3);
        updateGeneratedRotation();
        setChanged();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putBoolean("counterClockwise", counterClockwise);
        tag.put("wheelSlots", wheelSlots.serializeNBT());
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (tag.contains("counterClockwise")) {
            counterClockwise = tag.getBoolean("counterClockwise");
        }
        if (tag.contains("wheelSlots")) {
            wheelSlots.deserializeNBT(tag.getCompound("wheelSlots"));
        }
        // 记录本次加载后的生成速度，避免加载完成后首次变更槽时多余重建
        lastPropagatedGenerated = getGeneratedSpeed();
    }

    @Override
    @Nonnull
    public Component getDisplayName() {
        return Component.translatable("block.autoresource.water_wheel_motor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new WaterWheelMotorMenu(id, inv, worldPosition);
    }
}
