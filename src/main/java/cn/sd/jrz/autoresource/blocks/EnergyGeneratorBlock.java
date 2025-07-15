package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.EnergyGeneratorEntity;
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
public class EnergyGeneratorBlock extends Block implements ITileEntityProvider {
    private final DataConfig config;

    public EnergyGeneratorBlock(Properties properties, DataConfig config) {
        super(properties);
        this.config = config;
    }

    @Nullable
    @Override
    public TileEntity newBlockEntity(@Nonnull IBlockReader reader) {
        return new EnergyGeneratorEntity(config);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull ActionResultType use(@Nonnull BlockState state, World level, @Nonnull BlockPos pos, @Nonnull PlayerEntity player, @Nonnull Hand hand, @Nonnull BlockRayTraceResult result) {
        if (level.isClientSide) {
            return ActionResultType.SUCCESS;
        }
        EnergyGeneratorEntity generator = (EnergyGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return ActionResultType.FAIL;
        }
        long energy = generator.energy;
        long output = generator.output;
        double percent = (int) (generator.tickCount / 20.00D / generator.config.getSecond() * 10000D) / 100.00D;
        long increase = generator.beaconIncrease;
        if (output < generator.config.getMax()) {
            player.sendMessage(new TranslationTextComponent("screen.autoresource.energy_generator.message", energy, output, percent, increase), Util.NIL_UUID);
        } else {
            player.sendMessage(new TranslationTextComponent("screen.autoresource.energy_generator.message_max", energy, output), Util.NIL_UUID);
        }
        return ActionResultType.SUCCESS;
    }
}
