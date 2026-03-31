package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.EnergyGeneratorEntity;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("DuplicatedCode")
public class EnergyGeneratorBlock extends Block implements EntityBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnergyGeneratorBlock.class);
    private final DataConfig config;
    private final Direction[] directions = Direction.values();
    private int findIndex = 0;

    public EnergyGeneratorBlock(Properties properties, DataConfig config) {
        super(properties);
        this.config = config;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new EnergyGeneratorEntity(pos, state, config);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, _, _, tile) -> {
            try {
                tick(l, tile);
            } catch (Throwable e) {
                LOGGER.error("EnergyGeneratorBlock.getTicker error", e);
            }
        };
    }

    private <T extends BlockEntity> void tick(Level level, T tile) {
        if (level.isClientSide()) {
            return;
        }
        if (!(tile instanceof EnergyGeneratorEntity generator)) {
            return;
        }
        BlockPos blockPos = generator.getBlockPos();
        generator.tickCount = Tool.suit(generator.tickCount + 1);
        if (generator.tickCount / 20 >= generator.config.getSecond()) {
            generator.tickCount = 0;
            generator.beaconIncrease = generator.config.getStep();
            if (level.hasNeighborSignal(blockPos)) {
                BlockPos pos = blockPos.relative(Direction.DOWN);
                if (level.getBlockState(pos).getBlock() == Blocks.BEACON) {
                    generator.beaconIncrease = Tool.suit((long) (generator.output / 10000.0 * config.getBeaconStep()) + generator.config.getStep());
                }
            }
            generator.output = Math.min(generator.config.getMax(), Tool.suit(generator.output + generator.beaconIncrease));
        }
        generator.energy = Tool.suit(generator.energy + generator.output);
        //给实体输电
        List<Player> playerList = level.getEntitiesOfClass(Player.class, new AABB(blockPos.relative(Direction.UP)));
        for (Player player : playerList) {
            Inventory inventory = player.getInventory();
            for (ItemStack stack : inventory) {
                EnergyHandler handler = stack.getCapability(Capabilities.Energy.ITEM, null);
                if (handler == null || handler.getCapacityAsLong() <= handler.getAmountAsLong()) {
                    continue;
                }
                int maxOutput = Tool.suitInt(generator.energy);
                try (var tx = Transaction.open(null)) {
                    int result = handler.insert(maxOutput, tx);
                    if (result < 0) {
                        result = 0;
                    }
                    if (result > maxOutput) {
                        result = maxOutput;
                    }
                    generator.energy -= result;
                    if (generator.energy <= 0) {
                        break;
                    }
                }
            }
        }
        //给其他面输电
        for (int i = 0; i < directions.length; i++) {
            findIndex = (findIndex + 1) % directions.length;
            Direction direction = directions[findIndex];
            BlockPos pos = blockPos.relative(direction);
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                continue;
            }
            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, direction.getOpposite());
            if (handler == null || handler.getCapacityAsLong() <= handler.getAmountAsLong()) {
                continue;
            }
            int maxOutput = Tool.suitInt(generator.energy);
            try (var tx = Transaction.open(null)) {
                int result = handler.insert(maxOutput, tx);
                if (result < 0) {
                    result = 0;
                }
                if (result > maxOutput) {
                    result = maxOutput;
                }
                generator.energy -= result;
                if (generator.energy <= 0) {
                    break;
                }
            }
        }
        generator.setChanged();
    }

    @Override
    public @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        return use(level, pos, player);
    }

    @Override
    protected @Nonnull InteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        return use(level, pos, player);
    }

    private InteractionResult use(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        EnergyGeneratorEntity generator = (EnergyGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return InteractionResult.FAIL;
        }
        long energy = generator.energy;
        long output = generator.output;
        double percent = (int) (generator.tickCount / 20.00D / generator.config.getSecond() * 10000D) / 100.00D;
        long increase = generator.beaconIncrease;
        if (output < generator.config.getMax()) {
            player.sendOverlayMessage(Component.translatable("screen.autoresource.energy_generator.message", energy, output, percent, increase));
        } else {
            player.sendOverlayMessage(Component.translatable("screen.autoresource.energy_generator.message_max", energy, output));
        }
        return InteractionResult.SUCCESS;
    }
}
