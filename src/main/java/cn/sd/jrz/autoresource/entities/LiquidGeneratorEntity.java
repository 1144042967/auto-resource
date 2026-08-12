package cn.sd.jrz.autoresource.entities;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.connection.LiquidConnection;
import cn.sd.jrz.autoresource.menu.LiquidGeneratorMenu;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 流体生成器实体（水源机/岩浆机）。
 * <p>
 * 负责：产量自动增长、输入槽（空桶/可容纳流体物品）自动填充并转移到输出槽、
 * 上方容器内可容纳流体物品的填充、六面流体传输（可逐面禁用）以及"下方生成流体"。
 * 六面开关与"下方生成流体"为每台机器独立保存，可在 GUI 中修改。
 */
public class LiquidGeneratorEntity extends AbstractGeneratorEntity {
    private final LazyOptional<LiquidConnection> fluidOptional = LazyOptional.of(() -> new LiquidConnection(this));
    /**
     * 物品管道能力：输入走输入槽（可插入），输出走输出槽（可抽取）；
     * 输入槽不可抽取、输出槽不可插入，保证管道单向流动。
     */
    private final LazyOptional<IItemHandler> itemOptional = LazyOptional.of(() -> new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        @Nonnull
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? inputSlot.getStackInSlot(0) : outputSlot.getStackInSlot(0);
        }

        @Override
        @Nonnull
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return slot == 0 ? inputSlot.insertItem(0, stack, simulate) : stack;
        }

        @Override
        @Nonnull
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 ? outputSlot.extractItem(0, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? inputSlot.getSlotLimit(0) : outputSlot.getSlotLimit(0);
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot == 0 && inputSlot.isItemValid(0, stack);
        }
    });

    // 核心数据（流体数量单位为 mB/1000，即 B；output 单位同样为 mB/1000）
    public long liquid = 0;

    // 是否在下方空气方块放置对应流体（由 GUI 按钮控制，替代原红石激活判断，默认关闭）
    public boolean placeFluidBelow = false;

    // 输入槽：放入空桶或可容纳本机流体的物品，可放一组物品，组的大小由物品自身堆叠上限决定
    public final ItemStackHandler inputSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            // 空桶（vanilla 桶无 FLUID_HANDLER_ITEM 能力，需特判）
            if (stack.is(Items.BUCKET)) {
                return true;
            }
            // 其他物品：通过 FLUID_HANDLER_ITEM 能力判断是否还能容纳本机流体
            return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                    .map(handler -> {
                        for (int tank = 0; tank < handler.getTanks(); tank++) {
                            FluidStack fluid = handler.getFluidInTank(tank);
                            if ((fluid.isEmpty() || fluid.getFluid() == config.getFluid()) && handler.getTankCapacity(tank) > fluid.getAmount()) {
                                return true;
                            }
                        }
                        return false;
                    }).orElse(false);
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
            // 组的大小由物品自身的堆叠上限决定（如铁桶 16、普通物品 64）
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

    public LiquidGeneratorEntity(BlockPos pos, BlockState state, DataConfig config) {
        super(pos, state, config);
    }

    /**
     * 服务端每 tick 调用（由方块的 ticker 触发）
     */
    public void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        // 增长逻辑：产量到间隔后增加
        tickCount = Tool.suit(tickCount + 1);
        if (tickCount / 20 >= config.getSecond()) {
            tickCount = 0;
            output = Math.min(config.getMax(), Tool.suit(output + config.getStep()));
        }
        liquid = Tool.suit(liquid + output);

        // 优先填充输入槽（空桶/可容纳流体物品）
        fillInputSlot();
        // 填充上方容器中的可容纳流体物品
        fillContainersAbove();
        // 六面传输（有待填充的铁桶时保留液体，优先积累液体填桶）
        if (!isBucketPending()) {
            outputToSides();
        }
        // 开启"下方生成流体"时，向下方空气方块放置流体
        placeFluidBelow();
        markDirtyTick();
    }

    /**
     * 填充输入槽：
     * 空桶需要 1000 mB，填满后转移到输出槽；可容纳流体的物品尽量填充，填满后转移到输出槽。
     */
    private void fillInputSlot() {
        if (liquid <= 0) {
            return;
        }
        ItemStack input = inputSlot.getStackInSlot(0);
        if (input.isEmpty()) {
            return;
        }
        // 空桶：特判（vanilla 桶无 FLUID_HANDLER_ITEM 能力）
        if (input.is(Items.BUCKET)) {
            if (liquid >= 1000 && canInsertOutput(new ItemStack(getFilledBucketItem()))) {
                insertOutput(new ItemStack(getFilledBucketItem()));
                consumeOne(input);
                liquid -= 1000;
            }
            return;
        }
        // 其他可容纳流体的物品：逐件填充，填满后移入输出槽（输出槽限 1 个）
        ItemStack copy = input.copy();
        if (copy.getCount() > 1) {
            // 堆叠的可装流体物品每次只处理 1 件，避免整组共享 NBT 造成输出超限
            copy.setCount(1);
        }
        copy.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().ifPresent(fluidHandler -> {
            int maxFill = Tool.suitInt(liquid);
            if (maxFill <= 0) {
                return;
            }
            int filled = fluidHandler.fill(new FluidStack(config.getFluid(), maxFill), IFluidHandler.FluidAction.EXECUTE);
            if (filled <= 0) {
                return;
            }
            liquid -= filled;
            ItemStack result = fluidHandler.getContainer();
            if (result.isEmpty()) {
                result = copy;
            }
            if (isFull(result)) {
                // 填满 → 转移到输出槽（1 件）
                if (canInsertOutput(result)) {
                    insertOutput(result);
                    consumeOne(input);
                } else {
                    // 输出槽满：回退液体，输入保持原样，等待输出槽腾出
                    liquid += filled;
                }
            } else if (input.getCount() == 1) {
                // 单件未满 → 写回输入槽继续填充
                inputSlot.setStackInSlot(0, result);
            } else {
                // 堆叠物品无法干净放回部分填充的单件，回退液体
                liquid += filled;
            }
        });
    }

    /**
     * 给上方容器中可容纳流体的物品充入流体（箱子、漏斗等带物品栏的方块实体）
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
        blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (liquid <= 0) {
                    return;
                }
                ItemStack stack = handler.getStackInSlot(i);
                if (stack.isEmpty()) {
                    continue;
                }
                // 空桶特判：消耗一个空桶并在容器内放入流体桶
                if (stack.is(Items.BUCKET)) {
                    if (liquid >= 1000) {
                        ItemStack leftover = ItemHandlerHelper.insertItemStacked(handler, new ItemStack(getFilledBucketItem()), false);
                        if (leftover.isEmpty()) {
                            handler.extractItem(i, 1, false);
                            liquid -= 1000;
                            blockEntity.setChanged();
                        }
                    }
                    continue;
                }
                // 可容纳流体的物品：直接填充（修改的是容器内的物品栈）
                stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().ifPresent(fluidHandler -> {
                    int maxFill = Tool.suitInt(liquid);
                    if (maxFill <= 0) {
                        return;
                    }
                    int filled = fluidHandler.fill(new FluidStack(config.getFluid(), maxFill), IFluidHandler.FluidAction.EXECUTE);
                    if (filled > 0) {
                        liquid -= filled;
                        // 物品流体内存被修改，标记容器已改变以便落盘/同步
                        blockEntity.setChanged();
                    }
                });
            }
        });
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
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                continue;
            }
            int maxOutput = Tool.suitInt(liquid);
            IFluidHandler storage = entity.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite()).resolve().filter(handler -> {
                int tanks = handler.getTanks();
                for (int tank = 0; tank < tanks; tank++) {
                    if (handler.isFluidValid(tank, new FluidStack(config.getFluid(), maxOutput))) {
                        return true;
                    }
                }
                return false;
            }).orElse(null);
            if (storage == null) {
                continue;
            }
            int result = storage.fill(new FluidStack(config.getFluid(), maxOutput), IFluidHandler.FluidAction.EXECUTE);
            if (result < 0) {
                result = 0;
            }
            if (result > maxOutput) {
                result = maxOutput;
            }
            liquid -= result;
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
        // 输出槽无法接收流体桶时不保留液体，避免机器卡死
        if (!canInsertOutput(new ItemStack(getFilledBucketItem()))) {
            return false;
        }
        return liquid < 1000;
    }

    /**
     * 对应流体桶的物品（水桶/岩浆桶）
     */
    private Item getFilledBucketItem() {
        return config.getFluid() == Fluids.WATER ? Items.WATER_BUCKET : Items.LAVA_BUCKET;
    }

    /**
     * 输出槽能否放入该物品（空槽或同种且未达到槽位堆叠上限）
     */
    private boolean canInsertOutput(ItemStack stack) {
        ItemStack out = outputSlot.getStackInSlot(0);
        if (out.isEmpty()) {
            return true;
        }
        return out.is(stack.getItem()) && out.getCount() + stack.getCount() <= outputSlot.getSlotLimit(0);
    }

    /**
     * 把一个物品放入输出槽（调用前需先通过 canInsertOutput 校验）
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
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .map(handler -> {
                    for (int tank = 0; tank < handler.getTanks(); tank++) {
                        FluidStack fluid = handler.getFluidInTank(tank);
                        if ((fluid.isEmpty() || fluid.getFluid() == config.getFluid()) && handler.getTankCapacity(tank) > fluid.getAmount()) {
                            return false;
                        }
                    }
                    return true;
                }).orElse(true);
    }

    /**
     * 消耗输入槽中的 1 个物品（输入槽容量为 1）
     */
    private void consumeOne(ItemStack input) {
        input.shrink(1);
        inputSlot.setStackInSlot(0, input.isEmpty() ? ItemStack.EMPTY : input);
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return fluidOptional.cast();
        }
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemOptional.cast();
        }
        return super.getCapability(capability, direction);
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
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putLong("output", output);
        nbt.putLong("liquid", liquid);
        nbt.putLong("tickCount", tickCount);
        saveTransferFaces(nbt);
        nbt.putBoolean("placeFluidBelow", placeFluidBelow);
        nbt.put("inputSlot", inputSlot.serializeNBT());
        nbt.put("outputSlot", outputSlot.serializeNBT());
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("output", Tag.TAG_LONG)) {
            output = Tool.suit(nbt.getLong("output"));
        }
        if (nbt.contains("liquid", Tag.TAG_LONG)) {
            liquid = Tool.suit(nbt.getLong("liquid"));
        }
        if (nbt.contains("tickCount", Tag.TAG_LONG)) {
            tickCount = Tool.suit(nbt.getLong("tickCount"));
        }
        loadTransferFaces(nbt);
        if (nbt.contains("placeFluidBelow", Tag.TAG_BYTE)) {
            placeFluidBelow = nbt.getBoolean("placeFluidBelow");
        }
        if (nbt.contains("inputSlot", Tag.TAG_COMPOUND)) {
            inputSlot.deserializeNBT(nbt.getCompound("inputSlot"));
        }
        if (nbt.contains("outputSlot", Tag.TAG_COMPOUND)) {
            outputSlot.deserializeNBT(nbt.getCompound("outputSlot"));
        }
    }
}
