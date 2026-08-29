package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.AbstractGeneratorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 机器方块基类：共享配置持有、方块实体创建与 tick 分发，并提供打开 GUI 的公共逻辑；
 * 子类实现 {@link #createEntity} 与 {@link #tickEntity} 并提供机器专属逻辑（如流体的空桶提取）。
 */
public abstract class AbstractGeneratorBlock extends Block implements EntityBlock {
    protected final DataConfig config;

    protected AbstractGeneratorBlock(Properties properties, DataConfig config) {
        super(properties);
        this.config = config;
    }

    /**
     * 创建对应的方块实体
     */
    protected abstract BlockEntity createEntity(BlockPos pos, BlockState state);

    /**
     * 服务端 tick 分发（子类按实体类型调用对应 serverTick）
     */
    protected abstract void tickEntity(Level level, BlockEntity tile);

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return createEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, _, _, tile) -> tickEntity(l, tile);
    }

    @Override
    public @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        return openGui(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    protected @Nonnull InteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        return openGui(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    /**
     * 打开机器 GUI（子类 use 方法复用；服务端经实体 MenuProvider 打开）
     */
    protected boolean openGui(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return true;
        }
        AbstractGeneratorEntity generator = (AbstractGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return false;
        }
        player.openMenu(generator, pos);
        return true;
    }
}
