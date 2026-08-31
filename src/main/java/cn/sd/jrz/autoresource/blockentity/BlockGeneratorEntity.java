package cn.sd.jrz.autoresource.blockentity;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.capability.BlockConnection;
import cn.sd.jrz.autoresource.menu.BlockGeneratorMenu;
import cn.sd.jrz.autoresource.storage.MachineSlotStorage;
import cn.sd.jrz.autoresource.util.Tool;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 方块生成器实体：产量自动增长、标记槽（放入合法产品后锁定，决定输出方块种类）、
 * 六面方块传输（可逐面禁用）、"下方生成方块"。未标记时无法取出/传输/放置。
 */
public class BlockGeneratorEntity extends AbstractGeneratorEntity {

    // 核心数据（内部单位为 方块×1000）
    public long block = 0;

    // 是否在下方空气方块放置对应方块（由 GUI 按钮控制，默认关闭）
    public boolean placeBlockBelow = false;

    // 标记槽：放入一个合法物品后锁定，决定机器输出的方块种类
    public final MachineSlotStorage markerSlot = new MachineSlotStorage(1) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
            // 标记槽变化时同步到客户端并触发重新渲染（否则客户端看不到标记物品）
            Level level = getLevel();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
                // sendBlockUpdated 只发方块状态；标记物品数据还需额外发送 BE 更新包，
                // 客户端（GUI 标记/输出槽、四周贴图）才能即时读到 markerSlot
                if (level instanceof ServerLevel serverLevel) {
                    // 26.1.2：ChunkPos 构造器只接收 (int, int)，由 BlockPos 计算坐标
                    ChunkPos chunkPos = new ChunkPos(getBlockPos().getX() >> 4, getBlockPos().getZ() >> 4);
                    ClientboundBlockEntityDataPacket packet = getUpdatePacket();
                    serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false).forEach(p -> p.connection.send(packet));
                }
            }
        }
    }.setValidator(DataConfig::isBlockGeneratorItem).setSlotLimit(0, 1);

    // 对外连接实例（六面相同；由 TransferSetup 暴露到 ItemStorage.SIDED）
    private final BlockConnection blockConnection = new BlockConnection(this);

    public BlockGeneratorEntity(BlockPos pos, BlockState state, DataConfig config) {
        super(pos, state, config);
    }

    /**
     * 服务端每 tick 调用（由方块的 ticker 触发）
     */
    public void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        // 增长逻辑：产量到间隔后增加（与是否标记无关）
        tickCount = Tool.suit(tickCount + 1);
        if (tickCount / 20 >= config.getSecond()) {
            tickCount = 0;
            output = Math.min(config.getMax(), Tool.suit(output + config.getStep()));
        }
        block = Tool.suit(block + output);

        ItemStack marked = getMarkedItem();
        if (!marked.isEmpty()) {
            // 标记后才向六面传输方块（主动输出总开关关闭时不传输）
            if (outputEnabled) {
                outputToSides(marked);
            }
            // 开启"下方生成方块"时，向下方空气方块放置方块
            placeBlockBelowTick(marked);
        }
        markDirtyTick();
    }

    /**
     * 标记槽中的物品（未标记时返回空）
     */
    public ItemStack getMarkedItem() {
        return markerSlot.getItem(0);
    }

    /**
     * 六面方块传输（跳过被禁用的面），轮询索引负载均衡；仅输出标记的方块。
     * insert 对多槽目标按槽位逐个尝试合并/填充，语义接近 Forge 的 insertItemStacked。
     */
    private void outputToSides(ItemStack marked) {
        Level level = getLevel();
        if (level == null || block / 1000 <= 0) {
            return;
        }
        Direction[] directions = Direction.values();
        BlockPos blockPos = getBlockPos();
        ItemVariant variant = ItemVariant.of(marked.getItem());
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
            var target = ItemStorage.SIDED.find(level, pos, direction.getOpposite());
            if (target == null || !target.supportsInsertion()) {
                continue;
            }
            long maxOutput = Tool.suitInt(block / 1000);
            try (var txn = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                long inserted = target.insert(variant, maxOutput, txn);
                if (inserted <= 0) {
                    continue;
                }
                txn.commit();
                block -= inserted * 1000L;
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * "下方生成方块"开启时每 5 ticks 向下方空气放置标记方块，每次消耗 1000 单位
     */
    private void placeBlockBelowTick(ItemStack marked) {
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

    @Override
    @NotNull
    public Component getDisplayName() {
        BlockState state = getLevel() != null ? getLevel().getBlockState(getBlockPos()) : null;
        if (state != null && !state.isAir()) {
            return Component.translatable(state.getBlock().getDescriptionId());
        }
        return Component.translatable("block.autoresource.block_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
        return new BlockGeneratorMenu(id, inv, getBlockPos());
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput out) {
        // 26.1.2：流式 ValueOutput 持久化（与 1.21.11 对齐）
        out.putLong("output", output);
        out.putLong("block", block);
        out.putLong("tickCount", tickCount);
        saveTransferFaces(out);
        saveOutputEnabled(out);
        out.putBoolean("placeBlockBelow", placeBlockBelow);
        markerSlot.saveTo(out.child("markerSlot"));
    }

    /**
     * 初始同步到客户端的数据（含标记槽），保证进游戏后方块机即显示标记物品
     * 26.1.2：getUpdateTag 仍存在，参数变为 HolderLookup.Provider
     */
    @Override
    @NotNull
    public CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registryLookup) {
        return saveWithFullMetadata(registryLookup);
    }

    /**
     * 数据变化时发送给客户端的更新包（标记槽变化后强制刷新渲染）
     */
    @Override
    @NotNull
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput input) {
        // 26.1.2：loadAdditional 参数变为 ValueInput，getXOr(key, 当前值) 缺字段保持当前值
        output = Tool.suit(input.getLongOr("output", output));
        block = Tool.suit(input.getLongOr("block", block));
        tickCount = Tool.suit(input.getLongOr("tickCount", tickCount));
        loadTransferFaces(input);
        loadOutputEnabled(input);
        placeBlockBelow = input.getBooleanOr("placeBlockBelow", placeBlockBelow);
        markerSlot.loadFrom(input.childOrEmpty("markerSlot"));
    }
}