package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.DataConfig;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class EnergyGeneratorItem extends BlockItem {
    private final DataConfig config;

    public EnergyGeneratorItem(Block block, DataConfig config) {
        super(block, new Properties().stacksTo(1).fireResistant().tab(ItemManager.CREATIVE_MODE_TABS));
        this.config = config;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        double output = config.getMin();
        long energy = 0;
        long tickCount = 0;
        long second = config.getSecond();
        long step = config.getStep();
        if (stack.hasTag()) {
            CompoundNBT tag = stack.getTagElement("BlockEntityTag");
            if (tag != null) {
                if (tag.contains("output")) {
                    output = tag.getLong("output");
                }
                if (tag.contains("energy")) {
                    energy = tag.getLong("energy");
                }
                if (tag.contains("tickCount")) {
                    tickCount = tag.getLong("tickCount");
                }
            }
        }
        double percent = (int) (tickCount / 20.00D / second * 10000) / 100.00D;
        tooltip.add(new TranslationTextComponent("item.autoresource.energy_generator.tooltip.energy", energy));
        tooltip.add(new TranslationTextComponent("item.autoresource.energy_generator.tooltip.output", output));
        if (output < config.getMax()) {
            tooltip.add(new TranslationTextComponent("item.autoresource.energy_generator.tooltip.growth", percent));
        } else {
            tooltip.add(new TranslationTextComponent("item.autoresource.energy_generator.tooltip.growth_max"));
        }
        tooltip.add(new TranslationTextComponent("item.autoresource.energy_generator.tooltip.step", second, step));
        tooltip.add(new TranslationTextComponent("item.autoresource.energy_generator.tooltip.group_faster"));
        tooltip.add(new TranslationTextComponent("item.autoresource.energy_generator.tooltip.tip"));
    }
}