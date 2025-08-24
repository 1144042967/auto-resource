package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.setup.Registration;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class EnergyGeneratorItem extends BlockItem {
    private final DataConfig config;

    public EnergyGeneratorItem(Block block, Properties properties, DataConfig config) {
        super(block, properties);
        this.config = config;
    }

    @SuppressWarnings("deprecation")
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, @Nonnull TooltipDisplay display, @Nonnull Consumer<Component> consumer, @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, context, display, consumer, flag);
        double output = config.getMin();
        long energy = 0;
        long tickCount = 0;
        long second = config.getSecond();
        long step = config.getStep();
        String blockData = stack.getOrDefault(Registration.BLOCK_DATA.get(), "");
        if (!blockData.isEmpty()) {
            String[] dataArray = blockData.split(",");
            output = Tool.suit(dataArray[0]);
            energy = Tool.suit(dataArray[1]);
            tickCount = Tool.suit(dataArray[2]);
        }
        double percent = (int) (tickCount / 20.00D / second * 10000) / 100.00D;
        consumer.accept(Component.translatable("item.autoresource.energy_generator.tooltip.energy", energy));
        consumer.accept(Component.translatable("item.autoresource.energy_generator.tooltip.output", output));
        if (output < config.getMax()) {
            consumer.accept(Component.translatable("item.autoresource.energy_generator.tooltip.growth", percent));
        } else {
            consumer.accept(Component.translatable("item.autoresource.energy_generator.tooltip.growth_max"));
        }
        consumer.accept(Component.translatable("item.autoresource.energy_generator.tooltip.step", second, step));
        consumer.accept(Component.translatable("item.autoresource.energy_generator.tooltip.group_faster"));
        consumer.accept(Component.translatable("item.autoresource.energy_generator.tooltip.tip"));
    }
}