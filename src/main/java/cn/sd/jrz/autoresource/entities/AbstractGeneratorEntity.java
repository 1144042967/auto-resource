package cn.sd.jrz.autoresource.entities;

import cn.sd.jrz.autoresource.DataConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 机器方块实体基类：持有三种机器共有的产量（output）、增长 tick（tickCount）、
 * 六面传输开关、主动输出总开关与轮询索引（findIndex），提供面的开关判断、
 * 六面开关/主动输出开关的存档读写（26.x 的 ValueOutput/ValueInput）与 setChanged 节流。
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
     * 六面开关写入存档（子类 {@code saveAdditional} 调用）
     */
    protected void saveTransferFaces(ValueOutput valueOutput) {
        valueOutput.putBoolean("transferDown", transferDown);
        valueOutput.putBoolean("transferUp", transferUp);
        valueOutput.putBoolean("transferNorth", transferNorth);
        valueOutput.putBoolean("transferSouth", transferSouth);
        valueOutput.putBoolean("transferWest", transferWest);
        valueOutput.putBoolean("transferEast", transferEast);
    }

    /**
     * 六面开关从存档读取（子类 {@code loadAdditional} 调用）
     */
    protected void loadTransferFaces(ValueInput valueInput) {
        this.transferDown = valueInput.getBooleanOr("transferDown", this.transferDown);
        this.transferUp = valueInput.getBooleanOr("transferUp", this.transferUp);
        this.transferNorth = valueInput.getBooleanOr("transferNorth", this.transferNorth);
        this.transferSouth = valueInput.getBooleanOr("transferSouth", this.transferSouth);
        this.transferWest = valueInput.getBooleanOr("transferWest", this.transferWest);
        this.transferEast = valueInput.getBooleanOr("transferEast", this.transferEast);
    }

    /**
     * 主动输出总开关写入存档（子类 {@code saveAdditional} 调用）
     */
    protected void saveOutputEnabled(ValueOutput valueOutput) {
        valueOutput.putBoolean("outputEnabled", outputEnabled);
    }

    /**
     * 主动输出总开关从存档读取（子类 {@code loadAdditional} 调用）
     */
    protected void loadOutputEnabled(ValueInput valueInput) {
        this.outputEnabled = valueInput.getBooleanOr("outputEnabled", this.outputEnabled);
    }

    @Override
    @Nonnull
    public abstract Component getDisplayName();

    /**
     * 指定方向的相邻方块注册 id（用于 GUI 展示实际相邻方块的物品图标）。无世界或方块无物品时返回 0。
     */
    public int getNeighborBlockId(Direction direction) {
        Level level = getLevel();
        if (level == null) {
            return 0;
        }
        //noinspection deprecation
        return BuiltInRegistries.BLOCK.getId(level.getBlockState(worldPosition.relative(direction)).getBlock());
    }

    /**
     * 指定方向相邻方块的物品栈（数量 1）。无方块或方块无对应物品时返回空。用于 GUI 方向按钮显示相邻方块图标。
     */
    @Nonnull
    public ItemStack getNeighborStack(Direction direction) {
        //noinspection deprecation
        Item item = BuiltInRegistries.BLOCK.byId(getNeighborBlockId(direction)).asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Nullable
    @Override
    public abstract AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player);
}
