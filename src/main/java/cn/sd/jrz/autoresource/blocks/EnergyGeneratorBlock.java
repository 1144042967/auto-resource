package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.EnergyGeneratorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class EnergyGeneratorBlock extends AbstractGeneratorBlock {

    public EnergyGeneratorBlock(Properties properties, DataConfig config) {
        super(properties, config);
    }

    @Override
    protected BlockEntity createEntity(BlockPos pos, BlockState state) {
        return new EnergyGeneratorEntity(pos, state, config);
    }

    @Override
    protected void tickEntity(Level level, BlockEntity tile) {
        if (level.isClientSide || !(tile instanceof EnergyGeneratorEntity generator)) {
            return;
        }
        generator.serverTick();
    }

    /**
     * 破坏时，充电槽中的物品掉落（加速槽内容随物品 NBT 保留，不在此掉落）
     */
    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof EnergyGeneratorEntity entity) {
            ItemStack charge = entity.chargeSlot.getStackInSlot(0);
            if (!charge.isEmpty()) {
                drops.add(charge);
            }
        }
        return drops;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        EnergyGeneratorEntity generator = (EnergyGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return InteractionResult.FAIL;
        }
        // 右键打开 GUI
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, generator, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
