package cn.sd.jrz.autoresource.entities;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.menu.LiquidGeneratorMenu;
import cn.sd.jrz.autoresource.setup.ARRegistration;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 流体生成器实体（水源机/岩浆机，26.x 适配）。
 * <p>
 * 负责：产量自动增长、输入槽（空桶/可容纳流体物品）自动填充并转移到输出槽、
 * 上方容器内可容纳流体物品的填充、六面流体传输（可逐面禁用）以及"下方生成流体"。
 * <p>
 * 26.x 适配：
 * <ul>
 *     <li>实体级暴露的 {@link IFluidHandler} 包装已迁移至 {@link net.neoforged.neoforge.transfer.ResourceHandler} 体系（由 {@code LiquidConnection} 提供方块 Capability），不在此实现</li>
 *     <li>{@link ItemStackHandler} 沿用，但 26.x 仍兼容旧的 {@link IFluidHandlerItem} 物品能力</li>
 *     <li>数据持久化使用 26.x 的 {@link ValueInput}/{@link ValueOutput}</li>
 * </ul>
 */
public class LiquidGeneratorEntity extends BlockEntity implements MenuProvider {
    public final DataConfig config;

    // 核心数据（流体数量单位为 mB/1000，即 B；output 单位同样为 mB/1000）
    public long output;
    public long liquid = 0;
    public long tickCount = 0;

    // 六面流体传输开关（逐台保存，可在 GUI 修改，默认全启用）
    public boolean transferDown = true;
    public boolean transferUp = true;
    public boolean transferNorth = true;
    public boolean transferSouth = true;
    public boolean transferWest = true;
    public boolean transferEast = true;

    // 是否在下方空气方块放置对应流体（由 GUI 按钮控制，替代原红石激活判断，默认关闭）
    public boolean placeFluidBelow = false;

    // 输入槽：放入空桶或可容纳本机流体的物品，可放一组物品，组的大小由物品自身堆叠上限决定
    public final ItemStackHandler inputSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            // 空桶：vanilla 桶无流体能力，需特判
            if (stack.is(Items.BUCKET)) {
                return true;
            }
            // 其他物品：通过流体能力判断是否还能容纳本机流体
            ResourceHandler<FluidResource> handler = ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM);
            if (handler == null) {
                return false;
            }
            FluidResource resource = FluidResource.of(config.getFluid());
            for (int tank = 0; tank < handler.size(); tank++) {
                if (handler.isValid(tank, resource) && handler.getAmountAsLong(tank) < handler.getCapacityAsLong(tank, resource)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
            return stack.getMaxStackSize();
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // 输出槽：只允许机器放入已填满的物品，玩家/管道不可主动放入；只能放 1 个
    public final ItemStackHandler outputSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // 六面流体传输轮询索引
    private int findIndex = 0;

    public LiquidGeneratorEntity(BlockPos pos, BlockState state, DataConfig config) {
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
        liquid = Tool.suit(liquid + output);

        fillInputSlot();
        fillContainersAbove();
        if (!isBucketPending()) {
            outputToSides();
        }
        placeFluidBelow();
        setChanged();
    }

    /**
     * 填充输入槽：空桶需要 1000 mB，填满后转移到输出槽；可容纳流体的物品尽量填充，填满后转移到输出槽。
     */
    private void fillInputSlot() {
        if (liquid <= 0) {
            return;
        }
        ItemStack input = inputSlot.getStackInSlot(0);
        if (input.isEmpty()) {
            return;
        }
        if (input.is(Items.BUCKET)) {
            if (liquid >= 1000 && canInsertOutput(new ItemStack(getFilledBucketItem()))) {
                insertOutput(new ItemStack(getFilledBucketItem()));
                consumeOne(input);
                liquid -= 1000;
            }
            return;
        }
        ItemStack copy = input.copy();
        if (copy.getCount() > 1) {
            copy.setCount(1);
        }
        ResourceHandler<FluidResource> fluidHandler = ItemAccess.forStack(copy).getCapability(Capabilities.Fluid.ITEM);
        if (fluidHandler == null) {
            return;
        }
        int maxFill = Tool.suitInt(liquid);
        if (maxFill <= 0) {
            return;
        }
        int filled;
        try (Transaction tx = Transaction.open(null)) {
            filled = fluidHandler.insert(0, FluidResource.of(config.getFluid()), maxFill, tx);
            if (filled <= 0) {
                return;
            }
            tx.commit();
        }
        liquid -= filled;
        // 填充后 copy 的组件已更新（forStack 直接修改栈），结果物品就是 copy
        ItemStack result = copy;
        if (isFull(result)) {
            if (canInsertOutput(result)) {
                insertOutput(result);
                consumeOne(input);
            } else {
                liquid += filled;
            }
        } else if (input.getCount() == 1) {
            inputSlot.setStackInSlot(0, result);
        } else {
            liquid += filled;
        }
    }

    /**
     * 给上方容器中可容纳流体的物品充入流体
     */
    private void fillContainersAbove() {
        Level level = getLevel();
        if (level == null || liquid <= 0) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(getBlockPos().relative(Direction.UP));
        if (blockEntity == null) {
            return;
        }
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, blockEntity.getBlockPos(), Direction.DOWN);
        if (handler == null) {
            return;
        }
        for (int i = 0; i < handler.size(); i++) {
            if (liquid <= 0) {
                return;
            }
            ItemResource resource = handler.getResource(i);
            if (resource == null || resource.isEmpty()) {
                continue;
            }
            int amount = Tool.suitInt(handler.getAmountAsLong(i));
            if (amount <= 0) {
                continue;
            }
            ItemStack stack = resource.toStack(amount);
            if (stack.is(Items.BUCKET)) {
                if (liquid >= 1000) {
                    // 通过 ItemAccess 直接操作容器槽位的流体能力：空桶填充后自动替换为对应流体桶
                    ItemAccess access = ItemAccess.forHandlerIndex(handler, i);
                    ResourceHandler<FluidResource> fluidCap = access.getCapability(Capabilities.Fluid.ITEM);
                    if (fluidCap != null) {
                        try (Transaction tx = Transaction.open(null)) {
                            int filled = fluidCap.insert(0, FluidResource.of(config.getFluid()), 1000, tx);
                            if (filled >= 1000) {
                                tx.commit();
                                liquid -= 1000;
                                blockEntity.setChanged();
                            }
                        }
                    }
                }
                continue;
            }
            ResourceHandler<FluidResource> fluidHandler = ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM);
            if (fluidHandler == null) {
                continue;
            }
            int maxFill = Tool.suitInt(liquid);
            if (maxFill <= 0) {
                return;
            }
            try (Transaction tx = Transaction.open(null)) {
                int filled = fluidHandler.insert(0, FluidResource.of(config.getFluid()), maxFill, tx);
                if (filled > 0) {
                    tx.commit();
                    liquid -= filled;
                    blockEntity.setChanged();
                }
            }
        }
    }

    /**
     * 六面流体传输（跳过被禁用的面），轮询索引实现负载均衡
     */
    private void outputToSides() {
        Level level = getLevel();
        if (level == null || liquid <= 0) {
            return;
        }
        Direction[] directions = Direction.values();
        BlockPos blockPos = getBlockPos();
        for (int i = 0; i < directions.length; i++) {
            if (liquid <= 0) {
                return;
            }
            findIndex = (findIndex + 1) % directions.length;
            Direction direction = directions[findIndex];
            if (!isTransferEnabled(direction)) {
                continue;
            }
            BlockPos pos = blockPos.relative(direction);
            int maxOutput = Tool.suitInt(liquid);
            ResourceHandler<FluidResource> storage = level.getCapability(Capabilities.Fluid.BLOCK, pos, direction.getOpposite());
            if (storage == null) {
                continue;
            }
            int count = ResourceHandlerUtil.insertStacking(
                    storage, FluidResource.of(config.getFluid()), maxOutput, null);
            if (count < 0) {
                count = 0;
            }
            if (count > maxOutput) {
                count = maxOutput;
            }
            liquid -= count;
        }
    }

    /**
     * 开启"下方生成流体"时，每 5 ticks 尝试向下方空气方块放置对应流体，每次消耗 1000 mB
     */
    private void placeFluidBelow() {
        Level level = getLevel();
        if (level == null || !placeFluidBelow) {
            return;
        }
        if (liquid >= 1000 && tickCount % 5 == 0) {
            BlockPos pos = getBlockPos().relative(Direction.DOWN);
            if (level.getBlockState(pos).getBlock() == Blocks.AIR && level.setBlock(pos, config.getBlock().defaultBlockState(), 3)) {
                liquid -= 1000;
            }
        }
    }

    /**
     * 输入槽中是否有正在等待填充的空桶（此时保留液体、暂不向六面输出，以便积累液体填桶）
     */
    private boolean isBucketPending() {
        ItemStack input = inputSlot.getStackInSlot(0);
        if (!input.is(Items.BUCKET)) {
            return false;
        }
        if (!canInsertOutput(new ItemStack(getFilledBucketItem()))) {
            return false;
        }
        return liquid < 1000;
    }

    /**
     * 对应流体桶的物品（水桶/岩浆桶）
     */
    private net.minecraft.world.item.Item getFilledBucketItem() {
        return config.getFluid() == net.minecraft.world.level.material.Fluids.WATER ? Items.WATER_BUCKET : Items.LAVA_BUCKET;
    }

    /**
     * 输出槽能否放入该物品
     */
    private boolean canInsertOutput(ItemStack stack) {
        ItemStack out = outputSlot.getStackInSlot(0);
        if (out.isEmpty()) {
            return true;
        }
        return out.is(stack.getItem()) && out.getCount() + stack.getCount() <= outputSlot.getSlotLimit(0);
    }

    /**
     * 把一个物品放入输出槽
     */
    private void insertOutput(ItemStack stack) {
        ItemStack out = outputSlot.getStackInSlot(0);
        if (out.isEmpty()) {
            outputSlot.setStackInSlot(0, stack.copy());
        } else {
            out.grow(stack.getCount());
            outputSlot.setStackInSlot(0, out);
        }
    }

    /**
     * 判断物品是否已无法再容纳本机流体
     */
    private boolean isFull(ItemStack stack) {
        ResourceHandler<FluidResource> handler = ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) {
            return true;
        }
        FluidResource resource = FluidResource.of(config.getFluid());
        for (int tank = 0; tank < handler.size(); tank++) {
            if (handler.getAmountAsLong(tank) < handler.getCapacityAsLong(tank, resource)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 消耗输入槽中的 1 个物品
     */
    private void consumeOne(ItemStack input) {
        input.shrink(1);
        inputSlot.setStackInSlot(0, input.isEmpty() ? ItemStack.EMPTY : input);
    }

    /**
     * 指定面是否允许流体传输
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
        return Component.translatable("block.autoresource.liquid_generator_water");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new LiquidGeneratorMenu(id, inv, worldPosition);
    }

    @Override
    public void saveAdditional(@Nonnull ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.putLong("output", output);
        valueOutput.putLong("liquid", liquid);
        valueOutput.putLong("tickCount", tickCount);
        valueOutput.putBoolean("transferDown", transferDown);
        valueOutput.putBoolean("transferUp", transferUp);
        valueOutput.putBoolean("transferNorth", transferNorth);
        valueOutput.putBoolean("transferSouth", transferSouth);
        valueOutput.putBoolean("transferWest", transferWest);
        valueOutput.putBoolean("transferEast", transferEast);
        valueOutput.putBoolean("placeFluidBelow", placeFluidBelow);
        inputSlot.serialize(valueOutput.child("inputSlot"));
        outputSlot.serialize(valueOutput.child("outputSlot"));
    }

    @Override
    public void loadAdditional(@Nonnull ValueInput valueInput) {
        super.loadAdditional(valueInput);
        valueInput.getLong("output").ifPresent(it -> this.output = Tool.suit(it));
        valueInput.getLong("liquid").ifPresent(it -> this.liquid = Tool.suit(it));
        valueInput.getLong("tickCount").ifPresent(it -> this.tickCount = Tool.suit(it));
        this.transferDown = valueInput.getBooleanOr("transferDown", this.transferDown);
        this.transferUp = valueInput.getBooleanOr("transferUp", this.transferUp);
        this.transferNorth = valueInput.getBooleanOr("transferNorth", this.transferNorth);
        this.transferSouth = valueInput.getBooleanOr("transferSouth", this.transferSouth);
        this.transferWest = valueInput.getBooleanOr("transferWest", this.transferWest);
        this.transferEast = valueInput.getBooleanOr("transferEast", this.transferEast);
        this.placeFluidBelow = valueInput.getBooleanOr("placeFluidBelow", this.placeFluidBelow);
        inputSlot.deserialize(valueInput.childOrEmpty("inputSlot"));
        outputSlot.deserialize(valueInput.childOrEmpty("outputSlot"));
    }

    /**
     * 物品 DataComponent 编码格式：
     * output,liquid,tickCount,transferDown,transferUp,transferNorth,transferSouth,transferWest,transferEast,placeFluidBelow
     */
    @Override
    protected void collectImplicitComponents(@Nonnull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(ARRegistration.BLOCK_DATA.get(),
                output + "," + liquid + "," + tickCount + ","
                        + (transferDown ? 1 : 0) + "," + (transferUp ? 1 : 0) + "," + (transferNorth ? 1 : 0) + ","
                        + (transferSouth ? 1 : 0) + "," + (transferWest ? 1 : 0) + "," + (transferEast ? 1 : 0) + ","
                        + (placeFluidBelow ? 1 : 0));
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
        liquid = Tool.parseLong(dataArray, 1);
        tickCount = Tool.parseLong(dataArray, 2);
        transferDown = Tool.parseInt(dataArray, 3) == 1;
        transferUp = Tool.parseInt(dataArray, 4) == 1;
        transferNorth = Tool.parseInt(dataArray, 5) == 1;
        transferSouth = Tool.parseInt(dataArray, 6) == 1;
        transferWest = Tool.parseInt(dataArray, 7) == 1;
        transferEast = Tool.parseInt(dataArray, 8) == 1;
        placeFluidBelow = Tool.parseInt(dataArray, 9) == 1;
    }
}
