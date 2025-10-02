package cn.sd.jrz.autoresource.connection;

import cn.sd.jrz.autoresource.entities.BlockGeneratorEntity;
import cn.sd.jrz.autoresource.util.Tool;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nonnull;

public class BlockConnection implements ResourceHandler<ItemResource> {
    private final BlockGeneratorEntity owner;

    public BlockConnection(BlockGeneratorEntity owner) {
        this.owner = owner;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public @Nonnull ItemResource getResource(int index) {
        return ItemResource.of(owner.config.getBlock().asItem());
    }

    @Override
    public long getAmountAsLong(int index) {
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
        return 0;
    }

    @Override
    public int extract(int index, @Nonnull ItemResource resource, int amount, @Nonnull TransactionContext transaction) {
        if (!resource.is(owner.config.getBlock().asItem())) {
            return 0;
        }
        int maxOutput = Tool.suitInt(owner.block / 1000);
        if (maxOutput <= 0 || amount <= 0) {
            return 0;
        }
        int count = Math.min(maxOutput, amount);
        owner.block -= count * 1000L;
        owner.setChanged();
        return count;
    }
}
