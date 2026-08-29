package cn.sd.jrz.autoresource.storage;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 机器槽位 GUI 包装（对应 Forge 的 SlotItemHandler）：
 * - mayPlace 委托 storage.isItemValid
 * - 单槽堆叠上限委托 storage.getSlotLimit（如输出槽上限为 1、输入槽随物品）
 * 默认允许取出；标记槽等特殊行为由子类覆写 mayPickup
 */
public class MachineSlot extends Slot {

    public MachineSlot(MachineSlotStorage container, int index, int x, int y) {
        super(container, index, x, y);
    }

    /**
     * 暴露 holder 容器（vanilla Slot 字段 {@code container} 在 Mojang 命名下仍为 public final）
     */
    public MachineSlotStorage container() {
        return (MachineSlotStorage) this.container;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return container().isItemValid(this.getContainerSlot(), stack);
    }

    /**
     * GUI 点击放入时的数量限制（vanilla 用 stack.getMaxStackSize 与 container.getMaxStackSize 取小）
     */
    @Override
    public int getMaxStackSize(ItemStack stack) {
        // 基于具体 stack 的实际限制：物品自身上限与槽位自定义上限取小
        return container().getStackLimit(this.getContainerSlot(), stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return true;
    }
}
