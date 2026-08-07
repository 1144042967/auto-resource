package cn.sd.jrz.autoresource.entities;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.menu.EnergyGeneratorMenu;
import cn.sd.jrz.autoresource.setup.ARRegistration;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * FE 发电机实体（26.x 适配）。
 * <p>
 * 新传输 API：使用 {@link EnergyHandler} 替代旧版 IEnergyStorage；物品能力通过 {@link ItemAccess}。<br>
 * 数据持久化：使用 26.x 的 {@link ValueInput}/{@link ValueOutput}。
 */
public class EnergyGeneratorEntity extends BlockEntity implements MenuProvider {
    public final DataConfig config;

    // 核心数据
    public long output;
    public long energy = 0;
    public long tickCount = 0;
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

    // 六面输电开关（逐台保存，可在 GUI 修改，默认全启用）
    public boolean transferDown = true;
    public boolean transferUp = true;
    public boolean transferNorth = true;
    public boolean transferSouth = true;
    public boolean transferWest = true;
    public boolean transferEast = true;

    // 加速增长槽位（放入配置指定物品后增长量变为当前发电量的 1%），只能放 1 个
    public final ItemStackHandler starSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            Item star = config.getStarItem();
            return star != null && stack.is(star);
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
            // 26.x 通过 Capability 检测接口判断可充电物品
            EnergyHandler handler = ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM);
            return handler != null;
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

    // 六面输电轮询索引
    private int findIndex = 0;

    public EnergyGeneratorEntity(BlockPos pos, BlockState state, DataConfig config) {
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
        setChanged();
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
        EnergyHandler handler = ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM);
        if (handler != null) {
            chargeStack(stack, handler);
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
        java.util.List<LivingEntity> entityList = level.getEntitiesOfClass(LivingEntity.class, new AABB(getBlockPos().relative(Direction.UP)));
        for (LivingEntity livingEntity : entityList) {
            // 装备槽（含主手/副手/盔甲，覆盖 26.x 的 EquipmentSlot 系统）
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (energy <= 0) {
                    return;
                }
                ItemStack stack = livingEntity.getItemBySlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                EnergyHandler handler = ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM);
                if (handler != null) {
                    chargeStack(stack, handler);
                }
            }
            // 玩家物品栏（物品栏/存储栏，与原 getAllSlots 覆盖范围一致）
            if (livingEntity instanceof Player player) {
                Inventory inv = player.getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (energy <= 0) {
                        return;
                    }
                    ItemStack stack = inv.getItem(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    EnergyHandler handler = ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM);
                    if (handler != null) {
                        chargeStack(stack, handler);
                    }
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
        net.neoforged.neoforge.transfer.ResourceHandler<ItemResource> handler =
                level.getCapability(Capabilities.Item.BLOCK, blockEntity.getBlockPos(), Direction.DOWN);
        if (handler == null) {
            return;
        }
        for (int i = 0; i < handler.size(); i++) {
            if (energy <= 0) {
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
            if (stack.isEmpty()) {
                continue;
            }
            EnergyHandler energyHandler = ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM);
            if (energyHandler != null) {
                chargeStack(stack, energyHandler);
                // 修改了容器内物品的能量数据，标记容器已改变以便落盘/同步
                blockEntity.setChanged();
            }
        }
    }

    /**
     * 向可充电物品传入能量并扣减自身电量（26.x 传输 API）
     */
    private void chargeStack(ItemStack stack, EnergyHandler handler) {
        if (energy <= 0) {
            return;
        }
        int maxOutput = Tool.suitInt(energy);
        if (maxOutput <= 0) {
            return;
        }
        try (net.neoforged.neoforge.transfer.transaction.Transaction tx = net.neoforged.neoforge.transfer.transaction.Transaction.open(null)) {
            int result = handler.insert(maxOutput, tx);
            if (result < 0) {
                result = 0;
            }
            if (result > maxOutput) {
                result = maxOutput;
            }
            tx.commit();
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
                EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, direction.getOpposite());
                if (handler == null) {
                    continue;
                }
                chargeStack(ItemStack.EMPTY, handler);
            }
        }
    }

    /**
     * 无线充电每 tick 处理：
     * 每 tick 扫描一片（整个区域按全部方块线性均分为 wirelessInterval*20 片，游标记录上次位置下次继续），
     * 然后按重复传电次数遍历已记录位置尝试输电。
     */
    private void wirelessTick() {
        scanWirelessSlice();
        wirelessTransfer();
    }

    /**
     * 扫描当前分片：把整个扫描区域按全部方块线性均分为 wirelessInterval*20 片，每 tick 扫一片。
     */
    private void scanWirelessSlice() {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        int range = Tool.normalizeWirelessRange(wirelessRange);
        int half = range >> 1;
        BlockPos pos = getBlockPos();
        int originX = pos.getX() >> 4 << 4;
        int originZ = pos.getZ() >> 4 << 4;
        int minX = originX - half * 16;
        int minZ = originZ - half * 16;
        int width = range * 16;
        int minY = level.dimensionType().minY();
        long layerSize = (long) width * width;
        long volume = layerSize * level.getHeight();

        // 缩小范围后清理超出当前区域的目标
        wirelessTargets.keySet().removeIf(bp -> {
            int bx = bp.getX() >> 4;
            int bz = bp.getZ() >> 4;
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            return Math.abs(bx - cx) > half || Math.abs(bz - cz) > half;
        });

        long slices = Math.max(1L, (long) Math.max(1, wirelessInterval) * 20);
        long sliceSize = Math.max(1L, (volume + slices - 1) / slices);
        long start = scanCursor;
        long end = Math.min(volume, start + sliceSize);
        scanLinearRange(level, minX, minZ, width, minY, layerSize, start, end);
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
            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, bp, dir);
            if (handler != null) {
                wirelessTargets.put(bp.immutable(), dir);
                return;
            }
        }
        wirelessTargets.remove(bp);
    }

    /**
     * 每 tick 遍历已记录目标，按重复传电次数循环向其中输入电量
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
                if (!level.isLoaded(targetPos)) {
                    continue;
                }
                EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, targetPos, entry.getValue());
                if (handler == null) {
                    continue;
                }
                chargeStack(ItemStack.EMPTY, handler);
            }
        }
    }

    /**
     * 指定面是否允许输电
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
        return Component.translatable("block.autoresource.energy_generator_fe");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new EnergyGeneratorMenu(id, inv, worldPosition);
    }

    @Override
    public void saveAdditional(@Nonnull ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.putLong("output", output);
        valueOutput.putLong("energy", energy);
        valueOutput.putLong("tickCount", tickCount);
        valueOutput.putLong("nextIncrease", nextIncrease);
        valueOutput.putBoolean("wirelessOn", wirelessOn);
        valueOutput.putInt("wirelessInterval", wirelessInterval);
        valueOutput.putInt("wirelessRange", wirelessRange);
        valueOutput.putInt("transferRepeat", transferRepeat);
        valueOutput.putBoolean("transferDown", transferDown);
        valueOutput.putBoolean("transferUp", transferUp);
        valueOutput.putBoolean("transferNorth", transferNorth);
        valueOutput.putBoolean("transferSouth", transferSouth);
        valueOutput.putBoolean("transferWest", transferWest);
        valueOutput.putBoolean("transferEast", transferEast);
        starSlot.serialize(valueOutput.child("starSlot"));
        chargeSlot.serialize(valueOutput.child("chargeSlot"));
    }

    @Override
    public void loadAdditional(@Nonnull ValueInput valueInput) {
        super.loadAdditional(valueInput);
        valueInput.getLong("output").ifPresent(it -> this.output = Tool.suit(it));
        valueInput.getLong("energy").ifPresent(it -> this.energy = Tool.suit(it));
        valueInput.getLong("tickCount").ifPresent(it -> this.tickCount = Tool.suit(it));
        // 兼容旧存档字段名 beaconIncrease（1.21.1 时期迁移前的字段）
        this.nextIncrease = valueInput.getLongOr("nextIncrease", valueInput.getLongOr("beaconIncrease", this.nextIncrease));
        this.wirelessOn = valueInput.getBooleanOr("wirelessOn", this.wirelessOn);
        this.wirelessInterval = Math.max(1, valueInput.getIntOr("wirelessInterval", this.wirelessInterval));
        this.wirelessRange = Math.max(1, valueInput.getIntOr("wirelessRange", this.wirelessRange));
        this.transferRepeat = Math.max(1, valueInput.getIntOr("transferRepeat", this.transferRepeat));
        this.transferDown = valueInput.getBooleanOr("transferDown", this.transferDown);
        this.transferUp = valueInput.getBooleanOr("transferUp", this.transferUp);
        this.transferNorth = valueInput.getBooleanOr("transferNorth", this.transferNorth);
        this.transferSouth = valueInput.getBooleanOr("transferSouth", this.transferSouth);
        this.transferWest = valueInput.getBooleanOr("transferWest", this.transferWest);
        this.transferEast = valueInput.getBooleanOr("transferEast", this.transferEast);
        starSlot.deserialize(valueInput.childOrEmpty("starSlot"));
        chargeSlot.deserialize(valueInput.childOrEmpty("chargeSlot"));
    }

    /**
     * 物品 DataComponent 编码格式：
     * output,energy,tickCount,nextIncrease,wirelessOn,wirelessInterval,wirelessRange,transferRepeat,
     * transferDown,transferUp,transferNorth,transferSouth,transferWest,transferEast,starItemId
     */
    @Override
    protected void collectImplicitComponents(@Nonnull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(ARRegistration.BLOCK_DATA.get(),
                output + "," + energy + "," + tickCount + "," + nextIncrease + "," + (wirelessOn ? 1 : 0) + ","
                        + wirelessInterval + "," + wirelessRange + "," + transferRepeat + ","
                        + (transferDown ? 1 : 0) + "," + (transferUp ? 1 : 0) + "," + (transferNorth ? 1 : 0) + ","
                        + (transferSouth ? 1 : 0) + "," + (transferWest ? 1 : 0) + "," + (transferEast ? 1 : 0) + ","
                        + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(starSlot.getStackInSlot(0).getItem()).toString());
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
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.tryParse(starItemId)), 1);
            if (!stack.isEmpty()) {
                starSlot.setStackInSlot(0, stack);
            }
        }
    }
}
