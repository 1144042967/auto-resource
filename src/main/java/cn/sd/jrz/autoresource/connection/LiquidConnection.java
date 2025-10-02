package cn.sd.jrz.autoresource.connection;

import cn.sd.jrz.autoresource.entities.LiquidGeneratorEntity;
import cn.sd.jrz.autoresource.util.Tool;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nonnull;

public class LiquidConnection implements ResourceHandler<FluidResource> {
    private final LiquidGeneratorEntity owner;

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
        return Long.MAX_VALUE;
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
        owner.liquid -= count;
        owner.setChanged();
        return count;
    }
}
