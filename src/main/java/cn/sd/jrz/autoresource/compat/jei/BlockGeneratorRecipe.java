package cn.sd.jrz.autoresource.compat.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * 方块生成机的 JEI 展示配方：一页可生成的方块 + 卡片底部的说明文字。
 * <p>
 * 这台机器没有"输入 → 输出"的固定配方——输出种类由标记槽决定、可生成范围由配置
 * {@code block_generator.items} 决定，所以一页只把可生成方块平铺出来。
 * {@code noteLines} 每个元素占一行（先写总数、再写页码），列表为空即不画说明。
 */
public record BlockGeneratorRecipe(@Nonnull List<ItemStack> products, @Nonnull List<Component> noteLines) {
}
