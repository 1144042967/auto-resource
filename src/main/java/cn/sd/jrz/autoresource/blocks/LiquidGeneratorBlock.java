package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.LiquidGeneratorEntity;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
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
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new LiquidGeneratorEntity(pos, state, config);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, p, s, tile) -> tick(l, tile);
    }

    private <T extends BlockEntity> void tick(Level level, T tile) {
        if (level.isClientSide) {
            return;
        }
        if (!(tile instanceof LiquidGeneratorEntity generator)) {
            return;
        }
        generator.tickCount = Tool.suit(generator.tickCount + 1);
        if (generator.tickCount / 20 >= generator.config.getSecond()) {
            generator.tickCount = 0;
            generator.output = Math.min(generator.config.getMax(), Tool.suit(generator.output + generator.config.getStep()));
        }
        generator.liquid = Tool.suit(generator.liquid + generator.output);
        if (generator.liquid <= 0) {
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
            int maxOutput = Tool.suitInt(generator.liquid);
            IFluidHandler capability = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction.getOpposite());
            if (capability == null) {
                continue;
            }
            boolean canFill = false;
            for (int tank = 0; tank < capability.getTanks(); tank++) {
                if (capability.isFluidValid(tank, new FluidStack(generator.config.getFluid(), maxOutput))) {
                    canFill = true;
                    break;
                }
            }
            if (!canFill) {
                continue;
            }
            int result = capability.fill(new FluidStack(generator.config.getFluid(), maxOutput), IFluidHandler.FluidAction.EXECUTE);
            if (result < 0) {
                result = 0;
            }
            if (result > maxOutput) {
                result = maxOutput;
            }
            generator.liquid -= result;
            if (generator.liquid <= 0) {
                break;
            }
        }
        if (level.hasNeighborSignal(blockPos) && generator.liquid >= 1000 && generator.tickCount % 5 == 0) {
            BlockPos pos = blockPos.relative(Direction.DOWN);
            if (level.getBlockState(pos).getBlock() == Blocks.AIR && level.setBlock(pos, config.getBlock().defaultBlockState(), 3)) {
                generator.liquid -= 1000;
            }
        }
        generator.setChanged();
    }

    @Override
    public @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        return use(level, pos, player);
    }

    @Override
    protected @Nonnull InteractionResult useItemOn(@Nonnull ItemStack p_330929_, @Nonnull BlockState p_335716_, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        return use(level, pos, player);
    }

    private InteractionResult use(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        LiquidGeneratorEntity generator = (LiquidGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return InteractionResult.FAIL;
        }
        long liquid = generator.liquid / 1000;
        double output = generator.output / 1000D;
        double percent = (int) (generator.tickCount / 20.00 / generator.config.getSecond() * 10000) / 100.00;
        if (output < generator.config.getMax()) {
            player.displayClientMessage(Component.translatable("screen.autoresource.liquid_generator.message", liquid, output, percent), true);
        } else {
            player.displayClientMessage(Component.translatable("screen.autoresource.liquid_generator.message_max", liquid, output), true);
        }
        return InteractionResult.SUCCESS;
    }
}
