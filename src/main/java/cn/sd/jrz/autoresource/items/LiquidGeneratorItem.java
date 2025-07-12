package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.setup.Registration;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LiquidGeneratorItem extends BlockItem {
    private final DataConfig config;

    public LiquidGeneratorItem(Block block, DataConfig config) {
        super(block, new Properties().stacksTo(1).fireResistant().component(Registration.BLOCK_DATA.get(), ""));
        this.config = config;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
        double output = config.getMin() / 1000D;
        long liquid = 0;
        long tickCount = 0;
        long second = config.getSecond();
        long step = config.getStep();
        String blockData = stack.getOrDefault(Registration.BLOCK_DATA.get(), "");
        if (!blockData.isEmpty()) {
            String[] dataArray = blockData.split(",");
            output = Tool.suit(dataArray[0]) / 1000D;
            liquid = Tool.suit(dataArray[1]) / 1000;
            tickCount = Tool.suit(dataArray[2]);
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