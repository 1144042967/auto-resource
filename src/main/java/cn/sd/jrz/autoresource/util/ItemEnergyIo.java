package cn.sd.jrz.autoresource.util;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

/**
 * 物品能量读写兼容层（替代 Forge 版对 ForgeCapabilities.ENERGY 的逐栈查询）。
 * Fabric 生态经由 teamreborn/fabric transfer 的 {@link EnergyStorage#ITEM} 查找；
 * 充电需要物品处于"容器上下文"中才能持久生效：
 * - 位于可寻址槽位时传 SlottedStorage（变更自动写回原槽）
 * - 装备/手持槽等特殊位置用一次性上下文读取结果后由调用方回写
 */
public final class ItemEnergyIo {

    private ItemEnergyIo() {
    }

    /**
     * 判断物品是否可接收能量（存在支持插入且未满的能量存储）
     */
    public static boolean canReceive(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        EnergyStorage storage = findWithInitial(stack);
        return storage != null && storage.supportsInsertion() && storage.getAmount() < storage.getCapacity();
    }

    /**
     * 向位于给定单槽存储视图中的物品充能（上下文保证变更回写），返回实际充入量
     *
     * @param slot 物品所在的单槽存储视图（如 {@code InventoryStorage.of(container,null).getSlot(i)}）
     */
    public static long receive(net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> slot, long maxAmount) {
        if (maxAmount <= 0) {
            return 0;
        }
        ContainerItemContext context = ContainerItemContext.ofSingleSlot(slot);
        EnergyStorage storage = context.find(EnergyStorage.ITEM);
        if (storage == null || !storage.supportsInsertion()) {
            return 0;
        }
        try (Transaction txn = Transaction.openOuter()) {
            long received = Math.min(storage.insert(Math.min(maxAmount, Long.MAX_VALUE), txn), maxAmount);
            txn.commit();
            return Math.max(0, received);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 一次性上下文向独立物品充能，返回 [充入量, 结果堆栈]；用于没有对应存储视图的场景（如装备槽），
     * 调用方负责将结果堆栈写回原位置；不可充电时返回 null。
     */
    @SuppressWarnings("removal")
    @Nullable
    public static Result receiveStandalone(ItemStack stack, long maxAmount) {
        if (stack.isEmpty() || maxAmount <= 0) {
            return null;
        }
        ContainerItemContext context = ContainerItemContext.withInitial(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(stack), stack.getCount());
        EnergyStorage storage = context.find(EnergyStorage.ITEM);
        if (storage == null || !storage.supportsInsertion()) {
            return null;
        }
        try (Transaction txn = Transaction.openOuter()) {
            long received = storage.insert(maxAmount, txn);
            txn.commit();
            if (received <= 0) {
                return null;
            }
            ItemStack filled = context.getItemVariant().toStack((int) Math.max(1, context.getAmount()));
            return new Result(received, filled);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("removal")
    private static EnergyStorage findWithInitial(ItemStack stack) {
        ContainerItemContext context = ContainerItemContext.withInitial(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(stack), stack.getCount());
        return context.find(EnergyStorage.ITEM);
    }

    /**
     * standalone 充电结果：实际充入量与变换后的物品堆栈
     */
    public record Result(long received, ItemStack filled) {
    }
}
