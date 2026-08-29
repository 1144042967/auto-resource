package cn.sd.jrz.autoresource.compat.create;

import cn.sd.jrz.autoresource.setup.Registration;
import cn.sd.jrz.autoresource.storage.MachineSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.NotNull;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 水车马达容器：单个水车槽 + 玩家背包，数据槽同步转速/方向/面/容量到客户端，
 * 按钮切换旋转方向与六面输出方向（转速由水车数量决定，不可手动调节）。
 */
public class WaterWheelMotorMenu extends AbstractContainerMenu {
    // 按钮 ID
    public static final int BUTTON_DIRECTION = 0;
    public static final int BUTTON_FACE_DOWN = 1;
    public static final int BUTTON_FACE_UP = 2;
    public static final int BUTTON_FACE_NORTH = 3;
    public static final int BUTTON_FACE_SOUTH = 4;
    public static final int BUTTON_FACE_WEST = 5;
    public static final int BUTTON_FACE_EAST = 6;

    public final WaterWheelMotorEntity entity;

    // 客户端展示数据（服务端通过数据槽同步而来）
    private int clientSpeed;
    private int clientDirection; // 0=顺时针, 1=逆时针
    private int clientFace;
    private int clientCapacity;
    private final int[] clientNeighborBlockId = new int[6];

    public WaterWheelMotorMenu(int id, Inventory playerInventory, BlockPos pos) {
        super((MenuType<WaterWheelMotorMenu>) (MenuType<?>) Registration.WATER_WHEEL_MOTOR_MENU, id);
        this.entity = (WaterWheelMotorEntity) playerInventory.player.level().getBlockEntity(pos);

        // 水车槽位（单个槽；位置与背景纹理 water_wheel_motor_gui.png 中的槽位框一致）
        addSlot(new MachineSlot(entity.wheelSlots, 0, 8, 57) {
            @Override
            public void setChanged() {
                super.setChanged();
                // Shift+点击合并（moveItemStackTo）只调 setChanged、不触发 onContentsChanged，这里补发
                if (entity.getLevel() != null && !entity.getLevel().isClientSide) {
                    entity.handleWheelContentsChanged();
                }
            }
        });
        // 玩家背包：1-36（纹理中 4 行槽框在 y=96/114/132/150）
        addPlayerInventory(playerInventory, 97);

        // 数据同步
        addDataSlot(makeDataSlot(() -> entity.currentSpeed(), v -> clientSpeed = v));
        addDataSlot(makeDataSlot(() -> entity.counterClockwise ? 1 : 0, v -> clientDirection = v));
        addDataSlot(makeDataSlot(() -> entity.getOutputFace().ordinal(), v -> clientFace = v));
        addDataSlot(makeDataSlot(() -> (int) entity.totalCapacity(), v -> clientCapacity = v));
        // 六方向相邻方块注册 id（服务端读实体，客户端读同步值，供 GUI 方向按钮显示图标）
        for (Direction direction : Direction.values()) {
            final int idx = direction.ordinal();
            addDataSlot(makeDataSlot(() -> entity.getNeighborBlockId(direction), v -> clientNeighborBlockId[idx] = v));
        }
    }

    /**
     * 客户端/服务端都能访问的展示值（服务端读实体，客户端读同步值）
     */
    public int getSpeed() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.currentSpeed() : clientSpeed;
    }

    public boolean isCounterClockwise() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.counterClockwise : clientDirection != 0;
    }

    public Direction getFace() {
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide) {
            return entity.getOutputFace();
        }
        return Direction.values()[Math.floorMod(clientFace, 6)];
    }

    public int getCapacity() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? (int) entity.totalCapacity() : clientCapacity;
    }

    /**
     * 处理 GUI 按钮点击
     */
    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (entity == null || player.level().isClientSide) {
            return false;
        }
        switch (id) {
            case BUTTON_DIRECTION -> entity.toggleDirection();
            case BUTTON_FACE_DOWN -> entity.setOutputFace(Direction.DOWN);
            case BUTTON_FACE_UP -> entity.setOutputFace(Direction.UP);
            case BUTTON_FACE_NORTH -> entity.setOutputFace(Direction.NORTH);
            case BUTTON_FACE_SOUTH -> entity.setOutputFace(Direction.SOUTH);
            case BUTTON_FACE_WEST -> entity.setOutputFace(Direction.WEST);
            case BUTTON_FACE_EAST -> entity.setOutputFace(Direction.EAST);
            default -> {
                return false;
            }
        }
        entity.setChanged();
        return true;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (entity == null) {
            return false;
        }
        return entity.getLevel() != null && entity.getLevel().getBlockEntity(entity.getBlockPos()) == entity;
    }

    /**
     * 快速转移物品：水车槽位与玩家背包互移（水车槽每格限 1 个）
     */
    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemStack = stack.copy();
            if (index < WaterWheelMotorEntity.SLOT_COUNT) {
                // 水车槽 -> 玩家背包
                if (!this.moveItemStackTo(stack, WaterWheelMotorEntity.SLOT_COUNT, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包 -> 水车槽
                if (!this.moveItemStackTo(stack, 0, WaterWheelMotorEntity.SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
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

    private void addPlayerInventory(Inventory playerInventory, int baseY) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, baseY + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, baseY + 54));
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
