package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.LiquidGeneratorEntity;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class LiquidGeneratorBlock extends Block implements EntityBlock {
    private final DataConfig config;

    public LiquidGeneratorBlock(Properties properties, DataConfig config) {
        super(properties);
        this.config = config;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new LiquidGeneratorEntity(pos, state, config);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, p, s, tile) -> tick(l, tile);
    }

    private <T extends BlockEntity> void tick(Level level, T tile) {
        if (level.isClientSide || !(tile instanceof LiquidGeneratorEntity generator)) {
            return;
        }
        generator.serverTick();
    }

    /**
     * 破坏时，输入槽与输出槽中的物品掉落
     */
    @Override
    public @Nonnull List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof LiquidGeneratorEntity entity) {
            ItemStack input = entity.inputSlot.getStackInSlot(0);
            if (!input.isEmpty()) {
                drops.add(input);
            }
            ItemStack output = entity.outputSlot.getStackInSlot(0);
            if (!output.isEmpty()) {
                drops.add(output);
            }
        }
        return drops;
    }

    @Override
    public @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        return use(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    protected @Nonnull ItemInteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        return use(level, pos, player) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
    }

    /**
     * 手持空桶右击先尝试提取一桶液体，其余情况打开 GUI
     */
    private boolean use(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) {
            return true;
        }
        LiquidGeneratorEntity generator = (LiquidGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return false;
        }
        // 保留：手持空桶右击直接提取一桶液体
        if (generator.liquid >= 1000 && useBucket(player, generator)) {
            return true;
        }
        // 其他情况打开 GUI
        player.openMenu(generator, pos);
        return true;
    }

    private boolean useBucket(Player player, LiquidGeneratorEntity generator) {
        EquipmentSlot type;
        if (player.getMainHandItem().getItem() == Items.BUCKET) {
            type = EquipmentSlot.MAINHAND;
        } else if (player.getOffhandItem().getItem() == Items.BUCKET) {
            type = EquipmentSlot.OFFHAND;
        } else {
            return false;
        }
        int count = player.getItemBySlot(type).getCount();
        if (count == 1) {
            player.setItemSlot(type, getBucket());
        } else if (count > 1) {
            player.setItemSlot(type, new ItemStack(Items.BUCKET, count - 1));
            Tool.takeItem(player, getBucket());
        }
        generator.liquid -= 1000L;
        generator.setChanged();
        return true;
    }

    private ItemStack getBucket() {
        if (config.getFluid() == Fluids.WATER) {
            return new ItemStack(Items.WATER_BUCKET);
        } else {
            return new ItemStack(Items.LAVA_BUCKET);
        }
    }
}
