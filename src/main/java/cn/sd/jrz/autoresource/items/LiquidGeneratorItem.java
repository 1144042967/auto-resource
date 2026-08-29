package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.DataConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.NotNull;
import java.util.List;

public class LiquidGeneratorItem extends BlockItem {
    private final DataConfig config;

    public LiquidGeneratorItem(Block block, DataConfig config) {
        super(block, new Properties().stacksTo(1).fireResistant());
        this.config = config;
    }

    /**
     * 物品名称使用机器主题色
     */
    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return super.getName(stack).copy().withStyle(config.getThemeColor());
    }

    /**
     * tooltip 仅在客户端渲染调用，且只使用 common 类，无需环境隔离注解
     * 1.21.1：Level 参数被替换为 Item.TooltipContext；BlockEntityTag 通过 DataComponents 读取
     */
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
        double output = config.getMin() / 1000D;
        long liquid = 0;
        long tickCount = 0;
        long second = config.getSecond();
        long step = config.getStep();
        // 1.21.1：BlockEntityTag 改为组件存储（DataComponents.BLOCK_ENTITY_DATA），不存在则为空
        CompoundTag tag = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (tag != null && !tag.isEmpty()) {
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
        double percent = (int) (tickCount / 20.00D / second * 10000D) / 100.00D;
        // 数值行使用机器主题色
        tooltip.add(Component.translatable("item.autoresource.liquid_generator.tooltip.liquid", liquid).withStyle(config.getThemeColor()));
        tooltip.add(Component.translatable("item.autoresource.liquid_generator.tooltip.output", output).withStyle(config.getThemeColor()));
        if (output < config.getMax()) {
            tooltip.add(Component.translatable("item.autoresource.liquid_generator.tooltip.growth", percent).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("item.autoresource.liquid_generator.tooltip.growth_max").withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("item.autoresource.liquid_generator.tooltip.step", second, step / 1000D).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.autoresource.liquid_generator.tooltip.set_block").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.autoresource.liquid_generator.tooltip.tip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
