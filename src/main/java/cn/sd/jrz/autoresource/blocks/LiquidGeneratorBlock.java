package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.blockentity.LiquidGeneratorEntity;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LiquidGeneratorBlock extends AbstractGeneratorBlock {

    public LiquidGeneratorBlock(Properties properties, DataConfig config) {
        super(properties, config);
    }

    @Override
    protected BlockEntity createEntity(BlockPos pos, BlockState state) {
        return new LiquidGeneratorEntity(pos, state, config);
    }

    @Override
    protected void tickEntity(Level level, BlockEntity tile) {
        if (level.isClientSide || !(tile instanceof LiquidGeneratorEntity generator)) {
            return;
        }
        generator.serverTick();
    }

    /**
     * 破坏时输入槽与输出槽中的物品掉落（并从 block_entity_data 中移除，避免重放后槽位重复）
     */
    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, @NotNull LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof LiquidGeneratorEntity entity) {
            ItemStack input = entity.inputSlot.getItem(0);
            if (!input.isEmpty()) {
                drops.add(input);
            }
            ItemStack output = entity.outputSlot.getItem(0);
            if (!output.isEmpty()) {
                drops.add(output);
            }
        }
        return drops;
    }

    /**
     * 输入/输出槽内容由 {@link #getDrops} 单独掉落，不随 block_entity_data 保留
     */
    @Override
    protected void removeDroppedSlots(CompoundTag tag) {
        tag.remove("inputSlot");
        tag.remove("outputSlot");
    }

    @Override
    public @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        LiquidGeneratorEntity generator = (LiquidGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return InteractionResult.FAIL;
        }
        // 手持空桶右击直接提取一桶液体
        if (generator.liquid >= 1000 && useBucket(player, generator)) {
            return InteractionResult.SUCCESS;
        }
        // 其他情况打开 GUI（实体自身是 MenuProvider，createMenu 内取本方块坐标）
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(generator);
        }
        return InteractionResult.SUCCESS;
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
        Item item = config.getFluid() == Fluids.WATER ? Items.WATER_BUCKET : Items.LAVA_BUCKET;
        return new ItemStack(item);
    }
}
