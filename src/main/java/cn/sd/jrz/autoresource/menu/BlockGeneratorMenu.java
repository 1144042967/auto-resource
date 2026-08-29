package cn.sd.jrz.autoresource.menu;

import cn.sd.jrz.autoresource.blockentity.BlockGeneratorEntity;
import cn.sd.jrz.autoresource.setup.Registration;
import cn.sd.jrz.autoresource.storage.MachineSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.NotNull;

/**
 * 方块生成器容器：标记槽（0，锁定决定输出种类）、输出展示槽（1，单击提取）与玩家背包；数据槽同步 GUI，按钮修改开关/提取。
 */
public class BlockGeneratorMenu extends AbstractGeneratorMenu<BlockGeneratorEntity> {
    // 按钮 ID
    public static final int BUTTON_TRANSFER_DOWN = 0;
    public static final int BUTTON_TRANSFER_UP = 1;
    public static final int BUTTON_TRANSFER_NORTH = 2;
    public static final int BUTTON_TRANSFER_SOUTH = 3;
    public static final int BUTTON_TRANSFER_WEST = 4;
    public static final int BUTTON_TRANSFER_EAST = 5;
    public static final int BUTTON_PLACE_BLOCK = 6;
    public static final int BUTTON_EXTRACT_ONE = 7;
    public static final int BUTTON_EXTRACT_STACK = 8;
    public static final int BUTTON_EXTRACT_ALL = 9;
    public static final int BUTTON_OUTPUT = 10;

    // 客户端展示数据（服务端通过数据槽同步而来）
    private long clientBlock;
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
    private boolean clientPlaceBlockBelow;
    private boolean clientOutputEnabled;
    private final int[] clientNeighborBlockId = new int[6];

    public BlockGeneratorMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(Registration.BLOCK_GENERATOR_MENU, id, playerInventory, pos);

        // 机器槽位：0=标记槽（锁定），1=输出展示槽（单击提取）
        addSlot(new MachineSlot(entity.markerSlot, 0, 8, 113) {
            @Override
            public boolean mayPickup(@NotNull Player player) {
                // 一旦放入物品不允许取出/更换
                return false;
            }
        });
        addSlot(new Slot(new SimpleContainer(1), 0, 152, 113) {
            @Override
            public boolean hasItem() {
                return !getItem().isEmpty();
            }

            @Override
            @NotNull
            public ItemStack getItem() {
                ItemStack marked = entity != null ? entity.getMarkedItem() : ItemStack.EMPTY;
                return marked.isEmpty() ? ItemStack.EMPTY : marked.copy();
            }

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(@NotNull Player player) {
                return false;
            }

            @Override
            public void set(@NotNull ItemStack stack) {
            }

            @Override
            @NotNull
            public ItemStack remove(int amount) {
                return ItemStack.EMPTY;
            }
        });
        // 玩家背包：2-37
        addPlayerInventory(playerInventory, 153);

        // 数据同步（long 拆成高低 32 位两个数据槽）
        addDataSlot(makeDataSlot(() -> hiWord(entity.block), v -> clientBlock = mergeLong(v, loWord(clientBlock))));
        addDataSlot(makeDataSlot(() -> loWord(entity.block), v -> clientBlock = mergeLong(hiWord(clientBlock), v)));
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
        addDataSlot(makeDataSlot(() -> entity.placeBlockBelow ? 1 : 0, v -> clientPlaceBlockBelow = v != 0));
        addDataSlot(makeDataSlot(() -> entity.outputEnabled ? 1 : 0, v -> clientOutputEnabled = v != 0));
        // 六方向相邻方块注册 id（服务端读实体，客户端读同步值，供 GUI 方向按钮显示图标）
        for (Direction direction : Direction.values()) {
            final int idx = direction.ordinal();
            addDataSlot(makeDataSlot(() -> entity.getNeighborBlockId(direction), v -> clientNeighborBlockId[idx] = v));
        }
    }

    /**
     * 客户端/服务端都能访问的展示值（服务端读实体，客户端读同步值）
     */
    public long getBlock() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide() ? entity.block : clientBlock;
    }

    @Override
    public long getOutput() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide() ? entity.output : clientOutput;
    }

    public long getStep() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide() ? entity.config.getStep() : clientStep;
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
     * 是否开启"下方生成方块"（客户端读同步值，服务端读实体）
     */
    public boolean isPlaceBlockBelow() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide() ? entity.placeBlockBelow : clientPlaceBlockBelow;
    }

    /**
     * 是否开启主动输出（客户端读同步值，服务端读实体）
     */
    public boolean isOutputEnabled() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide() ? entity.outputEnabled : clientOutputEnabled;
    }

    /**
     * 处理 GUI 按钮点击（六面开关、下方生成方块、输出槽提取）
     */
    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        //noinspection resource
        if (entity == null || player.level().isClientSide()) {
            return false;
        }
        switch (id) {
            case BUTTON_TRANSFER_DOWN -> entity.transferDown = !entity.transferDown;
            case BUTTON_TRANSFER_UP -> entity.transferUp = !entity.transferUp;
            case BUTTON_TRANSFER_NORTH -> entity.transferNorth = !entity.transferNorth;
            case BUTTON_TRANSFER_SOUTH -> entity.transferSouth = !entity.transferSouth;
            case BUTTON_TRANSFER_WEST -> entity.transferWest = !entity.transferWest;
            case BUTTON_TRANSFER_EAST -> entity.transferEast = !entity.transferEast;
            case BUTTON_PLACE_BLOCK -> entity.placeBlockBelow = !entity.placeBlockBelow;
            case BUTTON_OUTPUT -> entity.outputEnabled = !entity.outputEnabled;
            case BUTTON_EXTRACT_ONE -> extractBlocks(player, 1);
            case BUTTON_EXTRACT_STACK -> extractBlocks(player, entity.getMarkedItem().getMaxStackSize());
            case BUTTON_EXTRACT_ALL -> extractBlocks(player, Long.MAX_VALUE);
            default -> {
                return false;
            }
        }
        entity.setChanged();
        return true;
    }

    /**
     * 从存量中提取最多 maxCount 个标记方块放入玩家背包；放不下时退回存量
     */
    private void extractBlocks(Player player, long maxCount) {
        if (entity.getMarkedItem().isEmpty() || maxCount <= 0) {
            return;
        }
        long remaining = maxCount;
        while (remaining > 0) {
            long available = entity.block / 1000;
            if (available <= 0) {
                break;
            }
            int amount = (int) Math.min(available, Math.min(remaining, 64));
            long got = entity.extractBlocks(amount);
            if (got <= 0) {
                break;
            }
            ItemStack stack = new ItemStack(entity.getMarkedItem().getItem(), (int) got);
            // addItem 会把无法放入背包的剩余部分留在 stack 中
            player.addItem(stack);
            int placed = (int) got - stack.getCount();
            if (placed < got) {
                // 背包放不下的部分退回存量
                entity.block += (got - placed) * 1000L;
            }
            remaining -= placed;
            if (placed <= 0) {
                break; // 背包已满
            }
        }
    }

    /**
     * 快速转移物品：标记槽不可取出，输出槽无实际物品；玩家背包只可移入标记槽。
     * <p>注意：必须自己实现合并/放置逻辑，不直接使用 vanilla {@link #moveItemStackTo}——
     * vanilla 的合并分支使用 {@code ItemStack.getMaxStackSize()}（物品自身上限）作为目标上限，
     * 不感知 {@link net.minecraft.world.inventory.Slot#getMaxStackSize(ItemStack)} 的槽位自定义上限，
     * 会绕过 {@link cn.sd.jrz.autoresource.storage.MachineSlotStorage#setItem} 的裁剪保护，
     * 表现即"放入后两槽位显示 64，重开 GUI 后实际没存"。
     */
    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index == 0 || index == 1) {
            // 标记槽锁定、输出展示槽不可操作
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem();
        ItemStack moving = original.copy();
        ItemStack leftover = insertIntoMarkerSlot(moving);
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
     * 尊重标记槽自定义上限（{@link Slot#getMaxStackSize(ItemStack)}）地把 stack 合并/放入标记槽，返回剩余。
     * 该方法与 vanilla {@link #moveItemStackTo} 等价但避开了"以物品自身上限为目标槽上限"的合并 bug。
     */
    private ItemStack insertIntoMarkerSlot(ItemStack stack) {
        Slot dest = this.slots.get(0);
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
            // 通过 setByPlayer → set → setItem 路径写回，由 MachineSlotStorage.setItem 的裁剪做最终防御
            ItemStack merged = existing.copy();
            merged.grow(toPlace);
            dest.setByPlayer(merged);
            dest.setChanged();
            return stack;
        }
        return stack;
    }

    /**
     * 指定方向相邻方块的物品栈（数量 1），无方块或方块无物品时返回空，供 GUI 方向按钮图标展示
     */
    @NotNull
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
        //noinspection deprecation
        Item item = BuiltInRegistries.BLOCK.byId(id).asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
