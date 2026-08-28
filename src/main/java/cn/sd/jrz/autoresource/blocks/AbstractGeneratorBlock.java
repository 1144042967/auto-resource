package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 机器方块基类：共享配置持有、方块实体创建与 tick 分发；子类实现 {@link #createEntity} 与 {@link #tickEntity} 并提供机器专属逻辑。
 */
public abstract class AbstractGeneratorBlock extends Block implements EntityBlock {
    protected final DataConfig config;

    protected AbstractGeneratorBlock(Properties properties, DataConfig config) {
        super(properties);
        this.config = config;
    }

    /**
     * 创建对应的方块实体
     */
    protected abstract BlockEntity createEntity(BlockPos pos, BlockState state);

    /**
     * 服务端 tick 分发（子类按实体类型调用对应 serverTick）
     */
    protected abstract void tickEntity(Level level, BlockEntity tile);

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return createEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return (l, p, s, tile) -> tickEntity(l, tile);
    }
}
