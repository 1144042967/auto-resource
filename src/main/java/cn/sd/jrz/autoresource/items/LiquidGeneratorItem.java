package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.DataConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
        double output = config.getMin() / 1000D;
        long liquid = 0;
        long tickCount = 0;
        long second = config.getSecond();
        long step = config.getStep();
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTagElement("BlockEntityTag");
            if (tag != null) {
                if (tag.contains("output", Tag.TAG_LONG)) {
                    output = tag.getLong("output") / 1000D;
                }
                if (tag.contains("liquid", Tag.TAG_LONG)) {
                    liquid = tag.getLong("liquid") / 1000;
                }
                if (tag.contains("tickCount", Tag.TAG_LONG)) {
                    tickCount = tag.getLong("tickCount");
                }
            }
        }
        double percent = (int) (tickCount / 20.00D / second * 10000D) / 100.00D;
        tooltip.add(Component.translatable("item.autoresource.liquid_generator.tooltip.liquid", liquid));
        tooltip.add(Component.translatable("item.autoresource.liquid_generator.tooltip.output", output));
        if (output < config.getMax()) {
            tooltip.add(Component.translatable("item.autoresource.liquid_generator.tooltip.growth", percent));
        } else {
            tooltip.add(Component.translatable("item.autoresource.liquid_generator.tooltip.growth_max"));
        }
        tooltip.add(Component.translatable("item.autoresource.liquid_generator.tooltip.step", second, step / 1000D));
        tooltip.add(Component.translatable("item.autoresource.liquid_generator.tooltip.set_block"));
        tooltip.add(Component.translatable("item.autoresource.liquid_generator.tooltip.tip"));
    }
}