package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.BlockGeneratorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    public @Nonnull InteractionResult use(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockGeneratorEntity generator = (BlockGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return InteractionResult.FAIL;
        }
        // 右击打开 GUI
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, generator, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
