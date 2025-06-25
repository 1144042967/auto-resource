package cn.sd.jrz.autoresource.connection;

import cn.sd.jrz.autoresource.entities.BlockGeneratorEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

public class BlockConnection implements IItemHandler {
    private final BlockGeneratorEntity owner;
    private final ItemStack stack;

    public BlockConnection(BlockGeneratorEntity owner) {
        this.owner = owner;
        this.stack = new ItemStack(owner.config.getBlock().asItem(), 0);
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        stack.setCount((int) (owner.block / 1000));
        return stack;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (owner.block / 1000 <= 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int ret = (int) Math.min(owner.block / 1000, amount);
        if (!simulate) {
            owner.block -= ret * 1000L;
            owner.setChanged();
        }
        return new ItemStack(owner.config.getBlock().asItem(), ret);
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return false;
    }
}
