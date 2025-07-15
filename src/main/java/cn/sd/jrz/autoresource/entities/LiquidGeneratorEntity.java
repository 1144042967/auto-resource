package cn.sd.jrz.autoresource.entities;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.connection.LiquidConnection;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LiquidGeneratorEntity extends TileEntity implements ICapabilityProvider, ITickableTileEntity {
    private final LazyOptional<LiquidConnection> fecOptional = LazyOptional.of(() -> new LiquidConnection(this));
    public final DataConfig config;
    public long output;
    public long liquid = 0;
    public long tickCount = 0;
    private final Direction[] directions = Direction.values();
    private int findIndex = 0;

    public LiquidGeneratorEntity(DataConfig config) {
        super(config.getEntityType());
        this.config = config;
        this.output = config.getMin();
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY ? fecOptional.cast() : super.getCapability(capability, direction);
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
        //计算产量
        this.tickCount = Tool.suit(this.tickCount + 1);
        if (this.tickCount / 20 >= this.config.getSecond()) {
            this.tickCount = 0;
            this.output = Math.min(this.config.getMax(), Tool.suit(this.output + this.config.getStep()));
        }
        this.liquid = Tool.suit(this.liquid + this.output);
        if (this.liquid <= 0) {
            this.setChanged();
            return;
        }
        //传输
        BlockPos blockPos = this.getBlockPos();
        for (int i = 0; i < directions.length; i++) {
            findIndex = (findIndex + 1) % directions.length;
            Direction direction = directions[findIndex];
            BlockPos pos = blockPos.relative(direction);
            TileEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                continue;
            }
            int maxOutput = Tool.suitInt(this.liquid);
            IFluidHandler storage = entity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction.getOpposite()).resolve().filter(handler -> {
                int tanks = handler.getTanks();
                for (int tank = 0; tank < tanks; tank++) {
                    if (handler.isFluidValid(tank, new FluidStack(this.config.getFluid(), maxOutput))) {
                        return true;
                    }
                }
                return false;
            }).orElse(null);
            if (storage == null) {
                continue;
            }
            int result = storage.fill(new FluidStack(this.config.getFluid(), maxOutput), IFluidHandler.FluidAction.EXECUTE);
            if (result < 0) {
                result = 0;
            }
            if (result > maxOutput) {
                result = maxOutput;
            }
            this.liquid -= result;
            if (this.liquid <= 0) {
                break;
            }
        }
        if (level.hasNeighborSignal(blockPos) && this.liquid >= 1000 && this.tickCount % 5 == 0) {
            BlockPos pos = blockPos.relative(Direction.DOWN);
            if (level.getBlockState(pos).getBlock() == Blocks.AIR && level.setBlock(pos, config.getBlock().defaultBlockState(), 3)) {
                this.liquid -= 1000;
            }
        }
        this.setChanged();
    }

    @Override
    @Nonnull
    public CompoundNBT save(@Nonnull CompoundNBT nbt) {
        nbt = super.save(nbt);
        nbt.putLong("output", output);
        nbt.putLong("liquid", liquid);
        nbt.putLong("tickCount", tickCount);
        return nbt;
    }

    @Override
    public void load(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.load(state, nbt);
        if (nbt.contains("output")) {
            output = Tool.suit(nbt.getLong("output"));
        }
        if (nbt.contains("liquid")) {
            liquid = Tool.suit(nbt.getLong("liquid"));
        }
        if (nbt.contains("tickCount")) {
            tickCount = Tool.suit(nbt.getLong("tickCount"));
        }
    }
}
