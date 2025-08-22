package cn.sd.jrz.autoresource.connection;

import cn.sd.jrz.autoresource.entities.LiquidGeneratorEntity;
import cn.sd.jrz.autoresource.util.Tool;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;

public class LiquidConnection implements IFluidHandler {
    private final LiquidGeneratorEntity owner;
    private final FluidStack stack;

    public LiquidConnection(LiquidGeneratorEntity owner) {
        this.owner = owner;
        this.stack = new FluidStack(owner.config.getFluid(), 0);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @Nonnull FluidStack getFluidInTank(int tank) {
        stack.setAmount(Tool.suitInt(owner.liquid));
        return stack;
    }

    @Override
    public int fill(FluidStack fluidStack, IFluidHandler.FluidAction fluidAction) {
        return 0;
    }

    @Override
    public @Nonnull FluidStack drain(int amount, IFluidHandler.FluidAction fluidAction) {
        int maxOutput = Tool.suitInt(owner.liquid);
        if (maxOutput <= 0 || amount <= 0) {
            return new FluidStack(owner.config.getFluid(), 0);
        }
        int ret = Math.min(maxOutput, amount);
        if (fluidAction.execute()) {
            owner.liquid -= ret;
            owner.setChanged();
        }
        return new FluidStack(owner.config.getFluid(), ret);
    }

    @Override
    public @Nonnull FluidStack drain(FluidStack fluidStack, IFluidHandler.FluidAction fluidAction) {
        if (fluidStack.getFluid() == owner.config.getFluid()) {
            return drain(fluidStack.getAmount(), fluidAction);
        } else {
            return new FluidStack(owner.config.getFluid(), 0);
        }
    }

    @Override
    public int getTankCapacity(int tank) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
        return false;
    }
}
