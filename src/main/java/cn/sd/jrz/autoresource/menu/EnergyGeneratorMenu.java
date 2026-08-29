package cn.sd.jrz.autoresource.menu;

import cn.sd.jrz.autoresource.entities.EnergyGeneratorEntity;
import cn.sd.jrz.autoresource.setup.ARRegistration;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * FE 发电机容器。
 * <p>
 * 包含加速槽（0）、充电槽（1）以及玩家背包。
 * 通过数据槽把能量、发电量、下次增长量、无线充电参数、六面开关等同步到客户端用于 GUI 展示，
 * 并在 GUI 中通过按钮（clickMenuButton）修改每台发电机的独立配置。
 * 26.x 适配：使用新传输 API（{@link net.neoforged.neoforge.items.ItemStackHandler}）。
 */
public class EnergyGeneratorMenu extends AbstractGeneratorMenu<EnergyGeneratorEntity> {
    // 按钮 ID
    public static final int BUTTON_WIRELESS = 0;
    public static final int BUTTON_INTERVAL_DOWN = 1;
    public static final int BUTTON_INTERVAL_UP = 2;
    public static final int BUTTON_RANGE_DOWN = 3;
    public static final int BUTTON_RANGE_UP = 4;
    public static final int BUTTON_REPEAT_DOWN = 5;
    public static final int BUTTON_REPEAT_UP = 6;
    public static final int BUTTON_TRANSFER_DOWN = 7;
    public static final int BUTTON_TRANSFER_UP = 8;
    public static final int BUTTON_TRANSFER_NORTH = 9;
    public static final int BUTTON_TRANSFER_SOUTH = 10;
    public static final int BUTTON_TRANSFER_WEST = 11;
    public static final int BUTTON_TRANSFER_EAST = 12;

    // 客户端展示数据（服务端通过数据槽同步而来）
    private long clientEnergy;
    private long clientOutput;
    private long clientNextIncrease;
    private int clientTickCount;
    private int clientSecond;
    private boolean clientWirelessOn;
    private int clientInterval;
    private int clientRange;
    private int clientRepeat;
    private final int[] clientNeighborBlockId = new int[6];

    public EnergyGeneratorMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(ARRegistration.ENERGY_GENERATOR_MENU.get(), id, playerInventory, pos);

        // 机器槽位：0=加速，1=充电
        if (entity != null) {
            addSlot(new SlotItemHandler(entity.starSlot, 0, 8, 190));
            addSlot(new SlotItemHandler(entity.chargeSlot, 0, 152, 190));
        }
        // 玩家背包：2-37
        addPlayerInventory(playerInventory, 230);

        // 数据同步（long 拆成高低 32 位两个数据槽）
        if (entity != null) {
            addDataSlot(makeDataSlot(() -> hiWord(entity.energy), v -> clientEnergy = mergeLong(v, loWord(clientEnergy))));
            addDataSlot(makeDataSlot(() -> loWord(entity.energy), v -> clientEnergy = mergeLong(hiWord(clientEnergy), v)));
            addDataSlot(makeDataSlot(() -> hiWord(entity.output), v -> clientOutput = mergeLong(v, loWord(clientOutput))));
            addDataSlot(makeDataSlot(() -> loWord(entity.output), v -> clientOutput = mergeLong(hiWord(clientOutput), v)));
            addDataSlot(makeDataSlot(() -> hiWord(entity.nextIncrease), v -> clientNextIncrease = mergeLong(v, loWord(clientNextIncrease))));
            addDataSlot(makeDataSlot(() -> loWord(entity.nextIncrease), v -> clientNextIncrease = mergeLong(hiWord(clientNextIncrease), v)));
            addDataSlot(makeDataSlot(() -> (int) Math.min(Integer.MAX_VALUE, entity.tickCount), v -> clientTickCount = v));
            addDataSlot(makeDataSlot(() -> (int) Math.min(Integer.MAX_VALUE, entity.config.getSecond()), v -> clientSecond = v));
            addDataSlot(makeDataSlot(() -> entity.wirelessOn ? 1 : 0, v -> clientWirelessOn = v != 0));
            addDataSlot(makeDataSlot(() -> entity.wirelessInterval, v -> clientInterval = v));
            addDataSlot(makeDataSlot(() -> Tool.normalizeWirelessRange(entity.wirelessRange), v -> clientRange = v));
            addDataSlot(makeDataSlot(() -> entity.transferRepeat, v -> clientRepeat = v));
        }
        // 六面传输开关数据槽（继承基类）
        addTransferFaceDataSlots();
        // 六方向相邻方块注册 id（服务端读实体，客户端读同步值，供 GUI 方向按钮显示图标）
        if (entity != null) {
            for (Direction direction : Direction.values()) {
                final int idx = direction.ordinal();
                addDataSlot(makeDataSlot(() -> entity.getNeighborBlockId(direction), v -> clientNeighborBlockId[idx] = v));
            }
        }
    }

    /**
     * 客户端/服务端都能访问的展示值（服务端读实体，客户端读同步值）
     */
    public long getEnergy() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide() ? entity.energy : clientEnergy;
    }

    @Override
    public long getOutput() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide() ? entity.output : clientOutput;
    }

    public long getNextIncrease() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide() ? entity.nextIncrease : clientNextIncrease;
    }

    @Override
    public long getMax() {
        return entity != null ? entity.config.getMax() : Long.MAX_VALUE;
    }

    @Override
    public int getTickCount() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide() ? (int) Math.min(Integer.MAX_VALUE, entity.tickCount) : clientTickCount;
    }

    @Override
    public int getSecond() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide() ? (int) Math.min(Integer.MAX_VALUE, entity.config.getSecond()) : clientSecond;
    }

    public boolean isWirelessOn() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide() ? entity.wirelessOn : clientWirelessOn;
    }

    public int getInterval() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide() ? entity.wirelessInterval : clientInterval;
    }

    public int getRange() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide() ? Tool.normalizeWirelessRange(entity.wirelessRange) : clientRange;
    }

    public int getRepeat() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide() ? entity.transferRepeat : clientRepeat;
    }

    /**
     * 处理 GUI 按钮点击（按钮 ID 由客户端发送）
     */
    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (entity == null || player.level().isClientSide()) {
            return false;
        }
        switch (id) {
            case BUTTON_WIRELESS -> entity.wirelessOn = !entity.wirelessOn;
            case BUTTON_INTERVAL_DOWN -> entity.wirelessInterval = Math.max(1, entity.wirelessInterval - 1);
            case BUTTON_INTERVAL_UP -> entity.wirelessInterval = Math.min(3600, entity.wirelessInterval + 1);
            case BUTTON_RANGE_DOWN -> entity.wirelessRange = stepRange(entity.wirelessRange, -1);
            case BUTTON_RANGE_UP -> entity.wirelessRange = stepRange(entity.wirelessRange, 1);
            case BUTTON_REPEAT_DOWN -> entity.transferRepeat = Math.max(1, entity.transferRepeat - 1);
            case BUTTON_REPEAT_UP -> entity.transferRepeat = Math.min(256, entity.transferRepeat + 1);
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

    /**
     * 区块范围步进：1 -> 3 -> 5 -> 1 循环
     */
    private static int stepRange(int current, int delta) {
        if (delta > 0) {
            return current >= 5 ? 1 : current + 2;
        }
        return current <= 1 ? 5 : current - 2;
    }

    /**
     * 快速转移物品
     */
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemStack = stack.copy();
            if (index < 2) {
                // 机器槽 -> 玩家背包
                if (!this.moveItemStackTo(stack, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包 -> 优先充电槽，其次加速槽，其余留在背包
                if (!this.moveItemStackTo(stack, 1, 2, false)) {
                    if (!this.moveItemStackTo(stack, 0, 1, false)) {
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

    /**
     * 加速槽所需物品（来自配置文件）
     */
    @Nullable
    public Item getStarItem() {
        return entity != null ? entity.config.getStarItem() : null;
    }

    /**
     * 指定方向相邻方块的物品栈（数量 1），无方块或方块无物品时返回空，供 GUI 方向按钮图标展示
     */
    @Nonnull
    public ItemStack getNeighborStack(Direction direction) {
        int id;
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide()) {
            id = entity.getNeighborBlockId(direction);
        } else {
            id = clientNeighborBlockId[direction.ordinal()];
        }
        if (id <= 0) {
            return ItemStack.EMPTY;
        }
        //noinspection deprecation
        Item item = BuiltInRegistries.BLOCK.byId(id).asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
