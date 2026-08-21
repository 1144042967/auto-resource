package cn.sd.jrz.autoresource.blockentity;

import cn.sd.jrz.autoresource.DataConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 机器方块实体基类：持有三种机器共有的产量（output）、增长 tick（tickCount）、
 * 六面传输开关与轮询索引（findIndex），提供面的开关判断、六面开关 NBT 读写与 setChanged 节流。
 */
public abstract class AbstractGeneratorEntity extends BlockEntity implements MenuProvider {
    public final DataConfig config;

    // 核心数据（output 单位因机器而异：FE 能量 / mB/1000 流体 / Block/1000 方块）
    public long output;
    public long tickCount = 0;

    // 六面传输开关（逐台保存，可在 GUI 修改，默认全启用）
    public boolean transferDown = true;
    public boolean transferUp = true;
    public boolean transferNorth = true;
    public boolean transferSouth = true;
    public boolean transferWest = true;
    public boolean transferEast = true;

    // 主动输出总开关（GUI 右上角按钮控制，默认开启；关闭后不再向相邻方块/管道六面传输）
    public boolean outputEnabled = true;

    // 六面传输轮询索引
    protected int findIndex = 0;

    // setChanged 节流计数（约每 20 tick 标记一次，避免机器每 tick 使 chunk 保持"未保存"状态）
    private int dirtyTicks = 0;

    protected AbstractGeneratorEntity(BlockPos pos, BlockState state, DataConfig config) {
        super(config.getEntityType(), pos, state);
        this.config = config;
        this.output = config.getMin();
    }

    /**
     * 指定面是否允许传输
     */
    public boolean isTransferEnabled(Direction direction) {
        return switch (direction) {
            case DOWN -> transferDown;
            case UP -> transferUp;
            case NORTH -> transferNorth;
            case SOUTH -> transferSouth;
            case WEST -> transferWest;
            case EAST -> transferEast;
        };
    }

    /**
     * 每 tick 存档标记，节流到约每 20 tick 调用一次 setChanged()，避免大量机器每 tick 标记 chunk 未保存
     */
    protected void markDirtyTick() {
        if (++dirtyTicks >= 20) {
            dirtyTicks = 0;
            setChanged();
        }
    }

    /**
     * 六面开关写入 NBT（子类 {@code saveAdditional} 调用）
     */
    protected void saveTransferFaces(CompoundTag nbt) {
        nbt.putBoolean("transferDown", transferDown);
        nbt.putBoolean("transferUp", transferUp);
        nbt.putBoolean("transferNorth", transferNorth);
        nbt.putBoolean("transferSouth", transferSouth);
        nbt.putBoolean("transferWest", transferWest);
        nbt.putBoolean("transferEast", transferEast);
    }

    /**
     * 六面开关从 NBT 读取（子类 {@code load} 调用）
     */
    protected void loadTransferFaces(CompoundTag nbt) {
        if (nbt.contains("transferDown", Tag.TAG_BYTE)) {
            transferDown = nbt.getBoolean("transferDown");
        }
        if (nbt.contains("transferUp", Tag.TAG_BYTE)) {
            transferUp = nbt.getBoolean("transferUp");
        }
        if (nbt.contains("transferNorth", Tag.TAG_BYTE)) {
            transferNorth = nbt.getBoolean("transferNorth");
        }
        if (nbt.contains("transferSouth", Tag.TAG_BYTE)) {
            transferSouth = nbt.getBoolean("transferSouth");
        }
        if (nbt.contains("transferWest", Tag.TAG_BYTE)) {
            transferWest = nbt.getBoolean("transferWest");
        }
        if (nbt.contains("transferEast", Tag.TAG_BYTE)) {
            transferEast = nbt.getBoolean("transferEast");
        }
    }

    /**
     * 主动输出总开关写入 NBT（子类 {@code saveAdditional} 调用）
     */
    protected void saveOutputEnabled(CompoundTag nbt) {
        nbt.putBoolean("outputEnabled", outputEnabled);
    }

    /**
     * 主动输出总开关从 NBT 读取（子类 {@code load} 调用）
     */
    protected void loadOutputEnabled(CompoundTag nbt) {
        if (nbt.contains("outputEnabled", Tag.TAG_BYTE)) {
            outputEnabled = nbt.getBoolean("outputEnabled");
        }
    }

    @Override
    @Nonnull
    public abstract Component getDisplayName();

    @Nullable
    @Override
    public abstract AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player);
}
