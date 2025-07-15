package cn.sd.jrz.autoresource.entities;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.connection.EnergyConnection;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnergyGeneratorEntity extends TileEntity implements ICapabilityProvider, ITickableTileEntity {
    private final LazyOptional<EnergyConnection> fecOptional = LazyOptional.of(() -> new EnergyConnection(this));
    public final DataConfig config;
    public long output;
    public long energy = 0;
    public long tickCount = 0;
    public long beaconIncrease = 0;
    private final Direction[] directions = Direction.values();
    private int findIndex = 0;

    public EnergyGeneratorEntity(DataConfig config) {
        super(config.getEntityType());
        this.config = config;
        this.output = config.getMin();
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        return capability == CapabilityEnergy.ENERGY ? fecOptional.cast() : super.getCapability(capability, direction);
    }

    @Override
    public void tick() {
        World level = this.level;
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            return;
        }
        BlockPos blockPos = this.getBlockPos();
        this.tickCount = Tool.suit(this.tickCount + 1);
        if (this.tickCount / 20 >= this.config.getSecond()) {
            this.tickCount = 0;
            this.beaconIncrease = this.config.getStep();
            if (level.hasNeighborSignal(blockPos)) {
                BlockPos pos = blockPos.relative(Direction.DOWN);
                if (level.getBlockState(pos).getBlock() == Blocks.BEACON) {
                    this.beaconIncrease = Tool.suit((long) (this.output / 10000.0 * config.getBeaconStep()) + this.config.getStep());
                }
            }
            this.output = Math.min(this.config.getMax(), Tool.suit(this.output + this.beaconIncrease));
        }
        this.energy = Tool.suit(this.energy + this.output);
        for (int i = 0; i < directions.length; i++) {
            findIndex = (findIndex + 1) % directions.length;
            Direction direction = directions[findIndex];
            BlockPos pos = blockPos.relative(direction);
            TileEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                continue;
            }
            IEnergyStorage storage = entity.getCapability(CapabilityEnergy.ENERGY, direction.getOpposite()).resolve().filter(IEnergyStorage::canReceive).orElse(null);
            if (storage == null) {
                continue;
            }
            int maxOutput = Tool.suitInt(this.energy);
            int result = storage.receiveEnergy(maxOutput, false);
            if (result < 0) {
                result = 0;
            }
            if (result > maxOutput) {
                result = maxOutput;
            }
            this.energy -= result;
            if (this.energy <= 0) {
                break;
            }
        }
        this.setChanged();
    }

    @Override
    @Nonnull
    public CompoundNBT save(@Nonnull CompoundNBT nbt) {
        nbt = super.save(nbt);
        nbt.putLong("output", output);
        nbt.putLong("energy", energy);
        nbt.putLong("tickCount", tickCount);
        nbt.putLong("beaconIncrease", beaconIncrease);
        return nbt;
    }

    @Override
    public void load(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.load(state, nbt);
        if (nbt.contains("output")) {
            output = Tool.suit(nbt.getLong("output"));
        }
        if (nbt.contains("energy")) {
            energy = Tool.suit(nbt.getLong("energy"));
        }
        if (nbt.contains("tickCount")) {
            tickCount = Tool.suit(nbt.getLong("tickCount"));
        }
        if (nbt.contains("beaconIncrease")) {
            tickCount = Tool.suit(nbt.getLong("beaconIncrease"));
        }
    }
}
