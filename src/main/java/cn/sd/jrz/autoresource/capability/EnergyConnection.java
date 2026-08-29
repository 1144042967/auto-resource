package cn.sd.jrz.autoresource.capability;

import cn.sd.jrz.autoresource.blockentity.EnergyGeneratorEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import team.reborn.energy.api.EnergyStorage;

/**
 * FE 发电机对外能量连接：只出不进。
 * - extract 供周边机器/管道抽电（事务中止时自动回滚）
 * - insert 恒返回 0（禁止向发电机注入）
 */
public class EnergyConnection implements EnergyStorage {

    private final EnergyGeneratorEntity owner;

    public EnergyConnection(EnergyGeneratorEntity owner) {
        this.owner = owner;
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        // 禁止输入
        return 0;
    }

    @Override
    public boolean supportsInsertion() {
        return false;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        long extracted = Math.min(maxAmount, owner.energy);
        if (extracted <= 0) {
            return 0;
        }
        owner.energy -= extracted;
        // 事务中止时把扣掉的电量补回去，保持与其他存储一致的事务语义
        transaction.addCloseCallback((txn, result) -> {
            if (result.wasAborted()) {
                owner.energy += extracted;
            }
        });
        return extracted;
    }

    @Override
    public boolean supportsExtraction() {
        return true;
    }

    @Override
    public long getAmount() {
        return Math.max(0, owner.energy);
    }

    /**
     * 存储容量：发电机内部储能为无上限设计
     */
    @Override
    public long getCapacity() {
        return Long.MAX_VALUE;
    }
}
