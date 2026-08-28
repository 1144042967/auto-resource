package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.blockentity.BlockGeneratorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.NotNull;

public class BlockGeneratorBlock extends AbstractGeneratorBlock {

    public BlockGeneratorBlock(Properties properties, DataConfig config) {
        super(properties, config);
    }

    @Override
    protected BlockEntity createEntity(BlockPos pos, BlockState state) {
        return new BlockGeneratorEntity(pos, state, config);
    }

    @Override
    protected void tickEntity(Level level, BlockEntity tile) {
        if (level.isClientSide || !(tile instanceof BlockGeneratorEntity generator)) {
            return;
        }
        generator.serverTick();
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand handIn, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockGeneratorEntity generator = (BlockGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return InteractionResult.FAIL;
        }
        // 右击打开 GUI（实体自身是 ExtendedScreenHandlerFactory，附带坐标数据）
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(generator);
        }
        return InteractionResult.SUCCESS;
    }
}
