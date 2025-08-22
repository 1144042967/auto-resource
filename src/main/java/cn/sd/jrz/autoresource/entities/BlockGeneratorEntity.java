package cn.sd.jrz.autoresource.entities;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

public class BlockGeneratorEntity extends BlockEntity {
    public final DataConfig config;
    public long output;
    public long block = 0;
    public long tickCount = 0;

    public BlockGeneratorEntity(BlockPos pos, BlockState state, DataConfig config) {
        super(config.getEntityType(), pos, state);
        this.config = config;
        this.output = config.getMin();
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putLong("output", output);
        nbt.putLong("block", block);
        nbt.putLong("tickCount", tickCount);
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("output", Tag.TAG_LONG)) {
            output = Tool.suit(nbt.getLong("output"));
        }
        if (nbt.contains("block", Tag.TAG_LONG)) {
            block = Tool.suit(nbt.getLong("block"));
        }
        if (nbt.contains("tickCount", Tag.TAG_LONG)) {
            tickCount = Tool.suit(nbt.getLong("tickCount"));
        }
    }
}
