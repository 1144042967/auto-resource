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

public class BlockGeneratorItem extends BlockItem {
    private final DataConfig config;

    public BlockGeneratorItem(Block block, DataConfig config) {
        super(block, new Properties().stacksTo(1).fireResistant().tab(ItemManager.CREATIVE_MODE_TABS));
        this.config = config;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        double output = config.getMin() / 1000D;
        long block = 0;
        long tickCount = 0;
        long second = config.getSecond();
        long step = config.getStep();
        if (stack.hasTag()) {
            CompoundNBT tag = stack.getTagElement("BlockEntityTag");
            if (tag != null) {
                if (tag.contains("output")) {
                    output = tag.getLong("output") / 1000D;
                }
                if (tag.contains("block")) {
                    block = tag.getLong("block") / 1000;
                }
                if (tag.contains("tickCount")) {
                    tickCount = tag.getLong("tickCount");
                }
            }
        }
        double percent = (int) (tickCount / 20.00D / second * 10000) / 100.00D;
        tooltip.add(new TranslationTextComponent("item.autoresource.block_generator.tooltip.block", block));
        tooltip.add(new TranslationTextComponent("item.autoresource.block_generator.tooltip.output", output));
        if (output < config.getMax()) {
            tooltip.add(new TranslationTextComponent("item.autoresource.block_generator.tooltip.growth", percent));
        } else {
            tooltip.add(new TranslationTextComponent("item.autoresource.block_generator.tooltip.growth_max"));
        }
        tooltip.add(new TranslationTextComponent("item.autoresource.block_generator.tooltip.step", second, step / 1000D));
        tooltip.add(new TranslationTextComponent("item.autoresource.block_generator.tooltip.set_block"));
        tooltip.add(new TranslationTextComponent("item.autoresource.block_generator.tooltip.tip"));
    }
}