package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.setup.Registration;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BlockGeneratorItem extends BlockItem {
    /** tooltip 每行展示的方块数量 */
    private static final int PER_ROW = 5;
    /** tooltip 最多展示的方块数量，超过则在尾部提示总数量 */
    private static final int MAX_ITEMS = 100;

    private final DataConfig config;

    public BlockGeneratorItem(Block block, DataConfig config) {
        super(block, new Properties().stacksTo(1).fireResistant().component(Registration.BLOCK_DATA.get(), ""));
        this.config = config;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
        double output = config.getMin() / 1000D;
        long block = 0;
        long tickCount = 0;
        long second = config.getSecond();
        long step = config.getStep();
        String markerItemId = "";
        String blockData = stack.getOrDefault(Registration.BLOCK_DATA.get(), "");
        if (!blockData.isEmpty()) {
            String[] dataArray = blockData.split(",");
            output = Tool.parseLong(dataArray, 0) / 1000D;
            block = Tool.parseLong(dataArray, 1) / 1000;
            tickCount = Tool.parseLong(dataArray, 2);
            markerItemId = Tool.parseString(dataArray, 10);
        }
        double percent = (int) (tickCount / 20.00D / second * 10000) / 100.00D;
        tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.block", block));
        tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.output", output));
        if (output < config.getMax()) {
            tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.growth", percent));
        } else {
            tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.growth_max"));
        }
        tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.step", second, step / 1000D));
        // 标记槽内容物描述（兼容未标记/为空的情况）
        if (!markerItemId.isEmpty()) {
            Item markedItem = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(markerItemId));
            if (markedItem != Items.AIR) {
                tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.marked", new ItemStack(markedItem).getHoverName()));
            } else {
                tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.unmarked"));
            }
        } else {
            tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.unmarked"));
        }
        // 可生成方块列表：配置中的 # 标签展开为实际物品；最多展示前 MAX_ITEMS 种，超过则在尾部提示总数量
        List<Item> items = new ArrayList<>(getSupportedItems());
        if (!items.isEmpty()) {
            tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.blocks"));
            int visible = Math.min(items.size(), MAX_ITEMS);
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (int i = 0; i < visible; i++) {
                if (count > 0) {
                    sb.append(", ");
                }
                sb.append(items.get(i).getDescription().getString());
                if (++count == PER_ROW) {
                    tooltip.add(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
                    sb = new StringBuilder();
                    count = 0;
                }
            }
            if (count > 0) {
                tooltip.add(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
            }
            if (items.size() > MAX_ITEMS) {
                tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.blocks_more", items.size(), MAX_ITEMS));
            }
        }
        tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.set_block"));
        tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.tip"));
    }

    /** 展开配置的方块生成机产品为实际物品集合（标签展开为标签下的所有物品，去重并保持配置顺序） */
    private static Set<Item> getSupportedItems() {
        Set<Item> supported = new LinkedHashSet<>();
        for (String entry : DataConfig.getBlockGeneratorItems()) {
            if (entry == null) {
                continue;
            }
            String id = entry.trim();
            if (id.isEmpty()) {
                continue;
            }
            if (id.startsWith("#")) {
                // 标签条目 → 展开为标签下的所有物品
                ResourceLocation loc = ResourceLocation.tryParse(id.substring(1));
                if (loc != null) {
                    TagKey<Item> tagKey = TagKey.create(Registries.ITEM, loc);
                    BuiltInRegistries.ITEM.getTag(tagKey).ifPresent(holders -> {
                        for (Holder<Item> holder : holders) {
                            supported.add(holder.value());
                        }
                    });
                }
            } else {
                // 物品 ID 条目 → 直接加入
                ResourceLocation loc = ResourceLocation.tryParse(id);
                if (loc != null) {
                    Item item = BuiltInRegistries.ITEM.get(loc);
                    if (item != Items.AIR) {
                        supported.add(item);
                    }
                }
            }
        }
        return supported;
    }
}
