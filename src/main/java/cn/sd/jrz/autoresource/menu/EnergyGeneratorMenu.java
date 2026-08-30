package cn.sd.jrz.autoresource.menu;

import cn.sd.jrz.autoresource.blockentity.EnergyGeneratorEntity;
import cn.sd.jrz.autoresource.setup.Registration;
import cn.sd.jrz.autoresource.storage.MachineSlot;
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * FE 发电机容器：加速槽（0）、充电槽（1）与玩家背包；数据槽同步 GUI，按钮（clickMenuButton）修改逐台独立配置。
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
    private boolean clientTransferDown;
    private boolean clientTransferUp;
    private boolean clientTransferNorth;
    private boolean clientTransferSouth;
    private boolean clientTransferWest;
    private boolean clientTransferEast;
    private final int[] clientNeighborBlockId = new int[6];

    public EnergyGeneratorMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(Registration.ENERGY_GENERATOR_MENU, id, playerInventory, pos);

        // 机器槽位：0=加速，1=充电
        addSlot(new MachineSlot(entity.starSlot, 0, 8, 190));
        addSlot(new MachineSlot(entity.chargeSlot, 0, 152, 190));
        // 玩家背包：2-37
        addPlayerInventory(playerInventory, 230);

        // 数据同步：long 值拆成 4×16 位块（1.21.1 数据槽仅 16 位，见 AbstractGeneratorMenu.w0）
        addDataSlot(makeDataSlot(() -> w0(entity.energy), v -> clientEnergy = mergeLong4(v, w1(clientEnergy), w2(clientEnergy), w3(clientEnergy))));
        addDataSlot(makeDataSlot(() -> w1(entity.energy), v -> clientEnergy = mergeLong4(w0(clientEnergy), v, w2(clientEnergy), w3(clientEnergy))));
        addDataSlot(makeDataSlot(() -> w2(entity.energy), v -> clientEnergy = mergeLong4(w0(clientEnergy), w1(clientEnergy), v, w3(clientEnergy))));
        addDataSlot(makeDataSlot(() -> w3(entity.energy), v -> clientEnergy = mergeLong4(w0(clientEnergy), w1(clientEnergy), w2(clientEnergy), v)));
        addDataSlot(makeDataSlot(() -> w0(entity.output), v -> clientOutput = mergeLong4(v, w1(clientOutput), w2(clientOutput), w3(clientOutput))));
        addDataSlot(makeDataSlot(() -> w1(entity.output), v -> clientOutput = mergeLong4(w0(clientOutput), v, w2(clientOutput), w3(clientOutput))));
        addDataSlot(makeDataSlot(() -> w2(entity.output), v -> clientOutput = mergeLong4(w0(clientOutput), w1(clientOutput), v, w3(clientOutput))));
        addDataSlot(makeDataSlot(() -> w3(entity.output), v -> clientOutput = mergeLong4(w0(clientOutput), w1(clientOutput), w2(clientOutput), v)));
        addDataSlot(makeDataSlot(() -> w0(entity.nextIncrease), v -> clientNextIncrease = mergeLong4(v, w1(clientNextIncrease), w2(clientNextIncrease), w3(clientNextIncrease))));
        addDataSlot(makeDataSlot(() -> w1(entity.nextIncrease), v -> clientNextIncrease = mergeLong4(w0(clientNextIncrease), v, w2(clientNextIncrease), w3(clientNextIncrease))));
        addDataSlot(makeDataSlot(() -> w2(entity.nextIncrease), v -> clientNextIncrease = mergeLong4(w0(clientNextIncrease), w1(clientNextIncrease), v, w3(clientNextIncrease))));
        addDataSlot(makeDataSlot(() -> w3(entity.nextIncrease), v -> clientNextIncrease = mergeLong4(w0(clientNextIncrease), w1(clientNextIncrease), w2(clientNextIncrease), v)));
        addDataSlot(makeDataSlot(() -> (int) Math.min(Integer.MAX_VALUE, entity.tickCount), v -> clientTickCount = v));
        addDataSlot(makeDataSlot(() -> (int) Math.min(Integer.MAX_VALUE, entity.config.getSecond()), v -> clientSecond = v));
        addDataSlot(makeDataSlot(() -> entity.wirelessOn ? 1 : 0, v -> clientWirelessOn = v != 0));
        addDataSlot(makeDataSlot(() -> entity.wirelessInterval, v -> clientInterval = v));
        addDataSlot(makeDataSlot(() -> Tool.normalizeWirelessRange(entity.wirelessRange), v -> clientRange = v));
        addDataSlot(makeDataSlot(() -> entity.transferRepeat, v -> clientRepeat = v));
        addDataSlot(makeDataSlot(() -> entity.transferDown ? 1 : 0, v -> clientTransferDown = v != 0));
        addDataSlot(makeDataSlot(() -> entity.transferUp ? 1 : 0, v -> clientTransferUp = v != 0));
        addDataSlot(makeDataSlot(() -> entity.transferNorth ? 1 : 0, v -> clientTransferNorth = v != 0));
        addDataSlot(makeDataSlot(() -> entity.transferSouth ? 1 : 0, v -> clientTransferSouth = v != 0));
        addDataSlot(makeDataSlot(() -> entity.transferWest ? 1 : 0, v -> clientTransferWest = v != 0));
        addDataSlot(makeDataSlot(() -> entity.transferEast ? 1 : 0, v -> clientTransferEast = v != 0));
        // 六方向相邻方块注册 id（服务端读实体，客户端读同步值，供 GUI 方向按钮显示图标）
        for (Direction direction : Direction.values()) {
            final int idx = direction.ordinal();
            addDataSlot(makeDataSlot(() -> entity.getNeighborBlockId(direction), v -> clientNeighborBlockId[idx] = v));
        }
    }

    /**
     * 客户端/服务端都能访问的展示值（服务端读实体，客户端读同步值）
     */
    public long getEnergy() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.energy : clientEnergy;
    }

    @Override
    public long getOutput() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.output : clientOutput;
    }

    public long getNextIncrease() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.nextIncrease : clientNextIncrease;
    }

    @Override
    public long getMax() {
        return entity != null ? entity.config.getMax() : Long.MAX_VALUE;
    }

    @Override
    public int getTickCount() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? (int) Math.min(Integer.MAX_VALUE, entity.tickCount) : clientTickCount;
    }

    @Override
    public int getSecond() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? (int) Math.min(Integer.MAX_VALUE, entity.config.getSecond()) : clientSecond;
    }

    public boolean isWirelessOn() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.wirelessOn : clientWirelessOn;
    }

    public int getInterval() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.wirelessInterval : clientInterval;
    }

    public int getRange() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? Tool.normalizeWirelessRange(entity.wirelessRange) : clientRange;
    }

    public int getRepeat() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.transferRepeat : clientRepeat;
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
     * 指定方向相邻方块的物品栈（数量 1），无方块或方块无物品时返回空，供 GUI 方向按钮图标展示
     */
    @NotNull
    @Override
    public ItemStack getNeighborStack(Direction direction) {
        int id;
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide) {
            id = entity.getNeighborBlockId(direction);
        } else {
            id = clientNeighborBlockId[direction.ordinal()];
        }
        if (id <= 0) {
            return ItemStack.EMPTY;
        }
        //noinspection
        Item item = BuiltInRegistries.BLOCK.byId(id).asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /**
     * 处理 GUI 按钮点击（按钮 ID 由客户端发送）
     */
    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        //noinspection resource
        if (entity == null || player.level().isClientSide) {
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
     * <p>玩家背包 -> 充电槽/加速槽的合并不直接使用 vanilla {@link #moveItemStackTo}——
     * vanilla 合并分支以物品自身上限作为目标上限，不感知槽位自定义上限（充电/加速槽上限为 1），
     * 会绕过 {@link cn.sd.jrz.autoresource.storage.MachineSlotStorage#setItem} 的裁剪保护，
     * 表现即"放入后槽位显示 64，重开 GUI 后实际没存"。
     */
    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem();
        if (index < 2) {
            // 机器槽 -> 玩家背包：vanilla moveItemStackTo 在该方向（玩家背包不限制堆叠）安全
            ItemStack moving = original.copy();
            if (!this.moveItemStackTo(moving, 2, 38, true)) {
                return ItemStack.EMPTY;
            }
            int placed = original.getCount() - moving.getCount();
            if (placed <= 0) {
                return ItemStack.EMPTY;
            }
            if (moving.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            slot.onTake(player, moving);
            return moving;
        }
        // 玩家背包 -> 优先充电槽(1)，其次加速槽(0)，全部尊重槽位自定义上限
        ItemStack moving = original.copy();
        ItemStack leftover = insertRespectingSlotLimit(moving, 1);
        leftover = insertRespectingSlotLimit(leftover, 0);
        int placed = original.getCount() - leftover.getCount();
        if (placed <= 0) {
            return ItemStack.EMPTY;
        }
        if (leftover.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, leftover);
        return leftover;
    }

    /**
     * 尊重槽位自定义上限地把 stack 合并/放入 destSlotIndex，返回剩余（与 {@code BlockGeneratorMenu.insertIntoMarkerSlot} 同构）。
     */
    private ItemStack insertRespectingSlotLimit(ItemStack stack, int destSlotIndex) {
        if (stack.isEmpty()) {
            return stack;
        }
        Slot dest = this.slots.get(destSlotIndex);
        if (!dest.mayPlace(stack)) {
            return stack;
        }
        ItemStack existing = dest.getItem();
        int slotLimit = dest.getMaxStackSize(stack);
        if (existing.isEmpty()) {
            int toPlace = Math.min(slotLimit, stack.getCount());
            if (toPlace <= 0) {
                return stack;
            }
            ItemStack placed = stack.split(toPlace);
            dest.setByPlayer(placed);
            dest.setChanged();
            return stack;
        }
        if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < slotLimit) {
            int toPlace = Math.min(stack.getCount(), slotLimit - existing.getCount());
            if (toPlace <= 0) {
                return stack;
            }
            stack.shrink(toPlace);
            ItemStack merged = existing.copy();
            merged.grow(toPlace);
            dest.setByPlayer(merged);
            dest.setChanged();
            return stack;
        }
        return stack;
    }

    /**
     * 加速槽所需物品（来自配置文件）
     */
    @Nullable
    public Item getStarItem() {
        return entity != null ? entity.config.getStarItem() : null;
    }
}
