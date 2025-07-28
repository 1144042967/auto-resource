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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class BlockGeneratorItem extends BlockItem {
    private final DataConfig config;

    public BlockGeneratorItem(Block block, Properties properties, DataConfig config) {
        super(block, properties);
        this.config = config;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, @Nonnull TooltipDisplay tooltipDisplay, @Nonnull Consumer<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flagIn);
        //noinspection resource
        Level level = context.level();
        if (level == null || !level.isClientSide) {
            return;
        }
        double output = config.getMin() / 1000D;
        long block = 0;
        long tickCount = 0;
        long second = config.getSecond();
        long step = config.getStep();
        String blockData = stack.getOrDefault(Registration.BLOCK_DATA.get(), "");
        if (!blockData.isEmpty()) {
            String[] dataArray = blockData.split(",");
            output = Tool.suit(dataArray[0]) / 1000D;
            block = Tool.suit(dataArray[1]) / 1000;
            tickCount = Tool.suit(dataArray[2]);
        }
        double percent = (int) (tickCount / 20.00D / second * 10000) / 100.00D;
        tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.block", block));
        tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.output", output));
        if (output < config.getMax()) {
            tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.growth", percent));
        } else {
            tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.growth_max"));
        }
        tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.step", second, step / 1000D));
        tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.set_block"));
        tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.tip"));
    }
}