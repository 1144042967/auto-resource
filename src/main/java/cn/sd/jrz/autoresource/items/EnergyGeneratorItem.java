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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.List;

public class EnergyGeneratorItem extends BlockItem {
    private final DataConfig config;

    public EnergyGeneratorItem(Block block, DataConfig config) {
        super(block, new Properties().stacksTo(1).fireResistant().component(Registration.BLOCK_DATA.get(), ""));
        this.config = config;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
        double output = config.getMin();
        long energy = 0;
        long tickCount = 0;
        long nextIncrease = config.getStep();
        boolean wirelessOn = false;
        long second = config.getSecond();
        long step = config.getStep();
        String blockData = stack.getOrDefault(Registration.BLOCK_DATA.get(), "");
        if (!blockData.isEmpty()) {
            String[] dataArray = blockData.split(",");
            output = Tool.parseLong(dataArray, 0);
            energy = Tool.parseLong(dataArray, 1);
            tickCount = Tool.parseLong(dataArray, 2);
            nextIncrease = Tool.parseLong(dataArray, 3);
            wirelessOn = Tool.parseInt(dataArray, 4) == 1;
        }
        double percent = (int) (tickCount / 20.00D / second * 10000) / 100.00D;
        tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.energy", energy));
        tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.output", output));
        if (output >= config.getMax()) {
            tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.next_max"));
        } else {
            tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.next", nextIncrease));
        }
        if (output < config.getMax()) {
            tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.growth", percent));
        } else {
            tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.growth_max"));
        }
        tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.step", second, step));
        tooltip.add(Component.translatable(wirelessOn ? "item.autoresource.energy_generator.tooltip.wireless_on" : "item.autoresource.energy_generator.tooltip.wireless_off"));
        tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.group_faster"));
        tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.tip"));
    }
}
