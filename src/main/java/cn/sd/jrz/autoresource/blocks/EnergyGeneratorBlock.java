package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.EnergyGeneratorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * FE 发电机方块。
 * <p>
 * 26.x 适配：tick 转发到 {@link EnergyGeneratorEntity#serverTick()}；右键打开 GUI（继承基类，包括 useWithoutItem 与 useItemOn 两种路径）；
 * 破坏时附带掉落充电槽内的物品（加速槽内容随物品 DataComponent 保留）。
 */
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
        if (level.isClientSide() || !(tile instanceof EnergyGeneratorEntity generator)) {
            return;
        }
        generator.serverTick();
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
}
