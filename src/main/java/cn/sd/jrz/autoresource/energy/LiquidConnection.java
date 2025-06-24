package cn.sd.jrz.autoresource.energy;

import cn.sd.jrz.autoresource.entities.LiquidGeneratorEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

public class LiquidConnection implements IFluidTank {
    private final LiquidGeneratorEntity owner;

    public LiquidConnection(LiquidGeneratorEntity owner) {
        this.owner = owner;
    }

    @Override
    public @NotNull FluidStack getFluid() {
        return new FluidStack(owner.config.getFluid(), getFluidAmount());
    }

    @Override
    public int getFluidAmount() {
        return (int) (owner.liquid);
    }

    @Override
    public int getCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isFluidValid(FluidStack fluidStack) {
        return false;
    }

    @Override
    public int fill(FluidStack fluidStack, IFluidHandler.FluidAction fluidAction) {
        return 0;
    }

    @Override
    public @NotNull FluidStack drain(int maxExtract, IFluidHandler.FluidAction fluidAction) {
        long liquidDrain = Math.min(owner.liquid, maxExtract);
        if (fluidAction.execute()) {
            owner.liquid -= liquidDrain;
        }
        owner.setChanged();
        return new FluidStack(owner.config.getFluid(), (int) liquidDrain);
    }

    @Override
    public @NotNull FluidStack drain(FluidStack fluidStack, IFluidHandler.FluidAction fluidAction) {
        if (fluidStack.getFluid() == owner.config.getFluid()) {
            return drain(fluidStack.getAmount(), fluidAction);
        } else {
            return FluidStack.EMPTY;
        }
    }
}
