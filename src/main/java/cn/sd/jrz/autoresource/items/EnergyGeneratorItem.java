package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.setup.ARRegistration;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * FE 发电机物品。
 * <p>
 * 26.x 适配：tooltip 使用 26.x 新签名 {@link TooltipDisplay} + {@link Consumer}；构造时注册默认 {@code BLOCK_DATA} 组件。
 */
public class EnergyGeneratorItem extends BlockItem {
    private final DataConfig config;

    public EnergyGeneratorItem(Block block, Properties properties, DataConfig config) {
        super(block, properties.component(ARRegistration.BLOCK_DATA.get(), ""));
        this.config = config;
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, @Nonnull TooltipDisplay tooltipDisplay, @Nonnull Consumer<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flagIn);
        double output = config.getMin();
        long energy = 0;
        long tickCount = 0;
        long nextIncrease = config.getStep();
        boolean wirelessOn = false;
        long second = config.getSecond();
        long step = config.getStep();
        String blockData = stack.getOrDefault(ARRegistration.BLOCK_DATA.get(), "");
        if (!blockData.isEmpty()) {
            String[] dataArray = blockData.split(",");
            output = Tool.parseLong(dataArray, 0);
            energy = Tool.parseLong(dataArray, 1);
            tickCount = Tool.parseLong(dataArray, 2);
            nextIncrease = Tool.parseLong(dataArray, 3);
            wirelessOn = Tool.parseInt(dataArray, 4) == 1;
        }
        double percent = (int) (tickCount / 20.00D / second * 10000) / 100.00D;
        tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.energy", energy));
        tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.output", output));
        if (output >= config.getMax()) {
            tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.next_max"));
        } else {
            tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.next", nextIncrease));
        }
        if (output < config.getMax()) {
            tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.growth", percent));
        } else {
            tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.growth_max"));
        }
        tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.step", second, step));
        tooltip.accept(Component.translatable(wirelessOn ? "item.autoresource.energy_generator.tooltip.wireless_on" : "item.autoresource.energy_generator.tooltip.wireless_off"));
        tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.group_faster"));
        tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.tip"));
    }
}
