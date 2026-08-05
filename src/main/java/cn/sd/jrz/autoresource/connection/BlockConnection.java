package cn.sd.jrz.autoresource.connection;

import cn.sd.jrz.autoresource.entities.BlockGeneratorEntity;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nonnull;

public class BlockConnection implements IItemHandler {
    private final BlockGeneratorEntity owner;
    private ItemStack stack = ItemStack.EMPTY;

    public BlockConnection(BlockGeneratorEntity owner) {
        this.owner = owner;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public @Nonnull ItemStack getStackInSlot(int slot) {
        // 未标记时无法输出
        if (owner.getMarkedItem().isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty() || !stack.is(owner.getMarkedItem().getItem())) {
            stack = new ItemStack(owner.getMarkedItem().getItem(), 0);
        }
        stack.setCount(Tool.suitInt(owner.block / 1000));
        return stack;
    }

    @Override
    public @Nonnull ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public @Nonnull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (owner.getMarkedItem().isEmpty()) {
            return ItemStack.EMPTY;
        }
        int maxOutput = Tool.suitInt(owner.block / 1000);
        if (maxOutput <= 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int ret = Math.min(maxOutput, amount);
        if (!simulate) {
            owner.block -= ret * 1000L;
            owner.setChanged();
        }
        return new ItemStack(owner.getMarkedItem().getItem(), ret);
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return false;
    }
}
