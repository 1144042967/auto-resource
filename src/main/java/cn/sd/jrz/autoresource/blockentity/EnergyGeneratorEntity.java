package cn.sd.jrz.autoresource.blockentity;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.capability.EnergyConnection;
import cn.sd.jrz.autoresource.menu.EnergyGeneratorMenu;
import cn.sd.jrz.autoresource.storage.MachineSlotStorage;
import cn.sd.jrz.autoresource.util.ItemEnergyIo;
import cn.sd.jrz.autoresource.util.Tool;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import team.reborn.energy.api.EnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * FE 发电机实体：发电量自动增长、能量存储、上方实体/容器物品充电、充电槽充电、
 * 六面输电（可逐面禁用）、指定物品加速增长、无线充电。各参数逐台独立保存。
 * Fabric 版能量交互全部经由 transfer API（teamreborn EnergyStorage），
 * Forge 版"反射补满第三方机器能量"在 Fabric 无目标 MOD，已随 EnergyBypass 移除——
 * 循环 insert 本身可将限速接收方逐步灌满。
 */
public class EnergyGeneratorEntity extends AbstractGeneratorEntity {

    // 核心数据
    public long energy = 0;
    /**
     * 下次增长的发电量（增长时实际增量）
     */
    public long nextIncrease = 0;

    // 无线充电（逐台保存，可在 GUI 修改）
    public boolean wirelessOn = false;
    /**
     * 无线扫描游标：记录上次扫描到的线性位置，下次从该位置继续
     */
    private long scanCursor = 0;
    /**
     * 已记录的支持电量接收的位置及其接收面
     */
    private final Map<BlockPos, Direction> wirelessTargets = new HashMap<>();
    public int wirelessInterval = 5;
    public int wirelessRange = 1;
    /**
     * 重复传电次数，对相邻输电和无线输电都生效
     */
    public int transferRepeat = 1;

    // 加速增长槽位（放入配置指定物品后增长量变为当前发电量的 1%），只能放 1 个
    public final MachineSlotStorage starSlot = new MachineSlotStorage(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(config.getStarItem());
        }
    }.setSlotLimit(0, 1);

    // 充电槽位（可放入可充电物品为其充电），每次充电 1 个
    public final MachineSlotStorage chargeSlot = new MachineSlotStorage(1) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
        }
    }.setValidator(ItemEnergyIo::canReceive).setSlotLimit(0, 1);

    // 对外能量连接（六面相同；由 TransferSetup 暴露）
    private final EnergyConnection energyConnection = new EnergyConnection(this);

    public EnergyGeneratorEntity(BlockPos pos, BlockState state, DataConfig config) {
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
        // 上方实体充电（玩家物品栏 + 装备栏全部覆盖）
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
     * 计算下一次增长的发电量；放入加速物品后变为当前发电量的 1%（至少 1）
     */
    private void updateNextIncrease() {
        long increase = config.getStep();
        if (!starSlot.getItem(0).isEmpty()) {
            increase = Math.max(1, output / 100);
        }
        nextIncrease = increase;
    }

    /**
     * 给充电槽中的物品充电（经槽位上下文，充入内容自动写回）
     */
    private void chargeChargeSlot() {
        if (energy <= 0) {
            return;
        }
        ItemStack stack = chargeSlot.getItem(0);
        if (stack.isEmpty()) {
            return;
        }
        SingleSlotStorage<ItemVariant> slotView = ContainerStorage.of(chargeSlot, null).getSlot(0);
        long received = ItemEnergyIo.receive(slotView, Tool.suitInt(energy));
        if (received > 0) {
            energy -= received;
            setChanged();
        }
    }

    /**
     * 给机器上方实体充电：玩家覆盖整个物品栏（背包+护甲+副手），生物覆盖手部与装备槽
     */
    private void chargeEntitiesAbove() {
        Level level = getLevel();
        if (level == null || energy <= 0) {
            return;
        }
        java.util.List<LivingEntity> entityList = level.getEntitiesOfClass(LivingEntity.class, new AABB(getBlockPos().relative(Direction.UP)));
        for (LivingEntity livingEntity : entityList) {
            if (energy <= 0) {
                return;
            }
            boolean charged = false;
            if (livingEntity instanceof Player player) {
                // 玩家：经 PlayerInventoryStorage 槽位视图逐格充电（变更自动回写原槽位）
                PlayerInventoryStorage storageView = PlayerInventoryStorage.of(player);
                for (int i = 0; i < storageView.getSlotCount() && energy > 0; i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    long received = ItemEnergyIo.receive(storageView.getSlot(i), Tool.suitInt(energy));
                    if (received > 0) {
                        energy -= received;
                        charged = true;
                    }
                }
                // 玩家背包物品被充电后需同步到客户端，否则物品能量条不刷新
                if (charged) {
                    player.inventoryMenu.broadcastChanges();
                }
            } else {
                // 生物：对六个装备/手持槽使用一次性上下文充电后显式写回
                for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                    if (energy <= 0) {
                        return;
                    }
                    ItemStack stack = livingEntity.getItemBySlot(equipmentSlot);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    ItemEnergyIo.Result result = ItemEnergyIo.receiveStandalone(stack, Tool.suitInt(energy));
                    if (result != null) {
                        livingEntity.setItemSlot(equipmentSlot, result.filled());
                        energy -= result.received();
                        charged = true;
                    }
                }
                if (charged) {
                    setChanged();
                }
            }
        }
    }

    /**
     * 给机器上方容器中的可充电物品充电（vanilla Container 路径，经槽位上下文回写）
     */
    private void chargeContainersAbove() {
        Level level = getLevel();
        if (level == null || energy <= 0) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(getBlockPos().relative(Direction.UP));
        if (!(blockEntity instanceof Container container)) {
            return;
        }
        ContainerStorage storageView = ContainerStorage.of(container, null);
        for (int i = 0; i < storageView.getSlotCount(); i++) {
            if (energy <= 0) {
                return;
            }
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            long received = ItemEnergyIo.receive(storageView.getSlot(i), Tool.suitInt(energy));
            if (received > 0) {
                energy -= received;
                // 修改了容器内物品的能量数据，标记容器已改变以便落盘/同步
                blockEntity.setChanged();
            }
        }
    }

    /**
     * 向指定位置的方块输出能量（transfer API 查找目标存储）
     *
     * @param pos           目标方块位置
     * @param fromDirection 目标接收能量的面（即本机对应面的对面）
     */
    private void outputTo(BlockPos pos, Direction fromDirection) {
        Level level = getLevel();
        if (level == null || energy <= 0) {
            return;
        }
        EnergyStorage target = EnergyStorage.SIDED.find(level, pos, fromDirection);
        if (target == null || !target.supportsInsertion()) {
            return;
        }
        int maxOutput = Tool.suitInt(energy);
        try (var txn = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            long received = target.insert(maxOutput, txn);
            if (received <= 0) {
                return;
            }
            txn.commit();
            energy -= received;
        } catch (Exception ignored) {
        }
    }

    /**
     * 六面输电（跳过被禁用的面），轮询索引负载均衡；重复传电次数生效
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
                // 正常输电（Find 目标不存在时返回 null 直接跳过）
                outputTo(pos, direction.getOpposite());
            }
        }
    }

    /**
     * 无线充电每 tick 处理：扫描一个分片，再按重复传电次数遍历已记录目标输电
     */
    private void wirelessTick() {
        // 无能量时无需扫描/输电
        if (energy <= 0) {
            return;
        }
        scanWirelessSlice();
        wirelessTransfer();
    }

    /**
     * 扫描当前分片：整个区域线性均分为 wirelessInterval*20 片，每 tick 扫一片，scanCursor 记录位置
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
        int minY = level.getMinY();
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
                if (!hasChunk(level, cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos bp = entry.getKey();
                    if (bp.equals(getBlockPos())) {
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
     * 区块是否已加载（不触发加载）
     */
    private static boolean hasChunk(Level level, int chunkX, int chunkZ) {
        return level.getChunkSource().hasChunk(chunkX, chunkZ);
    }

    /**
     * 扫描目标所有面，缓存第一个可接收能量的面；无可接收面则移除旧记录
     */
    private void refreshWirelessTarget(BlockPos bp, BlockEntity target) {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        for (Direction dir : Direction.values()) {
            EnergyStorage storage = EnergyStorage.SIDED.find(level, bp, dir);
            if (storage != null && storage.supportsInsertion()) {
                wirelessTargets.put(bp.immutable(), dir);
                return;
            }
        }
        wirelessTargets.remove(bp);
    }

    /**
     * 遍历已记录目标，按重复传电次数循环输电（使用扫描时缓存的面）
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
                if (!hasChunk(level, targetPos.getX() >> 4, targetPos.getZ() >> 4)) {
                    continue;
                }
                if (level.getBlockEntity(targetPos) == null) {
                    continue;
                }
                // 正常输电（无能量接入时静默跳过）
                outputTo(targetPos, entry.getValue());
            }
        }
    }

    @Override
    @NotNull
    public Component getDisplayName() {
        return Component.translatable("block.autoresource.energy_generator_fe");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
        return new EnergyGeneratorMenu(id, inv, getBlockPos());
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput out) {
        HolderLookup.Provider lookup = level != null ? level.registryAccess() : null;
        out.putLong("output", output);
        out.putLong("energy", energy);
        out.putLong("tickCount", tickCount);
        out.putLong("nextIncrease", nextIncrease);
        out.putBoolean("wirelessOn", wirelessOn);
        out.putInt("wirelessInterval", wirelessInterval);
        out.putInt("wirelessRange", wirelessRange);
        out.putInt("transferRepeat", transferRepeat);
        out.putBoolean("transferDown", transferDown);
        out.putBoolean("transferUp", transferUp);
        out.putBoolean("transferNorth", transferNorth);
        out.putBoolean("transferSouth", transferSouth);
        out.putBoolean("transferWest", transferWest);
        out.putBoolean("transferEast", transferEast);
        out.putBoolean("outputEnabled", outputEnabled);
        out.store("starSlot", CompoundTag.CODEC, starSlot.serializeNBT(lookup));
        out.store("chargeSlot", CompoundTag.CODEC, chargeSlot.serializeNBT(lookup));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        HolderLookup.Provider lookup = level != null ? level.registryAccess() : null;
        output = Tool.suit(in.getLongOr("output", output));
        energy = Tool.suit(in.getLongOr("energy", energy));
        tickCount = Tool.suit(in.getLongOr("tickCount", tickCount));
        // 兼容旧字段名 beaconIncrease
        nextIncrease = Tool.suit(in.getLongOr("nextIncrease", in.getLongOr("beaconIncrease", nextIncrease)));
        wirelessOn = in.getBooleanOr("wirelessOn", wirelessOn);
        wirelessInterval = Math.max(1, in.getIntOr("wirelessInterval", wirelessInterval));
        wirelessRange = Math.max(1, in.getIntOr("wirelessRange", wirelessRange));
        transferRepeat = Math.max(1, in.getIntOr("transferRepeat", transferRepeat));
        transferDown = in.getBooleanOr("transferDown", true);
        transferUp = in.getBooleanOr("transferUp", true);
        transferNorth = in.getBooleanOr("transferNorth", true);
        transferSouth = in.getBooleanOr("transferSouth", true);
        transferWest = in.getBooleanOr("transferWest", true);
        transferEast = in.getBooleanOr("transferEast", true);
        outputEnabled = in.getBooleanOr("outputEnabled", true);
        starSlot.deserializeNBT(in.read("starSlot", CompoundTag.CODEC).orElseGet(() -> starSlot.serializeNBT(lookup)), lookup);
        chargeSlot.deserializeNBT(in.read("chargeSlot", CompoundTag.CODEC).orElseGet(() -> chargeSlot.serializeNBT(lookup)), lookup);
    }
}
