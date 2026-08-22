package cn.sd.jrz.autoresource.connection;

import cn.sd.jrz.autoresource.entities.BlockGeneratorEntity;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

/**
 * 方块输出连接（26.x 传输 API）。
 * <p>
 * 未标记时无法输出（资源为空、数量为 0、提取返回 0）；标记后按标记物品输出存量方块。
 * 只允许提取（insert 返回 0），提取时在事务上下文中扣减存量，事务回滚时恢复，提交时通知实体保存。
 */
public class BlockConnection implements ResourceHandler<@NotNull ItemResource> {
    private final BlockGeneratorEntity owner;
    private final BlockJournal journal = new BlockJournal();

    public BlockConnection(BlockGeneratorEntity owner) {
        this.owner = owner;
    }

    /**
     * 当前标记的物品（未标记时返回空）
     */
    private ItemStack getMarked() {
        return owner.getMarkedItem();
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public @Nonnull ItemResource getResource(int index) {
        ItemStack marked = getMarked();
        if (marked.isEmpty()) {
            return ItemResource.EMPTY;
        }
        return ItemResource.of(marked.getItem());
    }

    @Override
    public long getAmountAsLong(int index) {
        if (getMarked().isEmpty()) {
            return 0;
        }
        return owner.block / 1000;
    }

    @Override
    public long getCapacityAsLong(int index, @Nonnull ItemResource resource) {
        return Long.MAX_VALUE / 1000;
    }

    @Override
    public boolean isValid(int index, @Nonnull ItemResource resource) {
        return false;
    }

    @Override
    public int insert(int index, @Nonnull ItemResource resource, int amount, @Nonnull TransactionContext transaction) {
        // 禁止输入
        return 0;
    }

    @Override
    public int extract(int index, @Nonnull ItemResource resource, int amount, @Nonnull TransactionContext transaction) {
        ItemStack marked = getMarked();
        if (marked.isEmpty() || !resource.is(marked.getItem())) {
            return 0;
        }
        int maxOutput = Tool.suitInt(owner.block / 1000);
        if (maxOutput <= 0 || amount <= 0) {
            return 0;
        }
        int count = Math.min(maxOutput, amount);
        journal.updateSnapshots(transaction);
        owner.block -= count * 1000L;
        return count;
    }

    private class BlockJournal extends SnapshotJournal<Long> {
        @Override
        protected Long createSnapshot() {
            return owner.block;
        }

        @Override
        protected void revertToSnapshot(Long snapshot) {
            owner.block = snapshot;
        }

        @Override
        protected void onRootCommit(Long originalState) {
            if (owner.block != originalState) {
                owner.setChanged();
            }
        }
    }
}
