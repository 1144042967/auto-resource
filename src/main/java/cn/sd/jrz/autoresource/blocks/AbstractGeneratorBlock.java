package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

    /**
     * 破坏掉落：经 loot 表掉落方块自身，并把机器状态写入掉落物品的
     * {@code minecraft:block_entity_data} 组件（放置时经 {@code loadCustomOnly} 恢复，
     * 数值/六面开关/标记等状态得以保留）。
     * <p>
     * 说明：1.21.1 的 loot 函数 {@code copy_nbt} 已更名为 {@code copy_custom_data} 且写入的是
     * {@code custom_data} 组件，本 mod 物品 tooltip 与放置恢复读取的是 {@code block_entity_data}，
     * 因此状态保留改由 Java 侧完成，loot 表只负责掉落方块自身与自定义名称。
     */
    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, @NotNull LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity != null) {
            HolderLookup.Provider registries = builder.getLevel().registryAccess();
            // saveCustomOnly 只写子类经 saveAdditional 保存的状态字段（含 CustomName），不含 id/坐标
            CompoundTag tag = blockEntity.saveCustomOnly(registries);
            CustomData data = CustomData.of(tag);
            for (ItemStack stack : drops) {
                if (stack.is(this.asItem())) {
                    stack.set(DataComponents.BLOCK_ENTITY_DATA, data);
                }
            }
        }
        return drops;
    }
}
