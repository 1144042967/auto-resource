package cn.sd.jrz.autoresource.menu;

import cn.sd.jrz.autoresource.entities.LiquidGeneratorEntity;
import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 流体生成器容器（水源机/岩浆机）。
 * <p>
 * 包含输入槽（0，放入空桶或可容纳流体的物品）、输出槽（1，存放已填满的桶/物品）以及玩家背包。
 * 通过数据槽把流体量、产量、下次增长量、增长进度、六面开关等同步到客户端用于 GUI 展示，
 * 并在 GUI 中通过按钮（clickMenuButton）修改每台机器的六面传输开关。
 */
public class LiquidGeneratorMenu extends AbstractContainerMenu {
    // 按钮 ID
    public static final int BUTTON_TRANSFER_DOWN = 0;
    public static final int BUTTON_TRANSFER_UP = 1;
    public static final int BUTTON_TRANSFER_NORTH = 2;
    public static final int BUTTON_TRANSFER_SOUTH = 3;
    public static final int BUTTON_TRANSFER_WEST = 4;
    public static final int BUTTON_TRANSFER_EAST = 5;

    public final LiquidGeneratorEntity entity;

    // 客户端展示数据（服务端通过数据槽同步而来）
    private long clientLiquid;
    private long clientOutput;
    private long clientStep;
    private int clientTickCount;
    private int clientSecond;
    private boolean clientTransferDown;
    private boolean clientTransferUp;
    private boolean clientTransferNorth;
    private boolean clientTransferSouth;
    private boolean clientTransferWest;
    private boolean clientTransferEast;

    public LiquidGeneratorMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(Registration.LIQUID_GENERATOR_MENU.get(), id);
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        this.entity = (LiquidGeneratorEntity) blockEntity;

        // 机器槽位：0=输入，1=输出
        addSlot(new SlotItemHandler(entity.inputSlot, 0, 8, 113));
        addSlot(new SlotItemHandler(entity.outputSlot, 0, 152, 113));
        // 玩家背包：2-37
        addPlayerInventory(playerInventory);

        // 数据同步（long 拆成高低 32 位两个数据槽）
        addDataSlot(makeDataSlot(() -> hiWord(entity.liquid), v -> clientLiquid = mergeLong(v, loWord(clientLiquid))));
        addDataSlot(makeDataSlot(() -> loWord(entity.liquid), v -> clientLiquid = mergeLong(hiWord(clientLiquid), v)));
        addDataSlot(makeDataSlot(() -> hiWord(entity.output), v -> clientOutput = mergeLong(v, loWord(clientOutput))));
        addDataSlot(makeDataSlot(() -> loWord(entity.output), v -> clientOutput = mergeLong(hiWord(clientOutput), v)));
        addDataSlot(makeDataSlot(() -> hiWord(entity.config.getStep()), v -> clientStep = mergeLong(v, loWord(clientStep))));
        addDataSlot(makeDataSlot(() -> loWord(entity.config.getStep()), v -> clientStep = mergeLong(hiWord(clientStep), v)));
        addDataSlot(makeDataSlot(() -> (int) Math.min(Integer.MAX_VALUE, entity.tickCount), v -> clientTickCount = v));
        addDataSlot(makeDataSlot(() -> (int) Math.min(Integer.MAX_VALUE, entity.config.getSecond()), v -> clientSecond = v));
        addDataSlot(makeDataSlot(() -> entity.transferDown ? 1 : 0, v -> clientTransferDown = v != 0));
        addDataSlot(makeDataSlot(() -> entity.transferUp ? 1 : 0, v -> clientTransferUp = v != 0));
        addDataSlot(makeDataSlot(() -> entity.transferNorth ? 1 : 0, v -> clientTransferNorth = v != 0));
        addDataSlot(makeDataSlot(() -> entity.transferSouth ? 1 : 0, v -> clientTransferSouth = v != 0));
        addDataSlot(makeDataSlot(() -> entity.transferWest ? 1 : 0, v -> clientTransferWest = v != 0));
        addDataSlot(makeDataSlot(() -> entity.transferEast ? 1 : 0, v -> clientTransferEast = v != 0));
    }

    /**
     * 客户端/服务端都能访问的展示值（服务端读实体，客户端读同步值）
     */
    public long getLiquid() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.liquid : clientLiquid;
    }

    public long getOutput() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.output : clientOutput;
    }

    public long getStep() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.config.getStep() : clientStep;
    }

    public long getMax() {
        return entity != null ? entity.config.getMax() : Long.MAX_VALUE;
    }

    /** 本机对应的流体（水源机为水，岩浆机为岩浆），用于 GUI 进度条配色 */
    public Fluid getFluid() {
        return entity != null ? entity.config.getFluid() : Fluids.WATER;
    }

    public int getTickCount() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? (int) Math.min(Integer.MAX_VALUE, entity.tickCount) : clientTickCount;
    }

    public int getSecond() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? (int) Math.min(Integer.MAX_VALUE, entity.config.getSecond()) : clientSecond;
    }

    public boolean isFaceEnabled(Direction direction) {
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide) {
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
     * 处理 GUI 按钮点击（六面传输开关）
     */
    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (entity == null || player.level().isClientSide) {
            return false;
        }
        switch (id) {
            case BUTTON_TRANSFER_DOWN -> entity.transferDown = !entity.transferDown;
            case BUTTON_TRANSFER_UP -> entity.transferUp = !entity.transferUp;
            case BUTTON_TRANSFER_NORTH -> entity.transferNorth = !entity.transferNorth;
            case BUTTON_TRANSFER_SOUTH -> entity.transferSouth = !entity.transferSouth;
            case BUTTON_TRANSFER_WEST -> entity.transferWest = !entity.transferWest;
            case BUTTON_TRANSFER_EAST -> entity.transferEast = !entity.transferEast;
            default -> {
                return false;
            }
        }
        entity.setChanged();
        return true;
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        if (entity == null) {
            return false;
        }
        return entity.getLevel() != null && entity.getLevel().getBlockEntity(entity.getBlockPos()) == entity;
    }

    /**
     * 快速转移物品
     */
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemStack = stack.copy();
            if (index < 2) {
                // 机器槽 -> 玩家背包
                if (!this.moveItemStackTo(stack, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包 -> 优先输入槽，其次输出槽
                if (!this.moveItemStackTo(stack, 0, 1, false)) {
                    if (!this.moveItemStackTo(stack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return itemStack;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 153 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 207));
        }
    }

    private static DataSlot makeDataSlot(IntSupplier getter, IntConsumer setter) {
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

    private static int hiWord(long value) {
        return (int) (value >> 32);
    }

    private static int loWord(long value) {
        return (int) (value & 0xFFFFFFFFL);
    }

    private static long mergeLong(int hi, int lo) {
        return ((long) hi << 32) | (lo & 0xFFFFFFFFL);
    }
}
