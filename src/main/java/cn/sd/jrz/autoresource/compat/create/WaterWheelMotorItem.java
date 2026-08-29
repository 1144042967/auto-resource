package cn.sd.jrz.autoresource.compat.create;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 水车马达物品（仅 Create 加载时注册）。tooltip 说明转速/应力容量由水车数量决定、
 * 方向可在 GUI 调节、破坏掉落内部水车。
 */
public class WaterWheelMotorItem extends BlockItem {

    public WaterWheelMotorItem(Block block) {
        super(block, new Properties().stacksTo(1).fireResistant());
    }

    /**
     * 物品名称使用水主题色（与水生成机一致）
     */
    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.AQUA);
    }

    /**
     * tooltip 仅在客户端渲染调用，且只使用 common 类，无需环境隔离注解
     */
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level worldIn, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        tooltip.add(Component.translatable("item.autoresource.water_wheel_motor.tooltip.owner").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.autoresource.water_wheel_motor.tooltip.speed").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.autoresource.water_wheel_motor.tooltip.capacity").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.autoresource.water_wheel_motor.tooltip.dir").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.autoresource.water_wheel_motor.tooltip.tip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
