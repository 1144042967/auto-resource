package cn.sd.jrz.autoresource.entities;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.energy.EnergyConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnergyGeneratorEntity extends BlockEntity implements ICapabilityProvider {
    private final LazyOptional<EnergyConnection> fecOptional = LazyOptional.of(() -> new EnergyConnection(this));
    public final DataConfig config;
    public long output;
    public long energy = 0;
    public int tickCount = 0;

    public EnergyGeneratorEntity(BlockPos pos, BlockState state, DataConfig config) {
        super(config.getEntityType(), pos, state);
        this.config = config;
        this.output = config.min;
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        return capability == ForgeCapabilities.ENERGY ? fecOptional.cast() : super.getCapability(capability, direction);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putLong("output", output);
        nbt.putLong("energy", energy);
        nbt.putLong("tickCount", tickCount);
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("output", Tag.TAG_LONG)) {
            output = nbt.getLong("output");
        }
        if (nbt.contains("energy", Tag.TAG_LONG)) {
            energy = nbt.getLong("energy");
        }
        if (nbt.contains("tickCount", Tag.TAG_LONG)) {
            tickCount = nbt.getInt("tickCount");
        }
    }
}
