package cn.sd.jrz.autoresource.util;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import org.jetbrains.annotations.Nullable;

/**
 * 物品流体温养读写兼容层（替代 Forge 版对 ForgeCapabilities.FLUID_HANDLER_ITEM 的逐栈查询），
 * 经由 Fabric Transfer API 的 {@code FluidStorage.ITEM} 查找；vanilla 空桶不在其列（由调用方特判）。
 * <p>
 * 单位换算：Fabric Transfer API 流体数量为 droplets，官方规定 1 mB = 81 droplets、1 桶 = 81000 droplets；
 * 本机内部存储单位即 mB（输出显示除以 1000 折算为桶）。
 */
public final class ItemFluidIo {

    /**
     * 每 mB 对应的 droplet 数（fabric 官方常量等价：FluidConstants.BUCKET=81000）
     */
    public static final long DROPLETS_PER_MB = 81L;

    private ItemFluidIo() {
    }

    /**
     * 物品是否能容纳指定流体（存在支持插入的能量存储，且内容为空或同种流体且有剩余容量）
     */
    public static boolean accepts(ItemStack stack, Fluid fluid) {
        Storage<FluidVariant> storage = findWithInitial(stack);
        if (storage == null || !storage.supportsInsertion()) {
            return false;
        }
        try (Transaction ignored = Transaction.openOuter()) {
            FluidVariant variant = FluidVariant.of(fluid);
            long currentlyHeld = 0;
            boolean hasCompatibleTank = false;
            // 遍历现有视图：任何空仓/已含同流体且有容量的视图均可视为可容纳
            for (var view : storage) {
                if (view.isResourceBlank()) {
                    hasCompatibleTank = view.getCapacity() > 0;
                } else if (view.getResource().equals(variant)) {
                    hasCompatibleTank = view.getAmount() < view.getCapacity();
                }
                currentlyHeld += view.getAmount();
                if (hasCompatibleTank) {
                    break;
                }
            }
            return hasCompatibleTank || currentlyHeld == 0 && anyCapacity(storage, variant);
        }
    }

    /**
     * 全部视图均为空时检查是否存在正容量（empty-combined-storage 场景兜底）
     */
    private static boolean anyCapacity(Storage<FluidVariant> storage, FluidVariant variant) {
        try (Transaction txn = Transaction.openOuter()) {
            return storage.insert(variant, 1, txn) > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 向独立物品灌入流体，最多灌入 {@code maxMillibuckets} mB，返回 [实际灌入 mB, 变换后的物品堆栈]；
     * 不能灌入（无能力/不接受该流体）时返回 null。调用方决定结果堆栈的去处（回写输入槽/转入输出槽）。
     */
    @Nullable
    public static FillResult fill(ItemStack stack, Fluid fluid, long maxMillibuckets) {
        if (stack.isEmpty() || maxMillibuckets <= 0) {
            return null;
        }
        ContainerItemContext context = ContainerItemContext.withConstant(ItemVariant.of(stack), stack.getCount());
        Storage<FluidVariant> storage = context.find(FluidStorage.ITEM);
        if (storage == null || !storage.supportsInsertion()) {
            return null;
        }
        FluidVariant variant = FluidVariant.of(fluid);
        long requestedDroplets = safeMul(maxMillibuckets, DROPLETS_PER_MB);
        if (requestedDroplets <= 0) {
            return null;
        }
        try (Transaction probe = Transaction.openOuter()) {
            long simulateFilled = storage.insert(variant, requestedDroplets, probe);
            if (simulateFilled <= 0) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        try (Transaction txn = Transaction.openOuter()) {
            long filledDroplets = storage.insert(variant, requestedDroplets, txn);
            txn.commit();
            if (filledDroplets <= 0) {
                return null;
            }
            // 扣减取整到 mB（<81 droplet 的零头归入耗损，保证机器侧守恒到 mB 精度）
            long millibuckets = filledDroplets / DROPLETS_PER_MB;
            if (millibuckets <= 0) {
                return null;
            }
            int count = (int) Math.max(1, context.getAmount());
            ItemStack filled = context.getItemVariant().toStack(count);
            return new FillResult(millibuckets, filled);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断物品是否已无法容纳更多指定流体（视为已满）；用于"填满后转移输出槽"判定
     */
    public static boolean isFull(ItemStack stack, Fluid fluid) {
        Storage<FluidVariant> storage = findWithInitial(stack);
        if (storage == null) {
            return true;
        }
        try (Transaction txn = Transaction.openOuter()) {
            long remaining = storage.insert(FluidVariant.of(fluid), DROPLETS_PER_MB, txn);
            return remaining <= 0;
        } catch (Exception e) {
            return true;
        }
    }

    private static Storage<FluidVariant> findWithInitial(ItemStack stack) {
        ContainerItemContext context = ContainerItemContext.withConstant(ItemVariant.of(stack), stack.getCount());
        return context.find(FluidStorage.ITEM);
    }

    /**
     * 饱和乘法：避免大数值时的 long 溢出
     */
    public static long safeMul(long a, long b) {
        if (a <= 0 || b <= 0) {
            return 0;
        }
        long value = a * b;
        return value / b != a ? Long.MAX_VALUE : value;
    }

    /**
     * 灌装结果：实际消耗的机器 mB 与变换后的物品
     */
    public record FillResult(long millibuckets, ItemStack filled) {
    }
}
