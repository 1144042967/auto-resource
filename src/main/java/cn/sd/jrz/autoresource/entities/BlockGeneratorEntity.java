package cn.sd.jrz.autoresource.entities;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.menu.BlockGeneratorMenu;
import cn.sd.jrz.autoresource.setup.ARRegistration;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 方块生成器实体（26.x 适配）。
 * <p>
 * 负责：产量自动增长、标记槽（放入一个合法方块生成机产品后锁定，决定输出方块的种类）、
 * 六面方块传输（可逐面禁用）以及"下方生成方块"。
 * 自动生成会一直计算，但未标记时无法取出/传输/放置；标记后不可更换。
 */
public class BlockGeneratorEntity extends BlockEntity implements MenuProvider {
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
            if (level != null && !level.isClientSide()) {
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
            outputToSides(marked);
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
            net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> handler =
                    level.getCapability(Capabilities.Item.BLOCK, pos, direction.getOpposite());
            if (handler == null) {
                continue;
            }
            int maxOutput = Tool.suitInt(block / 1000);
            int count = net.neoforged.neoforge.transfer.ResourceHandlerUtil.insertStacking(
                    handler, net.neoforged.neoforge.transfer.item.ItemResource.of(marked.getItem()), maxOutput, null);
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
    public void saveAdditional(@Nonnull ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.putLong("output", output);
        valueOutput.putLong("block", block);
        valueOutput.putLong("tickCount", tickCount);
        valueOutput.putBoolean("transferDown", transferDown);
        valueOutput.putBoolean("transferUp", transferUp);
        valueOutput.putBoolean("transferNorth", transferNorth);
        valueOutput.putBoolean("transferSouth", transferSouth);
        valueOutput.putBoolean("transferWest", transferWest);
        valueOutput.putBoolean("transferEast", transferEast);
        valueOutput.putBoolean("placeBlockBelow", placeBlockBelow);
        valueOutput.store("markerSlot", CompoundTag.CODEC, markerSlot.serializeNBT(holderLookup()));
    }

    @Override
    public void loadAdditional(@Nonnull ValueInput valueInput) {
        super.loadAdditional(valueInput);
        valueInput.getLong("output").ifPresent(it -> this.output = Tool.suit(it));
        valueInput.getLong("block").ifPresent(it -> this.block = Tool.suit(it));
        valueInput.getLong("tickCount").ifPresent(it -> this.tickCount = Tool.suit(it));
        valueInput.getBoolean("transferDown").ifPresent(it -> this.transferDown = it);
        valueInput.getBoolean("transferUp").ifPresent(it -> this.transferUp = it);
        valueInput.getBoolean("transferNorth").ifPresent(it -> this.transferNorth = it);
        valueInput.getBoolean("transferSouth").ifPresent(it -> this.transferSouth = it);
        valueInput.getBoolean("transferWest").ifPresent(it -> this.transferWest = it);
        valueInput.getBoolean("transferEast").ifPresent(it -> this.transferEast = it);
        valueInput.getBoolean("placeBlockBelow").ifPresent(it -> this.placeBlockBelow = it);
        valueInput.read("markerSlot", CompoundTag.CODEC).ifPresent(nbt -> markerSlot.deserializeNBT(holderLookup(), nbt));
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
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(markerItemId)), 1);
            if (!stack.isEmpty()) {
                markerSlot.setStackInSlot(0, stack);
            }
        }
    }
}
