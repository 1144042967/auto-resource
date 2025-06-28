package cn.sd.jrz.autoresource.connection;

import cn.sd.jrz.autoresource.entities.EnergyGeneratorEntity;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyConnection implements IEnergyStorage {
    private final EnergyGeneratorEntity owner;

    public EnergyConnection(EnergyGeneratorEntity owner) {
        this.owner = owner;
    }

    @Override
    public int getEnergyStored() {
        return Tool.suitInt(owner.energy);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int maxOutput = Tool.suitInt(owner.energy);
        if (maxOutput <= 0 || maxExtract <= 0) {
            return 0;
        }
        int ret = Math.min(maxOutput, maxExtract);
        if (!simulate) {
            owner.energy -= ret;
            owner.setChanged();
        }
        return ret;
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
