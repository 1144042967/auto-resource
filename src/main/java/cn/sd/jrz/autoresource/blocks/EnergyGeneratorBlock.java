package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.EnergyGeneratorEntity;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * FE 发电机方块。
 * <p>
 * 26.x 适配：tick 转发到 {@link EnergyGeneratorEntity#serverTick()}；右键打开 GUI（包括 useWithoutItem 与 useItemOn 两种路径）；
 * 破坏时附带掉落充电槽内的物品（加速槽内容随物品 DataComponent 保留）。
 */
public class EnergyGeneratorBlock extends Block implements EntityBlock {
    private final DataConfig config;

    public EnergyGeneratorBlock(Properties properties, DataConfig config) {
        super(properties);
        this.config = config;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new EnergyGeneratorEntity(pos, state, config);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, _, _, tile) -> {
            if (l.isClientSide() || !(tile instanceof EnergyGeneratorEntity generator)) {
                return;
            }
            generator.serverTick();
        };
    }

    /**
     * 破坏时，掉落充电槽内的物品（加速槽内容随物品 DataComponent 保留，不在此掉落）
     */
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

    @Override
    public @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        return openGui(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    protected @Nonnull InteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        return openGui(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    /**
     * 打开 FE 发电机 GUI
     */
    private boolean openGui(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return true;
        }
        EnergyGeneratorEntity generator = (EnergyGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return false;
        }
        player.openMenu(generator, pos);
        return true;
    }
}
