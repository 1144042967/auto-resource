package cn.sd.jrz.autoresource.energy;

import net.minecraftforge.energy.IEnergyStorage;

public class EnergyConnection implements IEnergyStorage {
    private final GeneratorEntity owner;

    public EnergyConnection(GeneratorEntity owner) {
        this.owner = owner;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        double energyExtracted = Math.min(owner.getEnergy(), maxExtract);
        if (!simulate) {
            owner.setEnergy(owner.getEnergy() - energyExtracted);
        }
        owner.setChanged();
        return (int) energyExtracted;
    }

    @Override
    public int getEnergyStored() {
        return (int) owner.getEnergy();
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) owner.getOutput();
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
