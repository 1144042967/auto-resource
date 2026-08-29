package cn.sd.jrz.autoresource.connection;

import cn.sd.jrz.autoresource.entities.EnergyGeneratorEntity;
import cn.sd.jrz.autoresource.util.Tool;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nonnull;

/**
 * 能量输出连接（26.x 传输 API）。
 * <p>
 * 只允许提取（insert 返回 0），提取时在事务上下文中扣减机器能量，
 * 事务回滚时恢复能量，提交时通知实体保存。
 */
public class EnergyConnection implements EnergyHandler {
    private final EnergyGeneratorEntity owner;
    private final EnergyJournal journal = new EnergyJournal();

    public EnergyConnection(EnergyGeneratorEntity owner) {
        this.owner = owner;
    }

    @Override
    public long getAmountAsLong() {
        return owner.energy;
    }

    @Override
    public long getCapacityAsLong() {
        return Long.MAX_VALUE;
    }

    @Override
    public int insert(int amount, @Nonnull TransactionContext transaction) {
        // 禁止输入
        return 0;
    }

    @Override
    public int extract(int amount, @Nonnull TransactionContext transaction) {
        int maxOutput = Tool.suitInt(owner.energy);
        if (maxOutput <= 0 || amount <= 0) {
            return 0;
        }
        int ret = Math.min(maxOutput, amount);
        journal.updateSnapshots(transaction);
        owner.energy -= ret;
        return ret;
    }

    private class EnergyJournal extends SnapshotJournal<Long> {
        @Override
        protected Long createSnapshot() {
            return owner.energy;
        }

        @Override
        protected void revertToSnapshot(Long snapshot) {
            owner.energy = snapshot;
        }

        @Override
        protected void onRootCommit(Long originalState) {
            if (owner.energy != originalState) {
                owner.setChanged();
            }
        }
    }
}
