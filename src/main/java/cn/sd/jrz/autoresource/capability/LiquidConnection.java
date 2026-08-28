package cn.sd.jrz.autoresource.capability;

import cn.sd.jrz.autoresource.util.ItemFluidIo;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import org.jetbrains.annotations.NotNull;
import java.util.Iterator;

/**
 * 流体生成器对外流体连接：只出不进的单资源存储。
 * 内部数量单位为 mB，对外按 Fabric Transfer API 规则换算为 droplets（81 倍）。
 */
public class LiquidConnection implements Storage<FluidVariant> {

    private final cn.sd.jrz.autoresource.blockentity.LiquidGeneratorEntity owner;

    public LiquidConnection(cn.sd.jrz.autoresource.blockentity.LiquidGeneratorEntity owner) {
        this.owner = owner;
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        // 禁止注入
        return 0;
    }

    @Override
    public boolean supportsInsertion() {
        return false;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (!resource.equals(currentVariant()) || owner.liquid <= 0 || maxAmount <= 0) {
            return 0;
        }
        // 对外以 droplets 计：可用量 = 内部 mB × 81（饱和处理）
        long availableDroplets = internalToDroplets(owner.liquid);
        long extracted = Math.min(maxAmount, availableDroplets);
        if (extracted <= 0) {
            return 0;
        }
        owner.liquid -= dropletsToInternal(extracted);
        transaction.addCloseCallback((txn, result) -> {
            if (result.wasAborted()) {
                owner.liquid += dropletsToInternal(extracted);
            }
        });
        return extracted;
    }

    @Override
    public boolean supportsExtraction() {
        return true;
    }

    @NotNull
    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
        return java.util.List.<StorageView<FluidVariant>>of(new View()).iterator();
    }

    private FluidVariant currentVariant() {
        return FluidVariant.of(owner.config.getFluid());
    }

    /**
     * 内部 mB → droplets（饱和）
     */
    public static long internalToDroplets(long millibuckets) {
        return ItemFluidIo.safeMul(millibuckets, ItemFluidIo.DROPLETS_PER_MB);
    }

    /**
     * droplets → 内部 mB（向下取整；每次抽取不足 1 mB 的零头归入耗损）
     */
    public static long dropletsToInternal(long droplets) {
        return droplets / ItemFluidIo.DROPLETS_PER_MB;
    }

    /**
     * 单一资源视图：读取当前存量，供管道探测
     */
    private class View implements StorageView<FluidVariant> {
        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return LiquidConnection.this.extract(resource, maxAmount, transaction);
        }

        @Override
        public boolean isResourceBlank() {
            return owner.liquid <= 0;
        }

        @NotNull
        @Override
        public FluidVariant getResource() {
            return currentVariant();
        }

        @Override
        public long getAmount() {
            return internalToDroplets(Math.max(0, owner.liquid));
        }

        @Override
        public long getCapacity() {
            return Long.MAX_VALUE;
        }
    }
}
