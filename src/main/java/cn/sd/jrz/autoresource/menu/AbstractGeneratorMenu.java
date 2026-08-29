package cn.sd.jrz.autoresource.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 机器容器基类：实体引用、DataSlot 工具（long 拆高低 32 位）、玩家背包布局与存活校验。
 * 子类负责专属槽位、数据槽、按钮与快速转移，并实现 GUI 展示取值方法。
 *
 * @param <T> 对应机器方块实体类型
 */
public abstract class AbstractGeneratorMenu<T extends BlockEntity> extends AbstractContainerMenu {
    protected final T entity;

    @SuppressWarnings("unchecked")
    protected AbstractGeneratorMenu(MenuType<?> type, int id, Inventory playerInventory, BlockPos pos) {
        super(type, id);
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        this.entity = (T) blockEntity;
    }

    /**
     * 当前产量（服务端读实体，客户端读同步值）
     */
    public abstract long getOutput();

    /**
     * 最大产量
     */
    public abstract long getMax();

    /**
     * 增长 tick 计数
     */
    public abstract int getTickCount();

    /**
     * 增长间隔（秒）
     */
    public abstract int getSecond();

    /**
     * 客户端/服务端都能访问的实体
     */
    public T getEntity() {
        return entity;
    }

    /**
     * 指定方向相邻方块的物品栈（数量 1），无方块或方块无物品时返回空，供 GUI 方向按钮图标展示
     * （默认返回空，由子类按需覆写以提供具体实现）
     */
    @Nonnull
    public ItemStack getNeighborStack(Direction direction) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        if (entity == null) {
            return false;
        }
        return entity.getLevel() != null && entity.getLevel().getBlockEntity(entity.getBlockPos()) == entity;
    }

    /**
     * 玩家背包：三行 + 快捷栏，行距固定 18；baseY 为机器 GUI 中玩家背包首行 y 坐标（各机器不同）
     */
    protected void addPlayerInventory(Inventory playerInventory, int baseY) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, baseY + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, baseY + 54));
        }
    }

    protected static DataSlot makeDataSlot(IntSupplier getter, IntConsumer setter) {
        return new DataSlot() {
            @Override
            public int get() {
                return getter.getAsInt();
            }

            @Override
            public void set(int value) {
                setter.accept(value);
            }
        };
    }

    protected static int hiWord(long value) {
        return (int) (value >> 32);
    }

    protected static int loWord(long value) {
        return (int) (value & 0xFFFFFFFFL);
    }

    protected static long mergeLong(int hi, int lo) {
        return ((long) hi << 32) | (lo & 0xFFFFFFFFL);
    }
}
