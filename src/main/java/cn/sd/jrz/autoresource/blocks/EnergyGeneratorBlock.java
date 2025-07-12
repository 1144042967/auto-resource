package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.EnergyGeneratorEntity;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
    private final Direction[] directions = Direction.values();
    private int findIndex = 0;

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
        if (level.isClientSide) {
            return;
        }
        if (!(tile instanceof EnergyGeneratorEntity generator)) {
            return;
        }
        BlockPos blockPos = generator.getBlockPos();
        generator.tickCount = Tool.suit(generator.tickCount + 1);
        if (generator.tickCount / 20 >= generator.config.getSecond()) {
            generator.tickCount = 0;
            generator.beaconIncrease = generator.config.getStep();
            if (level.hasNeighborSignal(blockPos)) {
                BlockPos pos = blockPos.relative(Direction.DOWN);
                if (level.getBlockState(pos).getBlock() == Blocks.BEACON) {
                    generator.beaconIncrease = Tool.suit((long) (generator.output / 10000.0 * config.getBeaconStep()) + generator.config.getStep());
                }
            }
            generator.output = Math.min(generator.config.getMax(), Tool.suit(generator.output + generator.beaconIncrease));
        }
        generator.energy = Tool.suit(generator.energy + generator.output);
        for (int i = 0; i < directions.length; i++) {
            findIndex = (findIndex + 1) % directions.length;
            Direction direction = directions[findIndex];
            BlockPos pos = blockPos.relative(direction);
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                continue;
            }
            IEnergyStorage storage = entity.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).resolve().filter(IEnergyStorage::canReceive).orElse(null);
            if (storage == null) {
                continue;
            }
            int maxOutput = Tool.suitInt(generator.energy);
            int result = storage.receiveEnergy(maxOutput, false);
            if (result < 0) {
                result = 0;
            }
            if (result > maxOutput) {
                result = maxOutput;
            }
            generator.energy -= result;
            if (generator.energy <= 0) {
                break;
            }
        }
        generator.setChanged();
    }

    @Override
    public @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return use(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack p_330929_, @NotNull BlockState p_335716_, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand handIn, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        return use(level, pos, player) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
    }

    private boolean use(Level level, BlockPos pos, Player player) {
        EnergyGeneratorEntity generator = (EnergyGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return false;
        }
        long energy = generator.energy;
        long output = generator.output;
        double percent = (int) (generator.tickCount / 20.00D / generator.config.getSecond() * 10000D) / 100.00D;
        long increase = generator.beaconIncrease;
        if (output < generator.config.getMax()) {
            player.sendSystemMessage(Component.translatable("screen.autoresource.energy_generator.message", energy, output, percent, increase));
        } else {
            player.sendSystemMessage(Component.translatable("screen.autoresource.energy_generator.message_max", energy, output));
        }
        return true;
    }
}
