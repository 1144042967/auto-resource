package cn.sd.jrz.autoresource.capability;

import cn.sd.jrz.autoresource.blockentity.BlockGeneratorEntity;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import javax.annotation.Nonnull;
import java.util.Iterator;

/**
 * 方块生成器对外物品连接：只出不进，仅输出标记的方块（数量单位为"件"）。
 * 未标记时视作空存储。内部存量以 Block×1000 计。
 */
public class BlockConnection implements Storage<ItemVariant> {

    private final BlockGeneratorEntity owner;

    public BlockConnection(BlockGeneratorEntity owner) {
        this.owner = owner;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        // 禁止输入
        return 0;
    }

    @Override
    public boolean supportsInsertion() {
        return false;
    }

    /**
     * 可用件数（Block/1000 → 件）
     */
    private long availableItems() {
        return Math.max(0, owner.block / 1000);
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        ItemStack marked = owner.getMarkedItem();
        if (marked.isEmpty() || maxAmount <= 0 || !resource.matches(marked)) {
            return 0;
        }
        long available = availableItems();
        long extracted = Math.min(maxAmount, available);
        if (extracted <= 0) {
            return 0;
        }
        owner.block -= extracted * 1000L;
        transaction.addCloseCallback((txn, result) -> {
            if (result.wasAborted()) {
                owner.block += extracted * 1000L;
            }
        });
        return extracted;
    }

    @Override
    public boolean supportsExtraction() {
        return true;
    }

    @Nonnull
    @Override
    public Iterator<? extends StorageView<ItemVariant>> iterator(TransactionContext transaction) {
        return java.util.List.<StorageView<ItemVariant>>of(new View()).iterator();
    }

    /**
     * 单一资源视图：标记后暴露当前可提取件数，未标记时为空白资源
     */
    private class View implements StorageView<ItemVariant> {
        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            return BlockConnection.this.extract(resource, maxAmount, transaction);
        }

        @Override
        public boolean isResourceBlank() {
            return owner.getMarkedItem().isEmpty();
        }

        @Nonnull
        @Override
        public ItemVariant getResource() {
            return ItemVariant.of(owner.getMarkedItem());
        }

        @Override
        public long getAmount() {
            return availableItems();
        }

        @Override
        public long getCapacity() {
            return Long.MAX_VALUE;
        }
    }
}
