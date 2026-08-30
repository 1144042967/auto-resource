package cn.sd.jrz.autoresource.compat.create;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * 水车马达物品（仅 Create 加载时注册）。tooltip 说明转速/应力容量由水车数量决定、
 * 方向可在 GUI 调节、破坏掉落内部水车。
 */
public class WaterWheelMotorItem extends BlockItem {

    public WaterWheelMotorItem(Block block, ResourceKey<Item> registryKey) {
        super(block, new Properties().setId(registryKey).stacksTo(1).fireResistant());
    }

    /**
     * 物品名称使用水主题色（与水生成机一致）。1.21.11：Item 默认名称键为 item.&lt;id&gt;，语言文件用 block.&lt;id&gt;，
     * 显式改用方块翻译键。
     */
    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.translatable(this.getBlock().getDescriptionId()).copy().withStyle(ChatFormatting.AQUA);
    }

    /**
     * tooltip 仅在客户端渲染调用，且只使用 common 类，无需环境隔离注解
     * 1.21.11：appendHoverText 签名改为 TooltipDisplay + Consumer<Component>
     */
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> tooltipAdder, @NotNull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flagIn);
        tooltipAdder.accept(Component.translatable("item.autoresource.water_wheel_motor.tooltip.owner").withStyle(ChatFormatting.GRAY));
        tooltipAdder.accept(Component.translatable("item.autoresource.water_wheel_motor.tooltip.speed").withStyle(ChatFormatting.GRAY));
        tooltipAdder.accept(Component.translatable("item.autoresource.water_wheel_motor.tooltip.capacity").withStyle(ChatFormatting.GRAY));
        tooltipAdder.accept(Component.translatable("item.autoresource.water_wheel_motor.tooltip.dir").withStyle(ChatFormatting.GRAY));
        tooltipAdder.accept(Component.translatable("item.autoresource.water_wheel_motor.tooltip.tip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
