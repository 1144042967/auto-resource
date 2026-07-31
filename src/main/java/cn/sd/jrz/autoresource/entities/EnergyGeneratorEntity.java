package cn.sd.jrz.autoresource.entities;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.connection.EnergyConnection;
import cn.sd.jrz.autoresource.menu.EnergyGeneratorMenu;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * FE 发电机实体。
 * <p>
 * 负责：发电量自动增长、能量存储、上方实体充电、六面输电（可逐面禁用）、
 * 充电槽物品充电、指定物品加速增长（增长量变为当前发电量的 1%）以及无线充电。
 * 无线充电与输电面等参数均为每台发电机独立保存，可在 GUI 中修改。
 */
public class EnergyGeneratorEntity extends BlockEntity implements ICapabilityProvider, MenuProvider {
    private final LazyOptional<EnergyConnection> fecOptional = LazyOptional.of(() -> new EnergyConnection(this));
    public final DataConfig config;

    // 核心数据
    public long output;
    public long energy = 0;
    public long tickCount = 0;
    /** 下次增长的发电量（同时用于增长时实际增量） */
    public long nextIncrease = 0;

    // 无线充电开关（逐台保存）
    public boolean wirelessOn = false;
    public int wirelessTimer = 0;

    // 无线充电参数（逐台保存，可在 GUI 修改）
    public int wirelessInterval = 5;
    public int wirelessRange = 1;
    /** 重复传电次数，对相邻输电和无线输电都生效 */
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
            return stack.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::canReceive).orElse(false);
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

    /** 服务端每 tick 调用（由方块的 ticker 触发） */
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
        // 上方实体充电
        chargePlayersAbove();
        // 六面输电（重复传电次数同样生效）
        outputToSides();
        // 无线充电
        if (wirelessOn) {
            wirelessTimer++;
            if (wirelessTimer / 20 >= Math.max(1, wirelessInterval)) {
                wirelessTimer = 0;
                wirelessCharge();
            }
        }
        setChanged();
    }

    /** 计算下一次增长的发电量并保存到 nextIncrease（原信标功能已由加速槽代替） */
    private void updateNextIncrease() {
        long increase = config.getStep();
        if (!starSlot.getStackInSlot(0).isEmpty()) {
            // 放入指定物品后，增长的发电量变为当前发电量的 1%（至少 1，避免低产量时停止增长）
            increase = Math.max(1, output / 100);
        }
        nextIncrease = increase;
    }

    /** 给充电槽中的物品充电 */
    private void chargeChargeSlot() {
        if (energy <= 0) {
            return;
        }
        ItemStack stack = chargeSlot.getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }
        stack.getCapability(ForgeCapabilities.ENERGY).resolve().filter(IEnergyStorage::canReceive).ifPresent(storage -> {
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
                chargeSlot.setStackInSlot(0, stack);
            }
        });
    }

    /** 给站在机器上方玩家的物品栏中可充电物品充电 */
    private void chargePlayersAbove() {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        List<Player> playerList = level.getEntitiesOfClass(Player.class, new AABB(getBlockPos().relative(Direction.UP)));
        for (Player player : playerList) {
            Iterable<ItemStack> slots = player.getAllSlots();
            for (ItemStack stack : slots) {
                if (energy <= 0) {
                    return;
                }
                stack.getCapability(ForgeCapabilities.ENERGY).resolve().filter(IEnergyStorage::canReceive).ifPresent(storage -> {
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
                });
            }
        }
    }

    /** 六面输电（跳过被禁用的面），轮询索引实现负载均衡；重复传电次数生效 */
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
                BlockEntity entity = level.getBlockEntity(pos);
                if (entity == null) {
                    continue;
                }
                entity.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).resolve().filter(IEnergyStorage::canReceive).ifPresent(storage -> {
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
                });
            }
        }
    }

    /** 无线充电：扫描区块范围内已加载区块中的能量接收方并输电，重复传电次数生效 */
    private void wirelessCharge() {
        Level level = getLevel();
        if (level == null || energy <= 0) {
            return;
        }
        int range = Tool.normalizeWirelessRange(wirelessRange);
        int repeat = Math.max(1, transferRepeat);
        int chunkX = getBlockPos().getX() >> 4;
        int chunkZ = getBlockPos().getZ() >> 4;
        // range=1 -> half=0（1x1 区块），range=3 -> half=1（3x3），range=5 -> half=2（5x5）
        int half = range >> 1;
        for (int rep = 0; rep < repeat && energy > 0; rep++) {
            for (int cz = chunkZ - half; cz <= chunkZ + half && energy > 0; cz++) {
                for (int cx = chunkX - half; cx <= chunkX + half && energy > 0; cx++) {
                    // 只处理已加载的区块，避免强制生成区块
                    if (!level.isLoaded(new BlockPos(cx << 4, getBlockPos().getY(), cz << 4))) {
                        continue;
                    }
                    LevelChunk chunk = level.getChunk(cx, cz);
                    if (chunk == null) {
                        continue;
                    }
                    for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                        BlockEntity target = entry.getValue();
                        if (target == this || energy <= 0) {
                            continue;
                        }
                        target.getCapability(ForgeCapabilities.ENERGY, null).resolve().filter(IEnergyStorage::canReceive).ifPresent(storage -> {
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
                        });
                    }
                }
            }
        }
    }

    /** 指定面是否允许输电 */
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
        return capability == ForgeCapabilities.ENERGY ? fecOptional.cast() : super.getCapability(capability, direction);
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
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putLong("output", output);
        nbt.putLong("energy", energy);
        nbt.putLong("tickCount", tickCount);
        nbt.putLong("nextIncrease", nextIncrease);
        nbt.putBoolean("wirelessOn", wirelessOn);
        nbt.putInt("wirelessTimer", wirelessTimer);
        nbt.putInt("wirelessInterval", wirelessInterval);
        nbt.putInt("wirelessRange", wirelessRange);
        nbt.putInt("transferRepeat", transferRepeat);
        nbt.putBoolean("transferDown", transferDown);
        nbt.putBoolean("transferUp", transferUp);
        nbt.putBoolean("transferNorth", transferNorth);
        nbt.putBoolean("transferSouth", transferSouth);
        nbt.putBoolean("transferWest", transferWest);
        nbt.putBoolean("transferEast", transferEast);
        nbt.put("starSlot", starSlot.serializeNBT());
        nbt.put("chargeSlot", chargeSlot.serializeNBT());
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
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
        if (nbt.contains("wirelessTimer", Tag.TAG_INT)) {
            wirelessTimer = nbt.getInt("wirelessTimer");
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
        if (nbt.contains("starSlot", Tag.TAG_COMPOUND)) {
            starSlot.deserializeNBT(nbt.getCompound("starSlot"));
        }
        if (nbt.contains("chargeSlot", Tag.TAG_COMPOUND)) {
            chargeSlot.deserializeNBT(nbt.getCompound("chargeSlot"));
        }
    }
}
