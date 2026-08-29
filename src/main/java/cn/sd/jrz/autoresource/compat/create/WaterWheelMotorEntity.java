package cn.sd.jrz.autoresource.compat.create;

import cn.sd.jrz.autoresource.storage.MachineSlotStorage;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

/**
 * 水车马达实体（仅 Create 加载时使用）。Create 动力源：转速 = 水车数量 × 单件转速
 * （水车 1 / 大水车 4），应力容量 = 数量 × 单件 SU（256/512）；未放水车不产生转速。
 * 旋转方向与输出面经 GUI 调节并持久化到 NBT。
 */
public class WaterWheelMotorEntity extends GeneratingKineticBlockEntity implements MenuProvider, ExtendedScreenHandlerFactory {
    /**
     * 水车槽位数量（单个槽，可放一组水车/大水车）
     */
    public static final int SLOT_COUNT = 1;
    /**
     * 每个水车增加的转速
     */
    public static final int SMALL_WATER_WHEEL_SPEED = 1;
    /**
     * 每个大水车增加的转速
     */
    public static final int LARGE_WATER_WHEEL_SPEED = 4;
    /**
     * 每个水车提供的应力容量
     */
    public static final float WATER_WHEEL_SU = 256f;
    /**
     * 每个大水车提供的应力容量
     */
    public static final float LARGE_WATER_WHEEL_SU = 512f;

    /**
     * 水车槽位（单个槽，仅接受水车/大水车，可放一组）
     */
    public final MachineSlotStorage wheelSlots = new MachineSlotStorage(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            handleWheelContentsChanged();
        }
    }.setValidator(WaterWheelMotorEntity::isWaterWheel);
    /**
     * 是否逆时针（默认顺时针=false）
     */
    public boolean counterClockwise = false;
    /**
     * 上次已成功传播的生成速度（运行时缓存）。Create 的 applyNewSpeed 在"已有网络、非0→非0"时
     * 不重播转速（同网络短路），需强制脱离网络重建；此字段用于识别槽内容实际变化，
     * 避免 deserializeNBT（也会触发 onContentsChanged）造成多余重建。
     */
    private float lastPropagatedGenerated = 0;

    /**
     * 水车槽内容变化的统一处理：更新转速/应力容量并强制把最新生成速度传播到动力网络。
     * 由两处调用：MachineSlotStorage.onContentsChanged；以及 Menu 中水车槽覆写的 Slot.setChanged
     * （moveItemStackTo 在槽内已有相同物品时只调 setChanged、不触发 onContentsChanged，
     * 导致"增加水车数量"时转速不传播）。
     */
    public void handleWheelContentsChanged() {
        // 仅服务端处理（客户端 read 时的 deserializeNBT 也会触发 onContentsChanged）
        if (level == null || level.isClientSide) {
            return;
        }
        setChanged();
        // 转速/应力容量变化：通知网络重新计算
        updateGeneratedRotation();
        // 兜底1：显式同步最新容量/应力（Create 的容量推送在网络重挂后可能被跳过）
        if (hasNetwork()) {
            KineticNetwork network = getOrCreateNetwork();
            network.updateCapacityFor(WaterWheelMotorEntity.this, totalCapacity());
            network.updateStressFor(WaterWheelMotorEntity.this, calculateStressApplied());
            network.updateStress();
        }
        // 兜底2：强制脱离网络并清零速度，再走 applyNewSpeed 的 previous==0 分支重建传播，绕过 Create 同网络短路
        float generated = getGeneratedSpeed();
        if (generated != lastPropagatedGenerated) {
            lastPropagatedGenerated = generated;
            detachKinetics();
            setNetwork(null);
            setSpeed(0);
            updateGeneratedRotation();
        }
        // 兜底3：置位 updateSpeed，下一 tick 由 attachKinetics 重新传播到整个网络
        updateSpeed = true;
    }

    public WaterWheelMotorEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * 打开扩展菜单时写入的附加数据：机器坐标
     */
    @Override
    public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
        buf.writeBlockPos(getBlockPos());
    }

    /**
     * 是否为水车或大水车
     */
    public static boolean isWaterWheel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.is(CreateCompat.waterWheelItem()) || stack.is(CreateCompat.largeWaterWheelItem());
    }

    /**
     * 当前输出方向（来自方块状态 FACING）
     */
    public Direction getOutputFace() {
        return getBlockState().getValue(WaterWheelMotorBlock.FACING);
    }

    /**
     * 当前转速：由机内水车数量决定（每个小水车 +1、每个大水车 +4）
     */
    public int currentSpeed() {
        ItemStack stack = wheelSlots.getItem(0);
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

    /**
     * 水车槽内所有水车/大水车累加的应力容量（按数量 × 单件 SU）
     */
    public float totalCapacity() {
        ItemStack stack = wheelSlots.getItem(0);
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

    /**
     * 切换旋转方向
     */
    public void toggleDirection() {
        counterClockwise = !counterClockwise;
        updateGeneratedRotation();
        setChanged();
    }

    /**
     * 设置输出面：改变方块状态 FACING 并重挂动力网络
     */
    public void setOutputFace(Direction face) {
        if (level == null || level.isClientSide || face == getOutputFace()) {
            return;
        }
        detachKinetics();
        level.setBlock(getBlockPos(), getBlockState().setValue(WaterWheelMotorBlock.FACING, face), 3);
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
    @NotNull
    public Component getDisplayName() {
        return Component.translatable("block.autoresource.water_wheel_motor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
        return new WaterWheelMotorMenu(id, inv, getBlockPos());
    }

    /**
     * 指定方向的相邻方块注册 id（用于 GUI 展示实际相邻方块的物品图标）。无世界或方块无物品时返回 0。
     */
    public int getNeighborBlockId(Direction direction) {
        Level level = getLevel();
        if (level == null) {
            return 0;
        }
        //noinspection deprecation
        return BuiltInRegistries.BLOCK.getId(level.getBlockState(worldPosition.relative(direction)).getBlock());
    }

    /**
     * 指定方向相邻方块的物品栈（数量 1）。无方块或方块无对应物品时返回空。用于 GUI 方向按钮显示相邻方块图标。
     */
    @Nonnull
    public ItemStack getNeighborStack(Direction direction) {
        //noinspection deprecation
        Item item = BuiltInRegistries.BLOCK.byId(getNeighborBlockId(direction)).asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
