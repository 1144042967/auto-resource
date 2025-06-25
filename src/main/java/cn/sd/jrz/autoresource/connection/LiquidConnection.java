package cn.sd.jrz.autoresource.connection;

import cn.sd.jrz.autoresource.entities.LiquidGeneratorEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

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
    public @NotNull FluidStack getFluidInTank(int tank) {
        stack.setAmount((int) (owner.liquid / 1000));
        return stack;
    }

    @Override
    public int fill(FluidStack fluidStack, IFluidHandler.FluidAction fluidAction) {
        return 0;
    }

    @Override
    public @NotNull FluidStack drain(int amount, IFluidHandler.FluidAction fluidAction) {
        if (owner.liquid / 1000 <= 0 || amount <= 0) {
            return new FluidStack(owner.config.getFluid(), 0);
        }
        int ret = (int) Math.min(owner.liquid / 1000, amount);
        if (fluidAction.execute()) {
            owner.liquid -= ret * 1000L;
            owner.setChanged();
        }
        return new FluidStack(owner.config.getFluid(), ret);
    }

    @Override
    public @NotNull FluidStack drain(FluidStack fluidStack, IFluidHandler.FluidAction fluidAction) {
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
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return false;
    }
}
