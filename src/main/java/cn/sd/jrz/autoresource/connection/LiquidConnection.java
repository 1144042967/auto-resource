package cn.sd.jrz.autoresource.connection;

import cn.sd.jrz.autoresource.entities.LiquidGeneratorEntity;
import cn.sd.jrz.autoresource.util.Tool;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

/**
 * 流体输出连接（26.x 传输 API）。
 * <p>
 * 只允许提取本机对应类型的流体（insert 返回 0），提取时在事务上下文中扣减存量，
 * 事务回滚时恢复，提交时通知实体保存。
 */
public class LiquidConnection implements ResourceHandler<@NotNull FluidResource> {
    private final LiquidGeneratorEntity owner;
    private final LiquidJournal journal = new LiquidJournal();

    public LiquidConnection(LiquidGeneratorEntity owner) {
        this.owner = owner;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public @Nonnull FluidResource getResource(int index) {
        return FluidResource.of(owner.config.getFluid());
    }

    @Override
    public long getAmountAsLong(int index) {
        return owner.liquid;
    }

    @Override
    public long getCapacityAsLong(int index, @Nonnull FluidResource resource) {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean isValid(int index, @Nonnull FluidResource resource) {
        return false;
    }

    @Override
    public int insert(int index, @Nonnull FluidResource resource, int amount, @Nonnull TransactionContext transaction) {
        // 禁止输入
        return 0;
    }

    @Override
    public int extract(int index, @Nonnull FluidResource resource, int amount, @Nonnull TransactionContext transaction) {
        if (!resource.is(owner.config.getFluid())) {
            return 0;
        }
        int maxOutput = Tool.suitInt(owner.liquid);
        if (maxOutput <= 0 || amount <= 0) {
            return 0;
        }
        int count = Math.min(maxOutput, amount);
        journal.updateSnapshots(transaction);
        owner.liquid -= count;
        return count;
    }

    private class LiquidJournal extends SnapshotJournal<Long> {
        @Override
        protected Long createSnapshot() {
            return owner.liquid;
        }

        @Override
        protected void revertToSnapshot(Long snapshot) {
            owner.liquid = snapshot;
        }

        @Override
        protected void onRootCommit(Long originalState) {
            if (owner.liquid != originalState) {
                owner.setChanged();
            }
        }
    }
}
