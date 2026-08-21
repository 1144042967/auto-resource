package cn.sd.jrz.autoresource.entities;

import cn.sd.jrz.autoresource.Config;
import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.compat.energybypass.EnergyBypass;
import cn.sd.jrz.autoresource.menu.EnergyGeneratorMenu;
import cn.sd.jrz.autoresource.setup.Registration;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FE 发电机实体。
 * <p>
 * 负责：发电量自动增长、能量存储、上方实体（玩家/生物）全部槽位充电、
 * 上方容器内物品充电、六面输电（可逐面禁用）、充电槽物品充电、
 * 指定物品加速增长（增长量变为当前发电量的 1%）以及无线充电。
 * 无线充电与输电面等参数均为每台发电机独立保存，可在 GUI 中修改。
 */
public class EnergyGeneratorEntity extends AbstractGeneratorEntity {
    public long energy = 0;
    /**
     * 下次增长的发电量（同时用于增长时实际增量）
     */
    public long nextIncrease = 0;

    // 无线充电开关（逐台保存）
    public boolean wirelessOn = false;
    /**
     * 无线扫描游标：记录上次扫描到的线性位置（按全部方块展平），下次从该位置继续
     */
    public long scanCursor = 0;
    /**
     * 已记录的支持电量接收的位置及其接收面（分片扫描时更新，传输时遍历）
     */
    public final Map<BlockPos, Direction> wirelessTargets = new HashMap<>();

    // 无线充电参数（逐台保存，可在 GUI 修改）
    public int wirelessInterval = 5;
    public int wirelessRange = 1;
    /**
     * 重复传电次数，对相邻输电和无线输电都生效
     */
    public int transferRepeat = 1;

    // 加速增长槽位（放入配置指定物品后增长量变为当前发电量的 1%），只能放 1 个
    public final ItemStackHandler starSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return stack.is(config.getStarItem());
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // 充电槽位（可放入可充电物品为其充电），每次充电 1 个
    public final ItemStackHandler chargeSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            return storage != null && storage.canReceive();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public EnergyGeneratorEntity(BlockPos pos, BlockState state, DataConfig config) {
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
        // 增长逻辑：先刷新下次增长量，到达间隔后应用
        tickCount = Tool.suit(tickCount + 1);
        updateNextIncrease();
        if (tickCount / 20 >= config.getSecond()) {
            tickCount = 0;
            output = Math.min(config.getMax(), Tool.suit(output + nextIncrease));
        }
        // 发电
        energy = Tool.suit(energy + output);

        // 充电槽充电
        chargeChargeSlot();
        // 上方实体充电（玩家物品栏/存储栏/装备栏全部覆盖）
        chargeEntitiesAbove();
        // 上方容器充电
        chargeContainersAbove();
        // 六面输电（有线传输优先级更高）
        outputToSides();
        // 无线充电（优先级更低）
        if (wirelessOn) {
            wirelessTick();
        }
        markDirtyTick();
    }

    /**
     * 计算下一次增长的发电量并保存到 nextIncrease（原信标功能已由加速槽代替）
     */
    private void updateNextIncrease() {
        long increase = config.getStep();
        if (!starSlot.getStackInSlot(0).isEmpty()) {
            // 放入指定物品后，增长的发电量变为当前发电量的 1%（至少 1，避免低产量时停止增长）
            increase = Math.max(1, output / 100);
        }
        nextIncrease = increase;
    }

    /**
     * 给充电槽中的物品充电
     */
    private void chargeChargeSlot() {
        if (energy <= 0) {
            return;
        }
        ItemStack stack = chargeSlot.getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }
        IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (storage != null && storage.canReceive()) {
            charge(stack, storage);
        }
    }

    /**
     * 给站在机器上方实体的所有槽位中可充电物品充电（玩家物品栏/存储栏/装备栏均覆盖）
     */
    private void chargeEntitiesAbove() {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        List<LivingEntity> entityList = level.getEntitiesOfClass(LivingEntity.class, new AABB(getBlockPos().relative(Direction.UP)));
        for (LivingEntity livingEntity : entityList) {
            Iterable<ItemStack> slots = livingEntity.getAllSlots();
            for (ItemStack stack : slots) {
                if (energy <= 0) {
                    return;
                }
                IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
                if (storage != null && storage.canReceive()) {
                    charge(stack, storage);
                }
            }
        }
    }

    /**
     * 给机器上方容器中的可充电物品充电（箱子、漏斗等带物品栏的方块实体）
     */
    private void chargeContainersAbove() {
        Level level = getLevel();
        if (level == null || energy <= 0) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(getBlockPos().relative(Direction.UP));
        if (blockEntity == null) {
            return;
        }
        var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, blockEntity.getBlockPos(), Direction.DOWN);
        if (handler == null) {
            return;
        }
        for (int i = 0; i < handler.getSlots(); i++) {
            if (energy <= 0) {
                return;
            }
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (storage != null && storage.canReceive()) {
                charge(stack, storage);
                // 修改了容器内物品的能量数据，标记容器已改变以便落盘/同步
                blockEntity.setChanged();
            }
        }
    }

    /**
     * 向可充电物品传入能量并扣减自身电量
     */
    private void charge(ItemStack stack, IEnergyStorage storage) {
        int maxOutput = Tool.suitInt(energy);
        int result = storage.receiveEnergy(maxOutput, false);
        if (result < 0) {
            result = 0;
        }
        if (result > maxOutput) {
            result = maxOutput;
        }
        if (result > 0) {
            energy -= result;
        }
    }

    /**
     * 六面输电（跳过被禁用的面），轮询索引实现负载均衡；重复传电次数生效
     */
    private void outputToSides() {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        Direction[] directions = Direction.values();
        for (int rep = 0; rep < transferRepeat && energy > 0; rep++) {
            for (int i = 0; i < directions.length; i++) {
                if (energy <= 0) {
                    return;
                }
                findIndex = (findIndex + 1) % directions.length;
                Direction direction = directions[findIndex];
                if (!isTransferEnabled(direction)) {
                    continue;
                }
                BlockPos pos = getBlockPos().relative(direction);
                IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction.getOpposite());
                // 目标暴露标准能量能力且可接收时才标准注入；否则（如 Flux 拒收设备）交由反射绕过处理
                if (storage != null && storage.canReceive()) {
                    charge(ItemStack.EMPTY, storage);
                }
                // 标准注入后仍有多余能量（或目标拒收、容量受限）时，反射补满其内部能量
                if (energy > 0 && Config.FE_BYPASS_ENABLED.get()) {
                    long consumed = EnergyBypass.tryRefill(level.getBlockEntity(pos), direction.getOpposite(), storage, energy);
                    if (consumed > 0) {
                        energy -= consumed;
                    }
                }
            }
        }
    }

    /**
     * 无线充电每 tick 处理：
     * 每 tick 扫描一片（整个区域按全部方块线性均分为 wirelessInterval*20 片，游标记录上次位置下次继续），
     * 然后按重复传电次数遍历已记录位置尝试输电。
     */
    private void wirelessTick() {
        // 扫描推进：每 tick 扫一片
        scanWirelessSlice();
        // 每 tick 遍历所有已记录目标，按重复传电次数循环输电
        wirelessTransfer();
    }

    /**
     * 扫描当前分片：把整个扫描区域按全部方块线性均分为 wirelessInterval*20 片，每 tick 扫一片。
     * 线性索引 index = (x-minX) + (z-minZ)*width + (y-minY)*width*width，scanCursor 记录上次扫描到的位置，下次继续。
     */
    private void scanWirelessSlice() {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        int range = Tool.normalizeWirelessRange(wirelessRange);
        // range=1 -> half=0（1x1 区块），range=3 -> half=1（3x3），range=5 -> half=2（5x5）
        int half = range >> 1;
        BlockPos pos = getBlockPos();
        int originX = pos.getX() >> 4 << 4;
        int originZ = pos.getZ() >> 4 << 4;
        int minX = originX - half * 16;
        int minZ = originZ - half * 16;
        int width = range * 16;
        int minY = level.getMinBuildHeight();
        long layerSize = (long) width * width; // 单个 Y 层的方块数
        long volume = layerSize * level.getHeight(); // 整个 3D 扫描体积

        // 缩小范围后清理超出当前区域的目标（无线目标按区块列判断）
        wirelessTargets.keySet().removeIf(bp -> {
            int bx = bp.getX() >> 4;
            int bz = bp.getZ() >> 4;
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            return Math.abs(bx - cx) > half || Math.abs(bz - cz) > half;
        });

        // 配置为秒，每秒 20 tick：完整扫描周期 = wirelessInterval 秒，共 wirelessInterval*20 个分片
        long slices = Math.max(1L, (long) Math.max(1, wirelessInterval) * 20);
        long sliceSize = Math.max(1L, (volume + slices - 1) / slices);
        long start = scanCursor;
        long end = Math.min(volume, start + sliceSize);
        scanLinearRange(level, minX, minZ, width, minY, layerSize, start, end);
        // 游标推进：扫完整个体积后回到 0 重新开始
        scanCursor = end >= volume ? 0 : end;
    }

    /**
     * 扫描线性索引落在 [from, to) 内的方块实体并刷新无线目标
     */
    private void scanLinearRange(Level level, int minX, int minZ, int width, int minY, long layerSize, long from, long to) {
        if (from >= to) {
            return;
        }
        BlockPos pos = getBlockPos();
        int cMinX = minX >> 4;
        int cMaxX = (minX + width - 1) >> 4;
        int cMinZ = minZ >> 4;
        int cMaxZ = (minZ + width - 1) >> 4;
        for (int cx = cMinX; cx <= cMaxX; cx++) {
            for (int cz = cMinZ; cz <= cMaxZ; cz++) {
                // 只处理已加载的区块，避免强制生成区块
                if (!level.isLoaded(new BlockPos(cx << 4, pos.getY(), cz << 4))) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos bp = entry.getKey();
                    if (bp.equals(worldPosition)) {
                        continue;
                    }
                    // 计算该方块实体的线性索引，仅处理落在本分片范围内的
                    long idx = (bp.getX() - minX) + (long) (bp.getZ() - minZ) * width + (long) (bp.getY() - minY) * layerSize;
                    if (idx < from || idx >= to) {
                        continue;
                    }
                    refreshWirelessTarget(bp, entry.getValue());
                }
            }
        }
    }

    /**
     * 扫描目标的所有面，找到第一个可输入能量的面截止并缓存该面；没有可接收面则移除旧记录
     */
    private void refreshWirelessTarget(BlockPos bp, BlockEntity target) {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        for (Direction dir : Direction.values()) {
            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, bp, dir);
            if (storage != null && storage.canReceive()) {
                wirelessTargets.put(bp.immutable(), dir);
                return;
            }
        }
        wirelessTargets.remove(bp);
    }

    /**
     * 每 tick 遍历已记录目标，按重复传电次数循环向其中输入电量（使用扫描时缓存的面）
     */
    private void wirelessTransfer() {
        Level level = getLevel();
        if (level == null || energy <= 0) {
            return;
        }
        int repeat = Math.max(1, transferRepeat);
        for (int rep = 0; rep < repeat && energy > 0; rep++) {
            for (Map.Entry<BlockPos, Direction> entry : wirelessTargets.entrySet()) {
                if (energy <= 0) {
                    return;
                }
                BlockPos targetPos = entry.getKey();
                // 只处理已加载区块，避免强制加载未加载区块
                if (!level.isLoaded(targetPos)) {
                    continue;
                }
                IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, entry.getValue());
                if (storage != null && storage.canReceive()) {
                    charge(ItemStack.EMPTY, storage);
                }
                // 标准注入后仍有多余能量（或目标拒收、容量受限）时，反射补满其内部能量
                if (energy > 0 && Config.FE_BYPASS_ENABLED.get()) {
                    long consumed = EnergyBypass.tryRefill(level.getBlockEntity(targetPos), entry.getValue(), storage, energy);
                    if (consumed > 0) {
                        energy -= consumed;
                    }
                }
            }
        }
    }

    @Override
    @Nonnull
    public Component getDisplayName() {
        return Component.translatable("block.autoresource.energy_generator_fe");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new EnergyGeneratorMenu(id, inv, worldPosition);
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt, @Nonnull HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        nbt.putLong("output", output);
        nbt.putLong("energy", energy);
        nbt.putLong("tickCount", tickCount);
        nbt.putLong("nextIncrease", nextIncrease);
        nbt.putBoolean("wirelessOn", wirelessOn);
        nbt.putInt("wirelessInterval", wirelessInterval);
        nbt.putInt("wirelessRange", wirelessRange);
        nbt.putInt("transferRepeat", transferRepeat);
        saveTransferFaces(nbt);
        saveOutputEnabled(nbt);
        nbt.put("starSlot", starSlot.serializeNBT(provider));
        nbt.put("chargeSlot", chargeSlot.serializeNBT(provider));
    }

    @Override
    public void loadAdditional(@Nonnull CompoundTag nbt, @Nonnull HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        if (nbt.contains("output", Tag.TAG_LONG)) {
            output = Tool.suit(nbt.getLong("output"));
        }
        if (nbt.contains("energy", Tag.TAG_LONG)) {
            energy = Tool.suit(nbt.getLong("energy"));
        }
        if (nbt.contains("tickCount", Tag.TAG_LONG)) {
            tickCount = Tool.suit(nbt.getLong("tickCount"));
        }
        if (nbt.contains("nextIncrease", Tag.TAG_LONG)) {
            nextIncrease = Tool.suit(nbt.getLong("nextIncrease"));
        } else if (nbt.contains("beaconIncrease", Tag.TAG_LONG)) {
            // 兼容旧存档字段名
            nextIncrease = Tool.suit(nbt.getLong("beaconIncrease"));
        }
        if (nbt.contains("wirelessOn", Tag.TAG_BYTE)) {
            wirelessOn = nbt.getBoolean("wirelessOn");
        }
        if (nbt.contains("wirelessInterval", Tag.TAG_INT)) {
            wirelessInterval = Math.max(1, nbt.getInt("wirelessInterval"));
        }
        if (nbt.contains("wirelessRange", Tag.TAG_INT)) {
            wirelessRange = Math.max(1, nbt.getInt("wirelessRange"));
        }
        if (nbt.contains("transferRepeat", Tag.TAG_INT)) {
            transferRepeat = Math.max(1, nbt.getInt("transferRepeat"));
        }
        loadTransferFaces(nbt);
        loadOutputEnabled(nbt);
        if (nbt.contains("starSlot", Tag.TAG_COMPOUND)) {
            starSlot.deserializeNBT(provider, nbt.getCompound("starSlot"));
        }
        if (nbt.contains("chargeSlot", Tag.TAG_COMPOUND)) {
            chargeSlot.deserializeNBT(provider, nbt.getCompound("chargeSlot"));
        }
    }

    /**
     * 物品 DataComponent 编码格式：
     * output,energy,tickCount,nextIncrease,wirelessOn,wirelessInterval,wirelessRange,transferRepeat,
     * transferDown,transferUp,transferNorth,transferSouth,transferWest,transferEast,starItemId
     */
    @Override
    protected void collectImplicitComponents(@Nonnull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(Registration.BLOCK_DATA.get(),
                output + "," + energy + "," + tickCount + "," + nextIncrease + "," + (wirelessOn ? 1 : 0) + ","
                        + wirelessInterval + "," + wirelessRange + "," + transferRepeat + ","
                        + (transferDown ? 1 : 0) + "," + (transferUp ? 1 : 0) + "," + (transferNorth ? 1 : 0) + ","
                        + (transferSouth ? 1 : 0) + "," + (transferWest ? 1 : 0) + "," + (transferEast ? 1 : 0) + ","
                        + starSlot.getStackInSlot(0).getItemHolder().getRegisteredName());
    }

    @Override
    protected void applyImplicitComponents(@Nonnull BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        String blockData = input.getOrDefault(Registration.BLOCK_DATA.get(), "");
        if (blockData.isEmpty()) {
            return;
        }
        String[] dataArray = blockData.split(",");
        output = Tool.parseLong(dataArray, 0);
        energy = Tool.parseLong(dataArray, 1);
        tickCount = Tool.parseLong(dataArray, 2);
        nextIncrease = Tool.parseLong(dataArray, 3);
        wirelessOn = Tool.parseInt(dataArray, 4) == 1;
        wirelessInterval = Math.max(1, Tool.parseInt(dataArray, 5));
        wirelessRange = Math.max(1, Tool.parseInt(dataArray, 6));
        transferRepeat = Math.max(1, Tool.parseInt(dataArray, 7));
        transferDown = Tool.parseInt(dataArray, 8) == 1;
        transferUp = Tool.parseInt(dataArray, 9) == 1;
        transferNorth = Tool.parseInt(dataArray, 10) == 1;
        transferSouth = Tool.parseInt(dataArray, 11) == 1;
        transferWest = Tool.parseInt(dataArray, 12) == 1;
        transferEast = Tool.parseInt(dataArray, 13) == 1;
        String starItemId = Tool.parseString(dataArray, 14);
        if (!starItemId.isEmpty()) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(starItemId)), 1);
            if (!stack.isEmpty()) {
                starSlot.setStackInSlot(0, stack);
            }
        }
    }
}
