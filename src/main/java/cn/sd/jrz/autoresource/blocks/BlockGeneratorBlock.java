package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.BlockGeneratorEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("deprecation")
public class BlockGeneratorBlock extends Block implements ITileEntityProvider {
    private final DataConfig config;

    public BlockGeneratorBlock(Properties properties, DataConfig config) {
        super(properties);
        this.config = config;
    }

    @Nullable
    @Override
    public TileEntity newBlockEntity(@Nonnull IBlockReader reader) {
        return new BlockGeneratorEntity(config);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull ActionResultType use(@Nonnull BlockState state, World level, @Nonnull BlockPos pos, @Nonnull PlayerEntity player, @Nonnull Hand hand, @Nonnull BlockRayTraceResult result) {
        if (level.isClientSide) {
            return ActionResultType.SUCCESS;
        }
        BlockGeneratorEntity generator = (BlockGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return ActionResultType.FAIL;
        }
        long block = generator.block / 1000;
        double output = generator.output / 1000D;
        double percent = (int) (generator.tickCount / 20.00D / generator.config.getSecond() * 10000D) / 100.00D;
        if (output < generator.config.getMax()) {
            player.sendMessage(new TranslationTextComponent("screen.autoresource.block_generator.message", block, output, percent), Util.NIL_UUID);
        } else {
            player.sendMessage(new TranslationTextComponent("screen.autoresource.block_generator.message_max", block, output), Util.NIL_UUID);
        }
        return ActionResultType.SUCCESS;
    }
}
