package cn.sd.jrz.autoresource.connection;

import cn.sd.jrz.autoresource.entities.EnergyGeneratorEntity;
import cn.sd.jrz.autoresource.util.Tool;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nonnull;

public class EnergyConnection implements EnergyHandler {
    private final EnergyGeneratorEntity owner;

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
        return 0;
    }

    @Override
    public int extract(int amount, @Nonnull TransactionContext transaction) {
        int maxOutput = Tool.suitInt(owner.energy);
        if (maxOutput <= 0 || amount <= 0) {
            return 0;
        }
        int ret = Math.min(maxOutput, amount);
        owner.energy -= ret;
        owner.setChanged();
        return ret;
    }
}
