package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.DataConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

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
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level worldIn, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        double output = config.getMin();
        long energy = 0;
        long tickCount = 0;
        long nextIncrease = config.getStep();
        boolean wirelessOn = false;
        long second = config.getSecond();
        long step = config.getStep();
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTagElement("BlockEntityTag");
            if (tag != null) {
                if (tag.contains("output", Tag.TAG_LONG)) {
                    output = tag.getLong("output");
                }
                if (tag.contains("energy", Tag.TAG_LONG)) {
                    energy = tag.getLong("energy");
                }
                if (tag.contains("tickCount", Tag.TAG_LONG)) {
                    tickCount = tag.getLong("tickCount");
                }
                if (tag.contains("nextIncrease", Tag.TAG_LONG)) {
                    nextIncrease = tag.getLong("nextIncrease");
                } else if (tag.contains("beaconIncrease", Tag.TAG_LONG)) {
                    // 兼容旧字段名
                    nextIncrease = tag.getLong("beaconIncrease");
                }
                if (tag.contains("wirelessOn", Tag.TAG_BYTE)) {
                    wirelessOn = tag.getBoolean("wirelessOn");
                }
            }
        }
        double percent = (int) (tickCount / 20.00D / second * 10000) / 100.00D;
        // 数值行使用机器主题色
        tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.energy", energy).withStyle(config.getThemeColor()));
        tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.output", output).withStyle(config.getThemeColor()));
        if (output >= config.getMax()) {
            tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.next_max").withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.next", nextIncrease).withStyle(config.getThemeColor()));
        }
        if (output < config.getMax()) {
            tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.growth", percent).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.growth_max").withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.step", second, step).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(wirelessOn ? "item.autoresource.energy_generator.tooltip.wireless_on" : "item.autoresource.energy_generator.tooltip.wireless_off")
                .withStyle(wirelessOn ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.group_faster").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.tip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
