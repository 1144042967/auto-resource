package cn.sd.jrz.autoresource.entities;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.menu.BlockGeneratorMenu;
import cn.sd.jrz.autoresource.setup.ARRegistration;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 方块生成器实体（26.x 适配）。
 * <p>
 * 负责：产量自动增长、标记槽（放入一个合法方块生成机产品后锁定，决定输出方块的种类）、
 * 六面方块传输（可逐面禁用）以及"下方生成方块"。
 * 自动生成会一直计算，但未标记时无法取出/传输/放置；标记后不可更换。
 */
public class BlockGeneratorEntity extends AbstractGeneratorEntity {
    // 核心数据（方块数量单位为 Block/1000，即块）
    public long block = 0;

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
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            }
        }
    };

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
        tickCount = Tool.suit(tickCount + 1);
        if (tickCount / 20 >= config.getSecond()) {
            tickCount = 0;
            output = Math.min(config.getMax(), Tool.suit(output + config.getStep()));
        }
        block = Tool.suit(block + output);

        ItemStack marked = getMarkedItem();
        if (!marked.isEmpty()) {
            // 标记后才向六面传输方块（关闭主动输出总开关时不传输）
            if (outputEnabled) {
                outputToSides(marked);
            }
            // 开启"下方生成方块"时，向下方空气方块放置方块
            placeBlockBelow(marked);
        }
        markDirtyTick();
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
            ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, direction.getOpposite());
            if (handler == null) {
                continue;
            }
            int maxOutput = Tool.suitInt(block / 1000);
            int count = ResourceHandlerUtil.insertStacking(
                    handler, ItemResource.of(marked.getItem()), maxOutput, null);
            if (count < 0) {
                count = 0;
            }
            if (count > maxOutput) {
                count = maxOutput;
            }
            block -= count * 1000L;
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

    /**
     * 初始同步到客户端的数据（包含标记槽），保证进游戏后方块机上即可显示标记物品
     */
    @Override
    @Nonnull
    public CompoundTag getUpdateTag(@Nonnull HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    /**
     * 数据变化时发送给客户端的更新包（标记槽变化后强制刷新渲染）
     */
    @Override
    @Nonnull
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void saveAdditional(@Nonnull ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.putLong("output", output);
        valueOutput.putLong("block", block);
        valueOutput.putLong("tickCount", tickCount);
        saveTransferFaces(valueOutput);
        saveOutputEnabled(valueOutput);
        valueOutput.putBoolean("placeBlockBelow", placeBlockBelow);
        markerSlot.serialize(valueOutput.child("markerSlot"));
    }

    @Override
    public void loadAdditional(@Nonnull ValueInput valueInput) {
        super.loadAdditional(valueInput);
        valueInput.getLong("output").ifPresent(it -> this.output = Tool.suit(it));
        valueInput.getLong("block").ifPresent(it -> this.block = Tool.suit(it));
        valueInput.getLong("tickCount").ifPresent(it -> this.tickCount = Tool.suit(it));
        loadTransferFaces(valueInput);
        loadOutputEnabled(valueInput);
        this.placeBlockBelow = valueInput.getBooleanOr("placeBlockBelow", this.placeBlockBelow);
        markerSlot.deserialize(valueInput.childOrEmpty("markerSlot"));
    }

    /**
     * 物品 DataComponent 编码格式：
     * output,block,tickCount,transferDown,transferUp,transferNorth,transferSouth,transferWest,transferEast,placeBlockBelow,markerItemId
     */
    @Override
    protected void collectImplicitComponents(@Nonnull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        ItemStack marked = getMarkedItem();
        builder.set(ARRegistration.BLOCK_DATA.get(),
                output + "," + block + "," + tickCount + ","
                        + (transferDown ? 1 : 0) + "," + (transferUp ? 1 : 0) + "," + (transferNorth ? 1 : 0) + ","
                        + (transferSouth ? 1 : 0) + "," + (transferWest ? 1 : 0) + "," + (transferEast ? 1 : 0) + ","
                        + (placeBlockBelow ? 1 : 0) + ","
                        + (marked.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(marked.getItem()).toString()));
    }

    @Override
    protected void applyImplicitComponents(@Nonnull DataComponentGetter input) {
        super.applyImplicitComponents(input);
        String blockData = input.getOrDefault(ARRegistration.BLOCK_DATA.get(), "");
        if (blockData.isEmpty()) {
            return;
        }
        String[] dataArray = blockData.split(",");
        output = Tool.parseLong(dataArray, 0);
        block = Tool.parseLong(dataArray, 1);
        tickCount = Tool.parseLong(dataArray, 2);
        transferDown = Tool.parseInt(dataArray, 3) == 1;
        transferUp = Tool.parseInt(dataArray, 4) == 1;
        transferNorth = Tool.parseInt(dataArray, 5) == 1;
        transferSouth = Tool.parseInt(dataArray, 6) == 1;
        transferWest = Tool.parseInt(dataArray, 7) == 1;
        transferEast = Tool.parseInt(dataArray, 8) == 1;
        placeBlockBelow = Tool.parseInt(dataArray, 9) == 1;
        String markerItemId = Tool.parseString(dataArray, 10);
        if (!markerItemId.isEmpty()) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.tryParse(markerItemId)), 1);
            if (!stack.isEmpty()) {
                markerSlot.setStackInSlot(0, stack);
            }
        }
    }
}
