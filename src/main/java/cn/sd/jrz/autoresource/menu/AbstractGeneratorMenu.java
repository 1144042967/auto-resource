package cn.sd.jrz.autoresource.menu;

import cn.sd.jrz.autoresource.entities.AbstractGeneratorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 机器容器基类：实体引用、DataSlot 工具（long 拆高低 32 位）、六面开关/主动输出总开关的
 * 数据槽同步与读取、玩家背包布局与存活校验。子类负责专属槽位、数据槽、按钮与快速转移，
 * 并实现 GUI 展示取值方法。
 *
 * @param <T> 对应机器方块实体类型
 */
public abstract class AbstractGeneratorMenu<T extends AbstractGeneratorEntity> extends AbstractContainerMenu {
    protected final T entity;

    // 客户端展示数据（服务端通过数据槽同步而来）
    protected boolean clientTransferDown = true;
    protected boolean clientTransferUp = true;
    protected boolean clientTransferNorth = true;
    protected boolean clientTransferSouth = true;
    protected boolean clientTransferWest = true;
    protected boolean clientTransferEast = true;
    protected boolean clientOutputEnabled = true;

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
     * 同步六面传输开关数据槽（子类在构造器注册完机器槽后调用）
     */
    protected void addTransferFaceDataSlots() {
        if (entity == null) {
            return;
        }
        addDataSlot(makeDataSlot(() -> entity.isTransferEnabled(Direction.DOWN) ? 1 : 0, v -> clientTransferDown = v != 0));
        addDataSlot(makeDataSlot(() -> entity.isTransferEnabled(Direction.UP) ? 1 : 0, v -> clientTransferUp = v != 0));
        addDataSlot(makeDataSlot(() -> entity.isTransferEnabled(Direction.NORTH) ? 1 : 0, v -> clientTransferNorth = v != 0));
        addDataSlot(makeDataSlot(() -> entity.isTransferEnabled(Direction.SOUTH) ? 1 : 0, v -> clientTransferSouth = v != 0));
        addDataSlot(makeDataSlot(() -> entity.isTransferEnabled(Direction.WEST) ? 1 : 0, v -> clientTransferWest = v != 0));
        addDataSlot(makeDataSlot(() -> entity.isTransferEnabled(Direction.EAST) ? 1 : 0, v -> clientTransferEast = v != 0));
    }

    /**
     * 同步主动输出总开关数据槽（子类在构造器注册完机器槽后调用）
     */
    protected void addOutputEnabledDataSlot() {
        if (entity == null) {
            return;
        }
        addDataSlot(makeDataSlot(() -> entity.outputEnabled ? 1 : 0, v -> clientOutputEnabled = v != 0));
    }

    /**
     * 指定面传输开关状态（服务端读实体，客户端读同步值）
     */
    public boolean isFaceEnabled(Direction direction) {
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide()) {
            return entity.isTransferEnabled(direction);
        }
        return switch (direction) {
            case DOWN -> clientTransferDown;
            case UP -> clientTransferUp;
            case NORTH -> clientTransferNorth;
            case SOUTH -> clientTransferSouth;
            case WEST -> clientTransferWest;
            case EAST -> clientTransferEast;
        };
    }

    /**
     * 主动输出总开关状态（服务端读实体，客户端读同步值）
     */
    public boolean isOutputEnabled() {
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide()) {
            return entity.outputEnabled;
        }
        return clientOutputEnabled;
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
