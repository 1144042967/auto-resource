package cn.sd.jrz.autoresource.blockentity;

import cn.sd.jrz.autoresource.DataConfig;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 机器方块实体基类：持有三种机器共有的产量（output）、增长 tick（tickCount）、
 * 六面传输开关与轮询索引（findIndex），提供面的开关判断、六面开关 NBT 读写与 setChanged 节流。
 * <p>
 * 实现 {@link ExtendedScreenHandlerFactory}：打开 GUI 时向客户端附带机器坐标
 * （对应 Forge 版 NetworkHooks.openScreen 携带的附加数据），客户端据此定位实体而非玩家位置。
 * <p>
 * 1.21.1 适配：ExtendedScreenHandlerFactory 接口改为泛型 D + StreamCodec 编解码
 * （getScreenOpeningData 返回类型化数据）；saveAdditional 新增 HolderLookup.Provider 参数。
 */
public abstract class AbstractGeneratorEntity extends BlockEntity implements MenuProvider, ExtendedScreenHandlerFactory<BlockPos> {
    public final DataConfig config;

    // 核心数据（output 单位因机器而异：FE 能量 / mB 流体 / 方块×1000）
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
     * 六面开关写入 NBT（子类 {@code writeNbt} 调用）
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
     * 六面开关从 NBT 读取（子类 {@code readNbt} 调用）
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
     * 主动输出总开关写入 NBT（子类 {@code writeNbt} 调用）
     */
    protected void saveOutputEnabled(CompoundTag nbt) {
        nbt.putBoolean("outputEnabled", outputEnabled);
    }

    /**
     * 主动输出总开关从 NBT 读取（子类 {@code readNbt} 调用）
     */
    protected void loadOutputEnabled(CompoundTag nbt) {
        if (nbt.contains("outputEnabled", Tag.TAG_BYTE)) {
            outputEnabled = nbt.getBoolean("outputEnabled");
        }
    }

    /**
     * 打开扩展菜单时向客户端同步的附加数据：机器坐标（客户端工厂据此构造同名菜单）
     */
    @Override
    @NotNull
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return getBlockPos();
    }

    @Override
    @NotNull
    public abstract Component getDisplayName();

    @Nullable
    @Override
    public abstract AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player);

    /**
     * 指定方向的相邻方块注册 id（用于 GUI 展示实际相邻方块的物品图标）。无世界或方块无物品时返回 0。
     */
    public int getNeighborBlockId(Direction direction) {
        Level level = getLevel();
        if (level == null) {
            return 0;
        }
        //noinspection
        return BuiltInRegistries.BLOCK.getId(level.getBlockState(worldPosition.relative(direction)).getBlock());
    }
}
