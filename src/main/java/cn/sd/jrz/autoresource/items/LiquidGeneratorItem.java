package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.DataConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

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
     */
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext ctx, @NotNull TooltipDisplay display, @NotNull Consumer<Component> tooltip, @NotNull TooltipFlag flagIn) {
        super.appendHoverText(stack, ctx, display, tooltip, flagIn);
        double output = config.getMin() / 1000D;
        long liquid = 0;
        long tickCount = 0;
        long second = config.getSecond();
        long step = config.getStep();
        TypedEntityData<BlockEntityType<?>> blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = blockEntityData == null ? null : blockEntityData.getUnsafe();
        if (tag != null) {
            if (tag.contains("output")) {
                output = tag.getLongOr("output", (long) output) / 1000D;
            }
            if (tag.contains("liquid")) {
                liquid = tag.getLongOr("liquid", liquid) / 1000;
            }
            if (tag.contains("tickCount")) {
                tickCount = tag.getLongOr("tickCount", tickCount);
            }
        }
        double percent = (int) (tickCount / 20.00D / second * 10000D) / 100.00D;
        // 数值行使用机器主题色
        tooltip.accept(Component.translatable("item.autoresource.liquid_generator.tooltip.liquid", liquid).withStyle(config.getThemeColor()));
        tooltip.accept(Component.translatable("item.autoresource.liquid_generator.tooltip.output", output).withStyle(config.getThemeColor()));
        if (output < config.getMax()) {
            tooltip.accept(Component.translatable("item.autoresource.liquid_generator.tooltip.growth", percent).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.accept(Component.translatable("item.autoresource.liquid_generator.tooltip.growth_max").withStyle(ChatFormatting.GOLD));
        }
        tooltip.accept(Component.translatable("item.autoresource.liquid_generator.tooltip.step", second, step / 1000D).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.autoresource.liquid_generator.tooltip.set_block").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.autoresource.liquid_generator.tooltip.tip").withStyle(ChatFormatting.DARK_GRAY));
    }
}