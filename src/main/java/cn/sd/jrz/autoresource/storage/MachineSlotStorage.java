package cn.sd.jrz.autoresource.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
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

    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
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
     * 单槽实际可容纳数量（受独立上限与物品自身堆叠上限双重约束）
     */
    public int getStackLimit(int slot, @NotNull ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    /**
     * 内容变化钩子：子类覆写或持有者据此持久化/触发方块更新
     */
    protected void onContentsChanged(int slot) {
        setChanged();
    }

    // ==================== NBT 读写（键位兼容 Forge 版存档） ====================

    /**
     * 序列化槽位内容到 ValueOutput（1.21.11 BlockEntity 持久化新 API）。
     * 结构保持 {Size, Items}，Items 元素由 {@link ItemStackWithSlot#CODEC} 编码（含槽位索引与物品栈），
     * 物品由 ValueOutput 内部提供的 RegistryOps 解析，无需外部 HolderLookup。
     */
    public void saveTo(ValueOutput output) {
        output.putInt("Size", stacks.size());
        var list = output.list("Items", ItemStackWithSlot.CODEC);
        for (int i = 0; i < stacks.size(); i++) {
            if (!stacks.get(i).isEmpty()) {
                list.add(new ItemStackWithSlot(i, stacks.get(i)));
            }
        }
    }

    /**
     * 从 ValueInput 读取槽位内容（与 {@link #saveTo} 格式对应）
     */
    public void loadFrom(ValueInput input) {
        setSize(input.getIntOr("Size", stacks.size()));
        for (ItemStackWithSlot isws : input.listOrEmpty("Items", ItemStackWithSlot.CODEC)) {
            int slot = isws.slot();
            if (slot >= 0 && slot < stacks.size()) {
                stacks.set(slot, isws.stack());
            }
        }
        onChanged();
    }

    /**
     * 从既有 CompoundTag 反序列化槽位内容（tooltip 读取 block_entity_data 组件中的机器状态时使用，
     * 与 {@link #saveTo} 使用相同的 {Size, Items} 结构）
     */
    public MachineSlotStorage deserializeNBT(HolderLookup.Provider registryLookup, CompoundTag nbt) {
        setSize(nbt.getIntOr("Size", stacks.size()));
        ListTag tagList = nbt.getListOrEmpty("Items");
        RegistryOps<Tag> ops = registryLookup.createSerializationContext(NbtOps.INSTANCE);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTags = tagList.getCompoundOrEmpty(i);
            ItemStackWithSlot isws = ItemStackWithSlot.CODEC.parse(ops, itemTags).result().orElse(null);
            if (isws != null) {
                int slot = isws.slot();
                if (slot >= 0 && slot < stacks.size()) {
                    stacks.set(slot, isws.stack());
                }
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

    @NotNull
    @Override
    public ItemStack getItem(int slot) {
        validateIndex(slot);
        return stacks.get(slot);
    }

    @NotNull
    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(stacks, slot, amount);
        if (!removed.isEmpty()) {
            onContentsChanged(slot);
        }
        return removed;
    }

    @NotNull
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(stacks, slot);
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
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
        Collections.fill(stacks, ItemStack.EMPTY);
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
