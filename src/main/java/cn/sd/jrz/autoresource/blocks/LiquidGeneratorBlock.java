package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.LiquidGeneratorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class LiquidGeneratorBlock extends Block implements EntityBlock {
    private final DataConfig config;
    private final Direction[] directions = Direction.values();
    private int findIndex = 0;

    public LiquidGeneratorBlock(Properties properties, DataConfig config) {
        super(properties);
        this.config = config;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new LiquidGeneratorEntity(pos, state, config);
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
        if (!(tile instanceof LiquidGeneratorEntity generator)) {
            return;
        }
        generator.tickCount++;
        if (generator.tickCount / 20 >= generator.config.second) {
            generator.tickCount = 0;
            generator.output = Math.min(generator.config.max, generator.output + generator.config.step);
        }
        generator.liquid += generator.output;
        if (generator.liquid < 0) {
            generator.liquid = 0;
        }
        if (generator.liquid / 1000 <= 0) {
            generator.setChanged();
            return;
        }
        //传输
        BlockPos blockPos = generator.getBlockPos();
        for (int i = 0; i < directions.length; i++) {
            findIndex = (findIndex + 1) % directions.length;
            Direction direction = directions[findIndex];
            BlockPos pos = blockPos.relative(direction);
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                continue;
            }
            IFluidHandler storage = entity.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite()).resolve().filter(
                    handler -> handler.isFluidValid((int) (generator.liquid / 1000), new FluidStack(generator.config.getFluid(), (int) (generator.liquid / 1000)))
            ).orElse(null);
            if (storage == null) {
                continue;
            }
            int fill = storage.fill(new FluidStack(generator.config.getFluid(), (int) (generator.liquid / 1000)), IFluidHandler.FluidAction.EXECUTE);
            generator.liquid -= fill * 1000L;
            if (generator.liquid / 1000 <= 0) {
                break;
            }
        }
        if (level.hasNeighborSignal(blockPos) && generator.liquid / 1000 >= 1 && generator.tickCount % 5 == 0) {
            BlockPos pos = blockPos.relative(Direction.DOWN);
            if (level.getBlockState(pos).getBlock() == Blocks.AIR && level.setBlock(pos, config.getBlock().defaultBlockState(), 3)) {
                generator.liquid -= 1000;
            }
        }
        generator.setChanged();
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand handIn, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        LiquidGeneratorEntity generator = (LiquidGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return InteractionResult.FAIL;
        }
        double liquid = generator.liquid / 1000D;
        double output = generator.output / 1000D;
        double percent = (int) (generator.tickCount / 20.00 / generator.config.second * 10000) / 100.00;
        if (output < generator.config.max) {
            player.sendSystemMessage(Component.translatable("screen.autoresource.liquid_generator.message", liquid, output, percent));
        } else {
            player.sendSystemMessage(Component.translatable("screen.autoresource.liquid_generator.message_max", liquid, output));
        }
        return InteractionResult.SUCCESS;
    }
}
