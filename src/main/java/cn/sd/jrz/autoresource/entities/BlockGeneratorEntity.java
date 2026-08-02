package cn.sd.jrz.autoresource.entities;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.connection.BlockConnection;
import cn.sd.jrz.autoresource.menu.BlockGeneratorMenu;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 方块生成器实体。
 * <p>
 * 负责：产量自动增长、标记槽（放入一个合法方块生成机产品后锁定，决定输出方块的种类）、
 * 六面方块传输（可逐面禁用）以及"下方生成方块"。
 * 自动生成会一直计算，但未标记时无法取出/传输/放置；标记后不可更换。
 */
public class BlockGeneratorEntity extends BlockEntity implements ICapabilityProvider, MenuProvider {
    private final LazyOptional<BlockConnection> blockOptional = LazyOptional.of(() -> new BlockConnection(this));
    public final DataConfig config;

    // 核心数据（方块数量单位为 Block/1000，即块）
    public long output;
    public long block = 0;
    public long tickCount = 0;

    // 六面方块传输开关（逐台保存，可在 GUI 修改，默认全启用）
    public boolean transferDown = true;
    public boolean transferUp = true;
    public boolean transferNorth = true;
    public boolean transferSouth = true;
    public boolean transferWest = true;
    public boolean transferEast = true;

    // 是否在下方空气方块放置对应方块（由 GUI 按钮控制，替代原红石激活判断，默认关闭）
    public boolean placeBlockBelow = false;

    // 标记槽：放入一个合法物品后锁定，决定机器输出的方块种类
    public final ItemStackHandler markerSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return DataConfig.isBlockGeneratorItem(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // 标记槽变化时强制同步到客户端，并触发重新渲染（否则客户端看不到标记的物品）
            Level level = getLevel();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            }
        }
    };

    // 六面传输轮询索引
    private int findIndex = 0;

    public BlockGeneratorEntity(BlockPos pos, BlockState state, DataConfig config) {
        super(config.getEntityType(), pos, state);
        this.config = config;
        this.output = config.getMin();
    }

    /**
     * 服务端每 tick 调用（由方块的 ticker 触发）
     */
    public void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        // 增长逻辑：产量到间隔后增加（自动生成一直计算，与是否标记无关）
        tickCount = Tool.suit(tickCount + 1);
        if (tickCount / 20 >= config.getSecond()) {
            tickCount = 0;
            output = Math.min(config.getMax(), Tool.suit(output + config.getStep()));
        }
        block = Tool.suit(block + output);

        ItemStack marked = getMarkedItem();
        if (!marked.isEmpty()) {
            // 标记后才向六面传输方块
            outputToSides(marked);
            // 开启"下方生成方块"时，向下方空气方块放置方块
            placeBlockBelow(marked);
        }
        setChanged();
    }

    /**
     * 标记槽中的物品（未标记时返回空）
     */
    public ItemStack getMarkedItem() {
        return markerSlot.getStackInSlot(0);
    }

    /**
     * 六面方块传输（跳过被禁用的面），轮询索引实现负载均衡；仅输出标记的方块
     */
    private void outputToSides(ItemStack marked) {
        Level level = getLevel();
        if (level == null || block / 1000 <= 0) {
            return;
        }
        Direction[] directions = Direction.values();
        BlockPos blockPos = getBlockPos();
        for (int i = 0; i < directions.length; i++) {
            if (block / 1000 <= 0) {
                return;
            }
            findIndex = (findIndex + 1) % directions.length;
            Direction direction = directions[findIndex];
            if (!isTransferEnabled(direction)) {
                continue;
            }
            BlockPos pos = blockPos.relative(direction);
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                continue;
            }
            IItemHandler handler = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).resolve().orElse(null);
            if (handler == null) {
                continue;
            }
            int maxOutput = Tool.suitInt(block / 1000);
            ItemStack result = ItemHandlerHelper.insertItemStacked(handler, new ItemStack(marked.getItem(), maxOutput), false);
            int count = result.getCount();
            if (count < 0) {
                count = 0;
            }
            if (count > maxOutput) {
                count = maxOutput;
            }
            block -= (maxOutput - count) * 1000L;
        }
    }

    /**
     * 开启"下方生成方块"时，每 5 ticks 尝试向下方空气方块放置标记的方块，每次消耗 1000 单位
     */
    private void placeBlockBelow(ItemStack marked) {
        Level level = getLevel();
        if (level == null || !placeBlockBelow) {
            return;
        }
        if (block >= 1000 && tickCount % 5 == 0) {
            BlockPos pos = getBlockPos().relative(Direction.DOWN);
            BlockState placed = getMarkedBlockState(marked);
            if (placed != null && level.getBlockState(pos).getBlock() == Blocks.AIR && level.setBlock(pos, placed, 3)) {
                block -= 1000;
            }
        }
    }

    /**
     * 标记物品对应的方块状态（标记物品必须是 BlockItem）
     */
    private BlockState getMarkedBlockState(ItemStack marked) {
        if (marked.getItem() instanceof BlockItem blockItem) {
            return blockItem.getBlock().defaultBlockState();
        }
        return null;
    }

    /**
     * 从存量中提取最多 count 个标记方块，返回实际提取数量（未标记时返回 0）
     */
    public long extractBlocks(long count) {
        ItemStack marked = getMarkedItem();
        if (marked.isEmpty() || count <= 0) {
            return 0;
        }
        long available = block / 1000;
        long toExtract = Math.min(available, count);
        if (toExtract <= 0) {
            return 0;
        }
        block -= toExtract * 1000L;
        return toExtract;
    }

    /**
     * 指定面是否允许方块传输
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

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        return capability == ForgeCapabilities.ITEM_HANDLER ? blockOptional.cast() : super.getCapability(capability, direction);
    }

    @Override
    @Nonnull
    public Component getDisplayName() {
        BlockState state = getLevel() != null ? getLevel().getBlockState(worldPosition) : null;
        if (state != null && !state.isAir()) {
            return Component.translatable(state.getBlock().getDescriptionId());
        }
        return Component.translatable("block.autoresource.block_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new BlockGeneratorMenu(id, inv, worldPosition);
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putLong("output", output);
        nbt.putLong("block", block);
        nbt.putLong("tickCount", tickCount);
        nbt.putBoolean("transferDown", transferDown);
        nbt.putBoolean("transferUp", transferUp);
        nbt.putBoolean("transferNorth", transferNorth);
        nbt.putBoolean("transferSouth", transferSouth);
        nbt.putBoolean("transferWest", transferWest);
        nbt.putBoolean("transferEast", transferEast);
        nbt.putBoolean("placeBlockBelow", placeBlockBelow);
        nbt.put("markerSlot", markerSlot.serializeNBT());
    }

    /**
     * 初始同步到客户端的数据（包含标记槽），保证进游戏后方块机上即可显示标记物品
     */
    @Override
    @Nonnull
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    /**
     * 数据变化时发送给客户端的更新包（标记槽变化后强制刷新渲染）
     */
    @Override
    @Nonnull
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("output", Tag.TAG_LONG)) {
            output = Tool.suit(nbt.getLong("output"));
        }
        if (nbt.contains("block", Tag.TAG_LONG)) {
            block = Tool.suit(nbt.getLong("block"));
        }
        if (nbt.contains("tickCount", Tag.TAG_LONG)) {
            tickCount = Tool.suit(nbt.getLong("tickCount"));
        }
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
        if (nbt.contains("placeBlockBelow", Tag.TAG_BYTE)) {
            placeBlockBelow = nbt.getBoolean("placeBlockBelow");
        }
        if (nbt.contains("markerSlot", Tag.TAG_COMPOUND)) {
            markerSlot.deserializeNBT(nbt.getCompound("markerSlot"));
        }
    }
}
