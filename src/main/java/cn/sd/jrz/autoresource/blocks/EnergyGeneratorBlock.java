package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.EnergyGeneratorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class EnergyGeneratorBlock extends Block implements EntityBlock {
    private final DataConfig config;

    public EnergyGeneratorBlock(Properties properties, DataConfig config) {
        super(properties);
        this.config = config;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new EnergyGeneratorEntity(pos, state, config);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return (l, p, s, tile) -> tick(l, tile);
    }

    private <T extends BlockEntity> void tick(Level level, T tile) {
        if (!(tile instanceof EnergyGeneratorEntity generator)) {
            return;
        }
        generator.tickCount++;
        if (generator.tickCount / 20 >= generator.config.second) {
            generator.tickCount = 0;
            generator.output = Math.min(generator.config.max, generator.output + generator.config.step);
            generator.energy += generator.output;
        }
        if (level.isClientSide) {
            return;
        }
        BlockPos blockPos = generator.getBlockPos();
        for (Direction direction : Direction.values()) {
            BlockPos pos = blockPos.relative(direction);
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                continue;
            }
            IEnergyStorage storage = entity.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).resolve().filter(IEnergyStorage::canReceive).orElse(null);
            if (storage == null) {
                continue;
            }
            generator.energy -= storage.receiveEnergy((int) generator.energy, false);
            if (generator.energy <= 0) {
                return;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand handIn, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        EnergyGeneratorEntity generator = (EnergyGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return InteractionResult.FAIL;
        }
        long energy = generator.energy;
        long output = generator.output;
        double percent = (int) (generator.tickCount / 20.00 / generator.config.second * 10000) / 100.00;
        if (output < generator.config.max) {
            player.sendSystemMessage(Component.translatable("screen.autoresource.energy_generator.message", energy, output, percent));
        } else {
            player.sendSystemMessage(Component.translatable("screen.autoresource.energy_generator.message_max", energy, output));
        }
        return InteractionResult.SUCCESS;
    }
}
