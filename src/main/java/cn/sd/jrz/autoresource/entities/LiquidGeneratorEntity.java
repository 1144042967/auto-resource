package cn.sd.jrz.autoresource.entities;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.connection.LiquidConnection;
import cn.sd.jrz.autoresource.setup.Registration;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
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

public class LiquidGeneratorEntity extends BlockEntity implements ICapabilityProvider {
    private final LazyOptional<LiquidConnection> fecOptional = LazyOptional.of(() -> new LiquidConnection(this));
    public final DataConfig config;
    public long output;
    public long liquid = 0;
    public long tickCount = 0;

    public LiquidGeneratorEntity(BlockPos pos, BlockState state, DataConfig config) {
        super(config.getEntityType(), pos, state);
        this.config = config;
        this.output = config.getMin();
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        return capability == ForgeCapabilities.FLUID_HANDLER ? fecOptional.cast() : super.getCapability(capability, direction);
    }

    @Override
    protected void applyImplicitComponents(@NotNull DataComponentInput input) {
        super.applyImplicitComponents(input);
        String blockData = input.getOrDefault(Registration.BLOCK_DATA.get(), "");
        if (blockData.isEmpty()) {
            return;
        }
        String[] dataArray = blockData.split(",");
        output = Tool.suit(dataArray[0]);
        liquid = Tool.suit(dataArray[1]);
        tickCount = Tool.suit(dataArray[2]);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NotNull Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(Registration.BLOCK_DATA.get(), output + "," + liquid + "," + tickCount);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        nbt.putLong("output", output);
        nbt.putLong("liquid", liquid);
        nbt.putLong("tickCount", tickCount);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        if (nbt.contains("output", Tag.TAG_LONG)) {
            output = Tool.suit(nbt.getLong("output"));
        }
        if (nbt.contains("liquid", Tag.TAG_LONG)) {
            liquid = Tool.suit(nbt.getLong("liquid"));
        }
        if (nbt.contains("tickCount", Tag.TAG_LONG)) {
            tickCount = Tool.suit(nbt.getLong("tickCount"));
        }
    }
}
