package cn.sd.jrz.autoresource.menu;

import cn.sd.jrz.autoresource.blockentity.BlockGeneratorEntity;
import cn.sd.jrz.autoresource.setup.Registration;
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
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

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
    // 六方向相邻方块注册 id（客户端读同步值，服务端读实体）
    private final int[] clientNeighborBlockId = new int[6];

    public BlockGeneratorMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(Registration.BLOCK_GENERATOR_MENU.get(), id, playerInventory, pos);

        // 机器槽位：0=标记槽（锁定），1=输出展示槽（单击提取）
        addSlot(new SlotItemHandler(entity.markerSlot, 0, 8, 113) {
            @Override
            public boolean mayPickup(@Nonnull Player player) {
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
            @Nonnull
            public ItemStack getItem() {
                ItemStack marked = entity != null ? entity.getMarkedItem() : ItemStack.EMPTY;
                return marked.isEmpty() ? ItemStack.EMPTY : marked.copy();
            }

            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(@Nonnull Player player) {
                return false;
            }

            @Override
            public void set(@Nonnull ItemStack stack) {
            }

            @Override
            @Nonnull
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
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.block : clientBlock;
    }

    @Override
    public long getOutput() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.output : clientOutput;
    }

    public long getStep() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.config.getStep() : clientStep;
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
     * 是否开启"下方生成方块"（客户端读同步值，服务端读实体）
     */
    public boolean isPlaceBlockBelow() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.placeBlockBelow : clientPlaceBlockBelow;
    }

    /**
     * 是否开启主动输出（客户端读同步值，服务端读实体）
     */
    public boolean isOutputEnabled() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.outputEnabled : clientOutputEnabled;
    }

    /**
     * 指定方向相邻方块的物品栈（数量 1），无方块或方块无物品时返回空，供 GUI 方向按钮图标展示
     */
    @Nonnull
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

    /**
     * 处理 GUI 按钮点击（六面开关、下方生成方块、输出槽提取）
     */
    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        //noinspection resource
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
     * 快速转移物品：标记槽不可取出，输出槽无实际物品；玩家背包只可移入标记槽
     */
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        if (index == 0 || index == 1) {
            // 标记槽锁定、输出展示槽不可操作
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ItemStack itemStack = stack.copy();
            // 玩家背包 -> 尝试移入标记槽
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
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
        return ItemStack.EMPTY;
    }
}
