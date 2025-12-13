package cn.sd.jrz.autoresource.entities;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.connection.LiquidConnection;
import cn.sd.jrz.autoresource.setup.ARRegistration;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("DuplicatedCode")
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
    protected void applyImplicitComponents(@Nonnull DataComponentGetter input) {
        super.applyImplicitComponents(input);
        String blockData = input.getOrDefault(ARRegistration.BLOCK_DATA.get(), "");
        if (blockData.isEmpty()) {
            return;
        }
        String[] dataArray = blockData.split(",");
        output = Tool.suit(dataArray[0]);
        liquid = Tool.suit(dataArray[1]);
        tickCount = Tool.suit(dataArray[2]);
    }

    @Override
    protected void collectImplicitComponents(@Nonnull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(ARRegistration.BLOCK_DATA.get(), output + "," + liquid + "," + tickCount);
    }

    @Override
    public void saveAdditional(@Nonnull ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.putLong("output", output);
        valueOutput.putLong("liquid", liquid);
        valueOutput.putLong("tickCount", tickCount);
    }

    @Override
    public void loadAdditional(@Nonnull ValueInput valueInput) {
        super.loadAdditional(valueInput);
        valueInput.getLong("output").ifPresent(it -> this.output = Tool.suit(it));
        valueInput.getLong("liquid").ifPresent(it -> this.liquid = Tool.suit(it));
        valueInput.getLong("tickCount").ifPresent(it -> this.tickCount = Tool.suit(it));
    }
}
