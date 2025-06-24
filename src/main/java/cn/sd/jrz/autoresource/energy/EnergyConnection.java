package cn.sd.jrz.autoresource.energy;

import cn.sd.jrz.autoresource.entities.EnergyGeneratorEntity;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyConnection implements IEnergyStorage {
    private final EnergyGeneratorEntity owner;

    public EnergyConnection(EnergyGeneratorEntity owner) {
        this.owner = owner;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        long energyExtracted = Math.min(owner.energy, maxExtract);
        if (!simulate) {
            owner.energy -= energyExtracted;
        }
        owner.setChanged();
        return (int) energyExtracted;
    }

    @Override
    public int getEnergyStored() {
        return (int) owner.energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return false;
    }
}
