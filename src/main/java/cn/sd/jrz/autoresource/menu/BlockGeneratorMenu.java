package cn.sd.jrz.autoresource.menu;

import cn.sd.jrz.autoresource.entities.BlockGeneratorEntity;
import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 方块生成器容器。
 * <p>
 * 包含标记槽（0，放入合法物品后锁定，决定输出方块种类）、输出展示槽（1，显示标记物品，单击提取）以及玩家背包。
 * 通过数据槽把存量、产量、下次增长量、增长进度、六面开关、"下方生成方块"等同步到客户端用于 GUI 展示，
 * 并通过按钮（clickMenuButton）修改六面开关、"下方生成方块"以及执行输出槽的提取操作。
 */
public class BlockGeneratorMenu extends AbstractContainerMenu {
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

    public final BlockGeneratorEntity entity;

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

    public BlockGeneratorMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(Registration.BLOCK_GENERATOR_MENU.get(), id);
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        this.entity = (BlockGeneratorEntity) blockEntity;

        // 机器槽位：0=标记槽（放入后锁定），1=输出展示槽（显示标记物品，单击提取）
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
        addPlayerInventory(playerInventory);

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
    }

    /** 客户端/服务端都能访问的展示值（服务端读实体，客户端读同步值） */
    public long getBlock() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.block : clientBlock;
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

    /** 是否开启"下方生成方块"（客户端读同步值，服务端读实体） */
    public boolean isPlaceBlockBelow() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.placeBlockBelow : clientPlaceBlockBelow;
    }

    /** 标记的物品（未标记返回空） */
    public ItemStack getMarkedItem() {
        return entity != null ? entity.getMarkedItem() : ItemStack.EMPTY;
    }

    /** 处理 GUI 按钮点击（六面开关、下方生成方块、输出槽提取） */
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
            case BUTTON_PLACE_BLOCK -> entity.placeBlockBelow = !entity.placeBlockBelow;
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
     * 从存量中提取最多 maxCount 个标记方块放入玩家背包；背包放不下时退回存量
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

    @Override
    public boolean stillValid(@Nonnull Player player) {
        if (entity == null) {
            return false;
        }
        return entity.getLevel() != null && entity.getLevel().getBlockEntity(entity.getBlockPos()) == entity;
    }

    /** 快速转移物品：标记槽不可取出，输出槽无实际物品；玩家背包只可移入标记槽 */
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        if (index == 0 || index == 1) {
            // 标记槽锁定、输出展示槽不可操作
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ItemStack itemStack = stack.copy();
            // 玩家背包 -> 尝试移入标记槽（仅合法物品且槽为空时成功）
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
