package cn.sd.jrz.autoresource.entities;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.connection.BlockConnection;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockGeneratorEntity extends TileEntity implements ICapabilityProvider, ITickableTileEntity {
    private final LazyOptional<BlockConnection> fecOptional = LazyOptional.of(() -> new BlockConnection(this));
    public final DataConfig config;
    public long output;
    public long block = 0;
    public long tickCount = 0;
    private final Direction[] directions = Direction.values();
    private int findIndex = 0;

    public BlockGeneratorEntity(DataConfig config) {
        super(config.getEntityType());
        this.config = config;
        this.output = config.getMin();
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? fecOptional.cast() : super.getCapability(capability, direction);
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
        this.block = Tool.suit(this.block + this.output);
        if (this.block / 1000 <= 0) {
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
            IItemHandler handler = entity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite()).resolve().orElse(null);
            if (handler == null) {
                continue;
            }
            int maxOutput = Tool.suitInt(this.block / 1000);
            ItemStack result = ItemHandlerHelper.insertItemStacked(handler, new ItemStack(config.getBlock(), maxOutput), false);
            int count = result.getCount();
            if (count < 0) {
                count = 0;
            }
            if (count > maxOutput) {
                count = maxOutput;
            }
            this.block -= (maxOutput - count) * 1000L;
            if (this.block / 1000 <= 0) {
                break;
            }
        }
        if (level.hasNeighborSignal(blockPos) && this.block >= 1000 && this.tickCount % 5 == 0) {
            BlockPos pos = blockPos.relative(Direction.DOWN);
            if (level.getBlockState(pos).getBlock() == Blocks.AIR && level.setBlock(pos, config.getBlock().defaultBlockState(), 3)) {
                this.block -= 1000;
            }
        }
        this.setChanged();
    }

    @Override
    @Nonnull
    public CompoundNBT save(@Nonnull CompoundNBT nbt) {
        nbt = super.save(nbt);
        nbt.putLong("output", output);
        nbt.putLong("block", block);
        nbt.putLong("tickCount", tickCount);
        return nbt;
    }

    @Override
    public void load(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.load(state, nbt);
        if (nbt.contains("output")) {
            output = Tool.suit(nbt.getLong("output"));
        }
        if (nbt.contains("block")) {
            block = Tool.suit(nbt.getLong("block"));
        }
        if (nbt.contains("tickCount")) {
            tickCount = Tool.suit(nbt.getLong("tickCount"));
        }
    }
}
