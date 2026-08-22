package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.BlockGeneratorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/**
 * 通用方块生成机方块。
 * <p>
 * 26.x 适配：tick 转发到 {@link BlockGeneratorEntity#serverTick()}；右键打开 GUI（继承基类）；
 * 标记槽内容随物品 DataComponent 保留（破坏时方块掉落不会丢失）。
 */
public class BlockGeneratorBlock extends AbstractGeneratorBlock {

    public BlockGeneratorBlock(Properties properties, DataConfig config) {
        super(properties, config);
    }

    @Override
    protected BlockEntity createEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new BlockGeneratorEntity(pos, state, config);
    }

    @Override
    protected void tickEntity(Level level, BlockEntity tile) {
        if (level.isClientSide() || !(tile instanceof BlockGeneratorEntity generator)) {
            return;
        }
        generator.serverTick();
    }
}
