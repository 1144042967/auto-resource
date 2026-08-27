package cn.sd.jrz.autoresource.blockentity;

import cn.sd.jrz.autoresource.capability.DualSlotPipeView;
import cn.sd.jrz.autoresource.capability.LiquidConnection;
import cn.sd.jrz.autoresource.menu.LiquidGeneratorMenu;
import cn.sd.jrz.autoresource.storage.MachineSlotStorage;
import cn.sd.jrz.autoresource.util.ItemFluidIo;
import cn.sd.jrz.autoresource.util.Tool;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 流体生成器实体（水源机/岩浆机）：产量自动增长、输入槽填充/转移输出、上方容器充液、
 * 六面流体传输（可逐面禁用）、"下方生成流体"。各参数逐台独立保存。
 *
 * 单位说明：内部存储单位为 mB（显示层除以 1000 折算成桶）；对外 Transfer API 以 droplets 计（×81）。
 */
public class LiquidGeneratorEntity extends AbstractGeneratorEntity {

    // 核心数据（内部单位 mB）
    public long liquid = 0;

    // 是否在下方空气方块放置对应流体（GUI 按钮控制，默认关闭）
    public boolean placeFluidBelow = false;

    // 输入槽：空桶或可容纳本机流体的物品，组大小由物品自身堆叠上限决定
    public final MachineSlotStorage inputSlot = new MachineSlotStorage(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            // 空桶特判（vanilla 桶不在 Transfer API 物品流体查找之列）
            if (stack.is(Items.BUCKET)) {
                return true;
            }
            return ItemFluidIo.accepts(stack, config.getFluid());
        }
    };

    // 输出槽：只允许机器放入已填满的物品，玩家/管道不可主动放入；只能放 1 个
    public final MachineSlotStorage outputSlot = new MachineSlotStorage(1).setValidator(stack -> false);

    // 对外连接实例（六面相同；由 TransferSetup 分别暴露到 FluidStorage/ItemStorage 的 SIDED 查找）
    private final LiquidConnection fluidConnection = new LiquidConnection(this);
    private final DualSlotPipeView pipeView = new DualSlotPipeView(this);

    public LiquidConnection getFluidConnection() {
        return fluidConnection;
    }

    public DualSlotPipeView getPipeView() {
        return pipeView;
    }

    public LiquidGeneratorEntity(BlockPos pos, BlockState state, DataConfig config) {
        super(pos, state, config);
        outputSlot.setSlotLimit(0, 1);
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
        // 六面传输（有待填充的铁桶时保留液体，优先积累液体填桶；主动输出总开关关闭时不传输）
        if (!isBucketPending() && outputEnabled) {
            outputToSides();
        }
        // 开启"下方生成流体"时，向下方空气方块放置流体
        placeFluidBelowTick();
        markDirtyTick();
    }

    /**
     * 填充输入槽：空桶需 1000 mB、可容纳流体的物品尽量填充，填满后转移到输出槽
     */
    private void fillInputSlot() {
        if (liquid <= 0) {
            return;
        }
        ItemStack input = inputSlot.getStackInSlot(0);
        if (input.isEmpty()) {
            return;
        }
        // 空桶：特判（vanilla 桶不在物品流体查找内）
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
        long maxFill = Tool.suitInt(liquid);
        if (maxFill <= 0) {
            return;
        }
        ItemFluidIo.FillResult fillResult = ItemFluidIo.fill(copy, config.getFluid(), maxFill);
        if (fillResult == null) {
            return;
        }
        int filled = (int) Math.min(fillResult.millibuckets(), maxFill);
        if (filled <= 0) {
            return;
        }
        ItemStack result = fillResult.filled();
        if (ItemFluidIo.isFull(result, config.getFluid())) {
            // 已满 → 转移到输出槽（1 件）
            if (canInsertOutput(result)) {
                insertOutput(result);
                consumeOne(input);
                liquid -= filled;
            }
            // 输出槽满：回退（填入结果丢弃、液体不扣），等待输出槽腾出
        } else if (input.getCount() == 1) {
            // 单件未满 → 写回输入槽继续填充
            inputSlot.setStackInSlot(0, result);
            liquid -= filled;
        } else {
            // 堆叠物品无法干净放回部分填充的单件：结果丢弃、液体不扣
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
        if (!(blockEntity instanceof Container container)) {
            return;
        }
        InventoryStorage storageView = InventoryStorage.of(container, null);
        for (int i = 0; i < storageView.getSlotCount(); i++) {
            if (liquid <= 0) {
                return;
            }
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            SlottedStorage<?> slotView = storageView.getSlot(i);
            // 空桶特判：同一事务内取出空桶、放入流体桶
            if (stack.is(Items.BUCKET)) {
                if (liquid >= 1000 && fillBucketInAboveContainer(blockEntity, slotView)) {
                    liquid -= 1000;
                    blockEntity.setChanged();
                }
                continue;
            }
            // 可容纳流体的物品：经槽位上下文填充（变更自动回写原槽位）
            ContainerItemContext context = ContainerItemContext.forSlot(slotView);
            Storage<FluidVariant> storage = FluidStorage.ITEM.find(context.getItemVariant(), context);
            if (storage == null || !storage.supportsInsertion()) {
                continue;
            }
            long requestedDroplets = ItemFluidIo.safeMul(Tool.suitInt(liquid), ItemFluidIo.DROPLETS_PER_MB);
            if (requestedDroplets <= 0) {
                return;
            }
            try (var txn = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                long filledDroplets = storage.insert(FluidVariant.of(config.getFluid()), requestedDroplets, txn);
                if (filledDroplets <= 0) {
                    continue;
                }
                txn.commit();
                long millibuckets = filledDroplets / ItemFluidIo.DROPLETS_PER_MB;
                if (millibuckets > 0) {
                    liquid -= millibuckets;
                    // 物品流体内存被修改，标记容器已改变以便落盘/同步
                    blockEntity.setChanged();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 上方容器槽位内的桶替换：取 1 个空桶放入同数量的对应流体桶（同槽有剩余空桶时会因容量不足失败回滚）
     */
    private boolean fillBucketInAboveContainer(BlockEntity blockEntity, SlottedStorage<?> slotView) {
        Item filledBucket = getFilledBucketItem();
        try (var txn = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            long taken = slotView.extract(ItemVariant.of(Items.BUCKET), 1, txn);
            if (taken != 1) {
                return false;
            }
            long placed = slotView.insert(ItemVariant.of(filledBucket), 1, txn);
            if (placed != 1) {
                return false;
            }
            txn.commit();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 六面流体传输（跳过被禁用的面），轮询索引负载均衡
     */
    private void outputToSides() {
        Level level = getLevel();
        if (level == null || liquid <= 0) {
            return;
        }
        Direction[] directions = Direction.values();
        BlockPos blockPos = getBlockPos();
        FluidVariant variant = FluidVariant.of(config.getFluid());
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
            Storage<FluidVariant> target = FluidStorage.SIDED.find(level, pos, direction.getOpposite());
            if (target == null || !target.supportsInsertion()) {
                continue;
            }
            long requestedDroplets = LiquidConnection.internalToDroplets(Math.min(Tool.suitInt(liquid), liquid));
            try (var txn = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                long pushed = target.insert(variant, requestedDroplets, txn);
                if (pushed <= 0) {
                    continue;
                }
                txn.commit();
                // 扣减向下取整到 mB（不足 81 droplet 的零头归入耗损）
                liquid -= pushed / ItemFluidIo.DROPLETS_PER_MB;
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * "下方生成流体"开启时每 5 ticks 向下方空气放置对应流体，每次消耗 1000 mB
     */
    private void placeFluidBelowTick() {
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
     * 输入槽是否有等待填充的空桶（此时保留液体、暂不向六面输出）
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
     * 输出槽能否放入该物品（空槽或同种且未达堆叠上限）
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
     * 消耗输入槽中的 1 个物品
     */
    private void consumeOne(ItemStack input) {
        input.shrink(1);
        inputSlot.setStackInSlot(0, input.isEmpty() ? ItemStack.EMPTY : input);
    }

    @Override
    @Nonnull
    public Component getDisplayName() {
        BlockState state = getLevel() != null ? getLevel().getBlockState(getBlockPos()) : null;
        if (state != null && !state.isAir()) {
            return Component.translatable(state.getBlock().getDescriptionId());
        }
        return Component.translatable("block.autoresource.liquid_generator_water");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new LiquidGeneratorMenu(id, inv, getBlockPos());
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putLong("output", output);
        nbt.putLong("liquid", liquid);
        nbt.putLong("tickCount", tickCount);
        saveTransferFaces(nbt);
        saveOutputEnabled(nbt);
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
        loadOutputEnabled(nbt);
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
