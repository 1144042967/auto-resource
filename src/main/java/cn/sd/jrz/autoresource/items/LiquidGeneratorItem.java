package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.DataConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class LiquidGeneratorItem extends BlockItem {
    private final DataConfig config;

    public LiquidGeneratorItem(Block block, DataConfig config) {
        super(block, new Properties().stacksTo(1).fireResistant());
        this.config = config;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level worldIn, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        double energy = 0;
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTagElement("BlockEntityTag");
            if (tag != null && tag.contains("energy")) {
                energy = tag.getDouble("energy");
            }
        }
        double maxEnergy = config.getStorageMaxEnergy();
        double percent = ((int) (energy / maxEnergy * 10000)) / 100.00;
        tooltip.add(Component.translatable("item.exponentialpower.storage.tooltip.stored_percent", percent));
        tooltip.add(Component.translatable("item.exponentialpower.storage.tooltip.stored_current", energy));
        tooltip.add(Component.translatable("item.exponentialpower.storage.tooltip.stored_max", maxEnergy));
    }
}