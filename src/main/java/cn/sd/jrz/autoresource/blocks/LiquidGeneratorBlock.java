package cn.sd.jrz.autoresource.blocks;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.entities.LiquidGeneratorEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("deprecation")
public class LiquidGeneratorBlock extends Block implements ITileEntityProvider {
    private final DataConfig config;

    public LiquidGeneratorBlock(Properties properties, DataConfig config) {
        super(properties);
        this.config = config;
    }

    @Nullable
    @Override
    public TileEntity newBlockEntity(@Nonnull IBlockReader reader) {
        return new LiquidGeneratorEntity(config);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull ActionResultType use(@Nonnull BlockState state, World level, @Nonnull BlockPos pos, @Nonnull PlayerEntity player, @Nonnull Hand hand, @Nonnull BlockRayTraceResult result) {
        if (level.isClientSide) {
            return ActionResultType.SUCCESS;
        }
        LiquidGeneratorEntity generator = (LiquidGeneratorEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return ActionResultType.FAIL;
        }
        if (generator.liquid >= 1000 && useBucket(player, generator)) {
            return ActionResultType.SUCCESS;
        }
        long liquid = generator.liquid / 1000;
        double output = generator.output / 1000D;
        double percent = (int) (generator.tickCount / 20.00 / generator.config.getSecond() * 10000) / 100.00;
        if (output < generator.config.getMax()) {
            player.sendMessage(new TranslationTextComponent("screen.autoresource.liquid_generator.message", liquid, output, percent), Util.NIL_UUID);
        } else {
            player.sendMessage(new TranslationTextComponent("screen.autoresource.liquid_generator.message_max", liquid, output), Util.NIL_UUID);
        }
        return ActionResultType.SUCCESS;
    }

    private boolean useBucket(PlayerEntity player, LiquidGeneratorEntity generator) {
        EquipmentSlotType type;
        if (player.getMainHandItem().getItem() == Items.BUCKET) {
            type = EquipmentSlotType.MAINHAND;
        } else if (player.getOffhandItem().getItem() == Items.BUCKET) {
            type = EquipmentSlotType.OFFHAND;
        } else {
            return false;
        }
        int count = player.getItemBySlot(type).getCount();
        if (count == 1) {
            player.setItemSlot(type, getBucket());
        } else if (count > 1) {
            player.setItemSlot(type, new ItemStack(Items.BUCKET, count - 1));
            player.addItem(getBucket());
        }
        generator.liquid -= 1000L;
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
