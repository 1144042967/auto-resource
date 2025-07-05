package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.BlockGeneratorEntity;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;
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
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class BlockGeneratorBlock extends Block implements EntityBlock {
    private final DataConfig config;
    private final Direction[] directions = Direction.values();
    private int findIndex = 0;

    public BlockGeneratorBlock(Properties properties, DataConfig config) {
        super(properties);
        this.config = config;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new BlockGeneratorEntity(pos, state, config);
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
        if (!(tile instanceof BlockGeneratorEntity generator)) {
            return;
        }
        //计算产量
        generator.tickCount = Tool.suit(generator.tickCount + 1);
        if (generator.tickCount / 20 >= generator.config.getSecond()) {
            generator.tickCount = 0;
            generator.output = Math.min(generator.config.getMax(), Tool.suit(generator.output + generator.config.getStep()));
        }
        generator.block = Tool.suit(generator.block + generator.output);
        if (generator.block / 1000 <= 0) {
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
            IItemHandler handler = entity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite()).resolve().orElse(null);
            if (handler == null) {
                continue;
            }
            int maxOutput = Tool.suitInt(generator.block / 1000);
            ItemStack result = ItemHandlerHelper.insertItemStacked(handler, new ItemStack(config.getBlock(), maxOutput), false);
            int count = result.getCount();
            if (count < 0) {
                count = 0;
            }
            if (count > maxOutput) {
                count = maxOutput;
            }
            generator.block -= (maxOutput - count) * 1000L;
            if (generator.block / 1000 <= 0) {
                break;
            }
        }
        if (level.hasNeighborSignal(blockPos) && generator.block >= 1000 && generator.tickCount % 5 == 0) {
            BlockPos pos = blockPos.relative(Direction.DOWN);
            if (level.getBlockState(pos).getBlock() == Blocks.AIR && level.setBlock(pos, config.getBlock().defaultBlockState(), 3)) {
                generator.block -= 1000;
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
        BlockGeneratorEntity generator = (BlockGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return InteractionResult.FAIL;
        }
        long block = generator.block / 1000;
        double output = generator.output / 1000D;
        double percent = (int) (generator.tickCount / 20.00D / generator.config.getSecond() * 10000D) / 100.00D;
        if (output < generator.config.getMax()) {
            player.sendMessage(new TranslatableComponent("screen.autoresource.block_generator.message", block, output, percent), Util.NIL_UUID);
        } else {
            player.sendMessage(new TranslatableComponent("screen.autoresource.block_generator.message_max", block, output), Util.NIL_UUID);
        }
        return InteractionResult.SUCCESS;
    }
}
