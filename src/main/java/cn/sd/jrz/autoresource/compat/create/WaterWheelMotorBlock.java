package cn.sd.jrz.autoresource.compat.create;

import cn.sd.jrz.autoresource.setup.Registration;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 水车马达方块（仅 Create 加载时注册）。单方向动力源：转速/应力容量由放入的水车数量决定，
 * 输出面（FACING）决定应力输出方向，转速数值由方块实体渲染器显示在四面侧。
 */
public class WaterWheelMotorBlock extends DirectionalKineticBlock implements IBE<WaterWheelMotorEntity> {

    public WaterWheelMotorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = getPreferredFacing(context);
        if ((context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) || preferred == null) {
            return super.getStateForPlacement(context);
        }
        return defaultBlockState().setValue(FACING, preferred);
    }

    /**
     * 整方块的点击/碰撞形状
     */
    @SuppressWarnings("deprecation")
    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Shapes.block();
    }

    /**
     * 破坏时把水车槽内的水车/大水车作为独立物品掉落（方块自身由战利品表掉落）。
     */
    @SuppressWarnings("deprecation")
    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, @NotNull LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof WaterWheelMotorEntity entity) {
            for (int i = 0; i < entity.wheelSlots.getContainerSize(); i++) {
                ItemStack stack = entity.wheelSlots.getItem(i);
                if (!stack.isEmpty()) {
                    drops.add(stack);
                }
            }
        }
        return drops;
    }

    // IRotate:

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        // 仅在输出面连接轴
        return face == state.getValue(FACING);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hideStressImpact() {
        return true;
    }

    @Override
    public Class<WaterWheelMotorEntity> getBlockEntityClass() {
        return WaterWheelMotorEntity.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public BlockEntityType<? extends WaterWheelMotorEntity> getBlockEntityType() {
        return (BlockEntityType<? extends WaterWheelMotorEntity>) Registration.WATER_WHEEL_MOTOR_ENTITY;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return getBlockEntityType().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // 水车马达是 SmartBlockEntity 子类，按 Create 的方式逐 tick 调用（仅对本实体类型生效）
        if (type != getBlockEntityType()) {
            return null;
        }
        return (l, p, s, be) -> ((SmartBlockEntity) be).tick();
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        WaterWheelMotorEntity entity = getBlockEntity(level, pos);
        if (entity == null) {
            return InteractionResult.FAIL;
        }
        // 右键打开 GUI（实体自身是 ExtendedScreenHandlerFactory，附带坐标数据）
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(entity);
        }
        return InteractionResult.SUCCESS;
    }
}
