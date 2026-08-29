package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.DataConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

public class EnergyGeneratorItem extends BlockItem {
    private final DataConfig config;

    public EnergyGeneratorItem(Block block, DataConfig config) {
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
    public void appendHoverText(@NotNull ItemStack stack, @NotNull net.minecraft.world.item.Item.TooltipContext ctx, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull Consumer<Component> tooltip, @NotNull TooltipFlag flagIn) {
        super.appendHoverText(stack, ctx, display, tooltip, flagIn);
        double output = config.getMin();
        long energy = 0;
        long tickCount = 0;
        long nextIncrease = config.getStep();
        boolean wirelessOn = false;
        long second = config.getSecond();
        long step = config.getStep();
        TypedEntityData<?> beData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (beData != null) {
            CompoundTag tag = beData.getUnsafe();
            if (tag.contains("output")) {
                output = tag.getLongOr("output", config.getMin());
            }
            if (tag.contains("energy")) {
                energy = tag.getLongOr("energy", 0);
            }
            if (tag.contains("tickCount")) {
                tickCount = tag.getLongOr("tickCount", 0);
            }
            if (tag.contains("nextIncrease")) {
                nextIncrease = tag.getLongOr("nextIncrease", config.getStep());
            } else if (tag.contains("beaconIncrease")) {
                // 兼容旧字段名
                nextIncrease = tag.getLongOr("beaconIncrease", config.getStep());
            }
            if (tag.contains("wirelessOn")) {
                wirelessOn = tag.getBooleanOr("wirelessOn", false);
            }
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
