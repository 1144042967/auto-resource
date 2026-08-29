package cn.sd.jrz.autoresource.capability;

import cn.sd.jrz.autoresource.blockentity.LiquidGeneratorEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 流体生成器的物品管道视图（对应 Forge 版暴露的匿名 IItemHandler）：
 * - 插入只能进入输入槽（槽 0），抽取只能来自输出槽（槽 1），保证单向流动
 * - 以 CombinedStorage 组合两个 SingleStackStorage 子视图实现；
 * SingleStackStorage 自带事务快照，事务中止时自动回滚堆栈
 */
public class DualSlotPipeView extends CombinedStorage<ItemVariant, Storage<ItemVariant>> {

    public DualSlotPipeView(LiquidGeneratorEntity owner) {
        super(List.of(new InputView(owner), new OutputView(owner)));
    }

    /**
     * 输入槽视图：仅允许插入输入槽认可物品（空桶/可容纳本机流体的容器），禁止抽取
     */
    private static class InputView extends SingleStackStorage {
        private final LiquidGeneratorEntity owner;

        InputView(LiquidGeneratorEntity owner) {
            this.owner = owner;
        }

        @Override
        @NotNull
        protected ItemStack getStack() {
            return owner.inputSlot.getItem(0);
        }

        @Override
        protected void setStack(@NotNull ItemStack stack) {
            owner.inputSlot.setItem(0, stack);
        }

        /**
         * 管道插入校验与 GUI 一致（复用输入槽自身的 validator）
         */
        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (maxAmount <= 0 || resource.isBlank()) {
                return 0;
            }
            // validator 只关心物品种类，取 1 件样本校验即可
            if (!owner.inputSlot.isItemValid(0, resource.toStack(1))) {
                return 0;
            }
            return super.insert(resource, maxAmount, transaction);
        }

        /**
         * 输入槽内物品不允许被管道抽取（单向流入）
         */
        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }
    }

    /**
     * 输出槽视图：仅可抽取机器放入的成品（流体桶/已灌装容器），禁止外部插入
     */
    private static class OutputView extends SingleStackStorage {
        private final LiquidGeneratorEntity owner;

        OutputView(LiquidGeneratorEntity owner) {
            this.owner = owner;
        }

        @Override
        @NotNull
        protected ItemStack getStack() {
            return owner.outputSlot.getItem(0);
        }

        @Override
        protected void setStack(@NotNull ItemStack stack) {
            owner.outputSlot.setItem(0, stack);
        }

        /**
         * 输出槽禁止外部插入（只能由机器填满后转入）
         */
        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }
    }
}
