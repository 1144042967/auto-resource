package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.BlockGeneratorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class BlockGeneratorBlock extends Block implements EntityBlock {
    private final DataConfig config;

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
        generator.tickCount++;
        if (generator.tickCount / 20 >= generator.config.second) {
            generator.tickCount = 0;
            generator.output = Math.min(generator.config.max, generator.output + generator.config.step);
        }
        generator.block += generator.output;
        if (generator.block < 0) {
            generator.block = 0;
        }
        if (generator.block / 1000 <= 0) {
            generator.setChanged();
            return;
        }
        //传输
        BlockPos blockPos = generator.getBlockPos();
        for (Direction direction : Direction.values()) {
            BlockPos pos = blockPos.relative(direction);
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                continue;
            }
            IItemHandler handler = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).resolve().orElse(null);
            if (handler == null) {
                continue;
            }
            ItemStack result = ItemHandlerHelper.insertItemStacked(handler, new ItemStack(config.getBlock(), (int) (generator.block / 1000)), false);
            generator.block -= ((int) (generator.block / 1000) - result.getCount()) * 1000L;
            if (generator.block / 1000 <= 0) {
                break;
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
        double block = generator.block / 1000D;
        double output = generator.output / 1000D;
        double percent = (int) (generator.tickCount / 20.00 / generator.config.second * 10000) / 100.00;
        if (output < generator.config.max) {
            player.sendSystemMessage(Component.translatable("screen.autoresource.block_generator.message", block, output, percent));
        } else {
            player.sendSystemMessage(Component.translatable("screen.autoresource.block_generator.message_max", block, output));
        }
        return InteractionResult.SUCCESS;
    }
}
