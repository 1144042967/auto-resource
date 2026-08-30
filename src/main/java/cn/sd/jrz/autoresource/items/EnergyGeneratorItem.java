package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.DataConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class EnergyGeneratorItem extends BlockItem {
    private final DataConfig config;

    public EnergyGeneratorItem(Block block, DataConfig config, ResourceKey<Item> registryKey) {
        super(block, new Properties().setId(registryKey).stacksTo(1).fireResistant());
        this.config = config;
    }

    /**
     * 物品名称使用机器主题色
     */
    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        // 1.21.11：Item 默认名称键为 item.<id>（Properties 用 "item" 前缀），而语言文件使用 block.<id>，
        // 故显式改用方块翻译键，避免显示未翻译的键名
        return Component.translatable(this.getBlock().getDescriptionId()).copy().withStyle(config.getThemeColor());
    }

    /**
     * tooltip 仅在客户端渲染调用，且只使用 common 类，无需环境隔离注解
     * 1.21.11：appendHoverText 签名改为 TooltipDisplay + Consumer<Component>；
     * 组件读取改为 TypedEntityData（type + tag）
     */
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> tooltipAdder, @NotNull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flagIn);
        double output = config.getMin();
        long energy = 0;
        long tickCount = 0;
        long nextIncrease = config.getStep();
        boolean wirelessOn = false;
        long second = config.getSecond();
        long step = config.getStep();
        // 1.21.11：block_entity_data 组件为 TypedEntityData，getUnsafe 取原始 tag
        TypedEntityData<BlockEntityType<?>> entityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = entityData == null ? new CompoundTag() : entityData.getUnsafe();
        if (!tag.isEmpty()) {
            // 1.21.11：CompoundTag.getLong/getBoolean 返回 Optional，改用 getXOr（缺字段用默认值）
            output = tag.getLongOr("output", config.getMin());
            energy = tag.getLongOr("energy", 0);
            tickCount = tag.getLongOr("tickCount", 0);
            // 兼容旧字段名 beaconIncrease
            nextIncrease = tag.getLongOr("nextIncrease", tag.getLongOr("beaconIncrease", config.getStep()));
            wirelessOn = tag.getBooleanOr("wirelessOn", false);
        }
        double percent = (int) (tickCount / 20.00D / second * 10000) / 100.00D;
        // 数值行使用机器主题色
        tooltipAdder.accept(Component.translatable("item.autoresource.energy_generator.tooltip.energy", energy).withStyle(config.getThemeColor()));
        tooltipAdder.accept(Component.translatable("item.autoresource.energy_generator.tooltip.output", output).withStyle(config.getThemeColor()));
        if (output >= config.getMax()) {
            tooltipAdder.accept(Component.translatable("item.autoresource.energy_generator.tooltip.next_max").withStyle(ChatFormatting.GOLD));
        } else {
            tooltipAdder.accept(Component.translatable("item.autoresource.energy_generator.tooltip.next", nextIncrease).withStyle(config.getThemeColor()));
        }
        if (output < config.getMax()) {
            tooltipAdder.accept(Component.translatable("item.autoresource.energy_generator.tooltip.growth", percent).withStyle(ChatFormatting.GREEN));
        } else {
            tooltipAdder.accept(Component.translatable("item.autoresource.energy_generator.tooltip.growth_max").withStyle(ChatFormatting.GOLD));
        }
        tooltipAdder.accept(Component.translatable("item.autoresource.energy_generator.tooltip.step", second, step).withStyle(ChatFormatting.GRAY));
        tooltipAdder.accept(Component.translatable(wirelessOn ? "item.autoresource.energy_generator.tooltip.wireless_on" : "item.autoresource.energy_generator.tooltip.wireless_off")
                .withStyle(wirelessOn ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltipAdder.accept(Component.translatable("item.autoresource.energy_generator.tooltip.group_faster").withStyle(ChatFormatting.DARK_GRAY));
        tooltipAdder.accept(Component.translatable("item.autoresource.energy_generator.tooltip.tip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
