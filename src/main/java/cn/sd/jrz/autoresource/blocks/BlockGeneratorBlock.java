package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.BlockGeneratorEntity;
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
 * 通用方块生成机方块。
 * <p>
 * 26.x 适配：tick 转发到 {@link BlockGeneratorEntity#serverTick()}；右键打开 GUI；
 * 标记槽内容随物品 DataComponent 保留（破坏时方块掉落不会丢失）。
 */
public class BlockGeneratorBlock extends Block implements EntityBlock {
    private final DataConfig config;

    public BlockGeneratorBlock(Properties properties, DataConfig config) {
        super(properties);
        this.config = config;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new BlockGeneratorEntity(pos, state, config);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, _, _, tile) -> {
            if (l.isClientSide() || !(tile instanceof BlockGeneratorEntity generator)) {
                return;
            }
            generator.serverTick();
        };
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
     * 打开方块生成机 GUI（标记槽放入合法物品后锁定；输出展示槽单击提取）
     */
    private boolean openGui(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return true;
        }
        BlockGeneratorEntity generator = (BlockGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return false;
        }
        player.openMenu(generator, pos);
        return true;
    }
}
