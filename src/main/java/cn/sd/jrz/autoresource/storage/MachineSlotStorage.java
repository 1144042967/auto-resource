package cn.sd.jrz.autoresource.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 机器内部槽位容器（对应 Forge 版使用的 ItemStackHandler 语义）：
 * - 每槽可独立设置堆叠上限 {@link #setSlotLimit} 与放入校验 {@link #setValidator}
 * - 保留 Forge 风格的 insertItem/extractItem(slot, amount, simulate) API
 * - 直接实现 vanilla {@link Container}，因此可被原生 {@link net.minecraft.world.inventory.Slot} 包装
 * - NBT 键位与 Forge 版一致（Size/Items[{Slot,id,Count}]），确保旧存档内容可继续读取
 * 内容变化通过 {@link #onContentsChanged(int)} 回调通知持有者（实体借此 setChanged/sendBlockUpdated）
 */
public class MachineSlotStorage implements Container {

    protected final List<ItemStack> stacks;
    /**
     * 各槽独立堆叠上限（<=0 表示跟随物品自身 maxStackSize）
     */
    protected final int[] slotLimits;
    @Nullable
    protected Predicate<ItemStack> validator;

    public MachineSlotStorage(int size) {
        this.stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            stacks.add(ItemStack.EMPTY);
        }
        this.slotLimits = new int[size];
    }

    public MachineSlotStorage setSize(int size) {
        while (stacks.size() > size) {
            stacks.remove(stacks.size() - 1);
        }
        while (stacks.size() < size) {
            stacks.add(ItemStack.EMPTY);
        }
        return this;
    }

    /**
     * 设置放入校验器（返回 false 则该槽拒绝该物品）
     */
    public MachineSlotStorage setValidator(@Nullable Predicate<ItemStack> validator) {
        this.validator = validator;
        return this;
    }

    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return validator == null || validator.test(stack);
    }

    public int getSlotLimit(int slot) {
        return slotLimits[slot] <= 0 ? Integer.MAX_VALUE : slotLimits[slot];
    }

    public MachineSlotStorage setSlotLimit(int slot, int limit) {
        slotLimits[slot] = limit;
        return this;
    }

    /**
     * 向指定槽插入物品并返回剩余部分；simulate 为 true 时只计算不入账
     */
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!isItemValid(slot, stack)) {
            return stack;
        }
        ItemStack existing = stacks.get(slot);

        int limit = getStackLimit(slot, existing);
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(existing, stack)) {
                return stack;
            }
            limit -= existing.getCount();
        }
        if (limit <= 0) {
            return stack;
        }
        boolean reachedLimit = stack.getCount() > limit;
        if (!simulate) {
            if (existing.isEmpty()) {
                stacks.set(slot, reachedLimit ? stack.split(limit) : stack.split(stack.getCount()));
            } else {
                existing.grow(reachedLimit ? limit : stack.getCount());
            }
            onContentsChanged(slot);
        } else if (reachedLimit) {
            // 模拟模式下扣减副本计数以返回剩余量
            stack = stack.copy();
            stack.shrink(limit);
        }
        return reachedLimit ? stack : ItemStack.EMPTY;
    }

    /**
     * 从指定槽提取至多 amount 个物品；simulate 为 true 时只计算不入账
     */
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int toExtract = Math.min(amount, existing.getCount());
        ItemStack extracted = existing.copy();
        extracted.setCount(toExtract);
        if (!simulate) {
            existing.shrink(toExtract);
            if (existing.isEmpty()) {
                stacks.set(slot, ItemStack.EMPTY);
            }
            onContentsChanged(slot);
        }
        return extracted;
    }

    /**
     * 单槽实际可容纳数量（受独立上限与物品自身堆叠上限双重约束）
     */
    public int getStackLimit(int slot, @Nonnull ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    /**
     * 内容变化钩子：子类覆写或持有者据此持久化/触发方块更新
     */
    protected void onContentsChanged(int slot) {
        setChanged();
    }

    // ==================== NBT 读写（键位兼容 Forge 版存档） ====================

    public CompoundTag serializeNBT() {
        ListTag nbtTagList = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            if (!stacks.get(i).isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                stacks.get(i).save(itemTag);
                nbtTagList.add(itemTag);
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", nbtTagList);
        nbt.putInt("Size", stacks.size());
        return nbt;
    }

    public MachineSlotStorage deserializeNBT(CompoundTag nbt) {
        setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : stacks.size());
        ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTags = tagList.getCompound(i);
            int slot = itemTags.getInt("Slot");
            if (slot >= 0 && slot < stacks.size()) {
                stacks.set(slot, ItemStack.of(itemTags));
            }
        }
        onChanged();
        return this;
    }

    /**
     * 反序列化后的整体刷新钩子（默认 no-op）
     */
    protected void onChanged() {
    }

    // ==================== Container 实现（供 vanilla Slot 与 GUI 使用） ====================

    @Override
    public int getContainerSize() {
        return stacks.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Nonnull
    @Override
    public ItemStack getItem(int slot) {
        validateIndex(slot);
        return stacks.get(slot);
    }

    @Nonnull
    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(stacks, slot, amount);
        if (!removed.isEmpty()) {
            onContentsChanged(slot);
        }
        return removed;
    }

    @Nonnull
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(stacks, slot);
    }

    @Override
    public void setItem(int slot, @Nonnull ItemStack stack) {
        validateIndex(slot);
        stacks.set(slot, stack);
        // 超出槽限制的部分截断
        int limit = getStackLimit(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > limit) {
            stack.setCount(limit);
        }
        onContentsChanged(slot);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
        onChanged();
    }

    @Override
    public void setChanged() {
        // 由持有实体覆写为持久化标记；此处默认无操作（无世界上下文）
    }

    @Override
    public boolean stillValid(@Nullable Player player) {
        return true;
    }

    private void validateIndex(int slot) {
        if (slot < 0 || slot >= stacks.size()) {
            throw new IndexOutOfBoundsException("Slot index " + slot + " out of range [0, " + stacks.size() + ")");
        }
    }
}
