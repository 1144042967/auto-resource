package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.EnergyGeneratorEntity;
import cn.sd.jrz.autoresource.entities.GeneratorEntity;
import cn.sd.jrz.autoresource.entities.StorageEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class BlockGeneratorBlock  extends Block implements EntityBlock {
    private final DataConfig config;

    public BlockGeneratorBlock(Properties properties, DataConfig config) {
        super(properties);
        this.config = config;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new EnergyGeneratorEntity(pos, state, config);
    }

    @javax.annotation.Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return type == config.getEntityType() ? (l, p, s, tile) -> EnergyGeneratorEntity.tick(tile) : null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand handIn, @NotNull BlockHitResult hit) {
        if (!level.isClientSide) {
            StorageEntity te = (StorageEntity) level.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            double energy = te.getEnergy();
            double maxEnergy = config.getStorageMaxEnergy();
            double percent = ((int) (energy / maxEnergy * 10000.00)) / 100.00;
            player.sendSystemMessage(Component.translatable("screen.exponentialpower.storage_total_percent", percent));
            player.sendSystemMessage(Component.translatable("screen.exponentialpower.storage_total_current", energy));
            player.sendSystemMessage(Component.translatable("screen.exponentialpower.storage_total_max", maxEnergy));
        }
        return InteractionResult.SUCCESS;
    }


    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (stack.hasCustomHoverName()) {
            BlockEntity te = level.getBlockEntity(pos);
            if (te instanceof GeneratorEntity generatorEntity) {
                generatorEntity.setCustomName(stack.getHoverName());
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState oldState, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean bool) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity blockentity = level.getBlockEntity(pos);
            if (blockentity instanceof Container) {
                Containers.dropContents(level, pos, (Container) blockentity);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(oldState, level, pos, newState, bool);
        }
    }
}
