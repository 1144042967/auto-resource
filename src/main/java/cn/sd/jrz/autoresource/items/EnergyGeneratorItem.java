package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.setup.ARRegistration;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.ChatFormatting;
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

    /** 物品名称使用机器主题色 */
    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(config.getThemeColor());
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
        // 数值行使用机器主题色
        tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.energy", energy).withStyle(config.getThemeColor()));
        tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.output", output).withStyle(config.getThemeColor()));
        if (output >= config.getMax()) {
            tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.next_max").withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.next", nextIncrease).withStyle(config.getThemeColor()));
        }
        if (output < config.getMax()) {
            tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.growth", percent).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.growth_max").withStyle(ChatFormatting.GOLD));
        }
        tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.step", second, step).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(wirelessOn ? "item.autoresource.energy_generator.tooltip.wireless_on" : "item.autoresource.energy_generator.tooltip.wireless_off")
                .withStyle(wirelessOn ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.group_faster").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.autoresource.energy_generator.tooltip.tip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
