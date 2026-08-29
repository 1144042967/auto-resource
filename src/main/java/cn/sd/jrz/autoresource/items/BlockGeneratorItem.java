package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.storage.MachineSlotStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class BlockGeneratorItem extends BlockItem {
    /**
     * tooltip 每行展示的方块数量
     */
    private static final int PER_ROW = 5;
    /**
     * tooltip 最多展示的方块数量，超过则在尾部提示总数量
     */
    private static final int MAX_ITEMS = 100;

    private final DataConfig config;

    public BlockGeneratorItem(Block block, DataConfig config) {
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
        double output = config.getMin() / 1000D;
        long block = 0;
        long tickCount = 0;
        long second = config.getSecond();
        long step = config.getStep();
        CompoundTag tag = readBlockEntityTag(stack);
        if (tag != null) {
            if (tag.contains("output")) {
                output = tag.getLongOr("output", config.getMin()) / 1000D;
            }
            if (tag.contains("block")) {
                block = tag.getLongOr("block", 0) / 1000;
            }
            if (tag.contains("tickCount")) {
                tickCount = tag.getLongOr("tickCount", 0);
            }
        }
        double percent = (int) (tickCount / 20.00D / second * 10000) / 100.00D;
        // 数值行使用机器主题色
        tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.block", block).withStyle(config.getThemeColor()));
        tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.output", output).withStyle(config.getThemeColor()));
        if (output < config.getMax()) {
            tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.growth", percent).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.growth_max").withStyle(ChatFormatting.GOLD));
        }
        tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.step", second, step / 1000D).withStyle(ChatFormatting.GRAY));
        // 标记槽内容物描述（兼容未标记/为空的情况）
        CompoundTag markerNbt = (tag != null) ? tag.getCompoundOrEmpty("markerSlot") : null;
        if (tag != null && !markerNbt.isEmpty()) {
            MachineSlotStorage marker = new MachineSlotStorage(1).deserializeNBT(markerNbt);
            ItemStack marked = marker.getItem(0);
            if (!marked.isEmpty()) {
                tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.marked", marked.getHoverName()).withStyle(ChatFormatting.GOLD));
            } else {
                tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.unmarked").withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.unmarked").withStyle(ChatFormatting.GRAY));
        }
        // 可生成方块列表（# 标签展开为实际物品；最多展示 MAX_ITEMS 种）
        List<Item> items = new ArrayList<>(getSupportedItems());
        if (!items.isEmpty()) {
            tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.blocks").withStyle(ChatFormatting.GRAY));
            int visible = Math.min(items.size(), MAX_ITEMS);
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (int i = 0; i < visible; i++) {
                if (count > 0) {
                    sb.append(", ");
                }
                // 26.x 起 Item.getDescription() 被移除，使用 getDescriptionId() 拿语言键后转字符串
                sb.append(items.get(i).getDescriptionId());
                if (++count == PER_ROW) {
                    tooltip.accept(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
                    sb = new StringBuilder();
                    count = 0;
                }
            }
            if (count > 0) {
                tooltip.accept(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
            }
            if (items.size() > MAX_ITEMS) {
                tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.blocks_more", items.size(), MAX_ITEMS).withStyle(ChatFormatting.GRAY));
            }
        }
        tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.set_block").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.autoresource.block_generator.tooltip.tip").withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * 配置列表缓存快照（用于判断配置变化而失效缓存）
     */
    private static List<String> cachedConfigItems;
    /**
     * 展开后的实际物品集合缓存（避免 tooltip 每帧重复展开标签）
     */
    private static Set<Item> cachedSupportedItems;

    /**
     * 展开配置的产品为实际物品集合（标签展开、去重、保持顺序），按配置内容缓存
     */
    private static Set<Item> getSupportedItems() {
        List<String> current = DataConfig.getBlockGeneratorItems();
        if (cachedSupportedItems == null || !current.equals(cachedConfigItems)) {
            cachedConfigItems = List.copyOf(current);
            cachedSupportedItems = expandSupportedItems(cachedConfigItems);
        }
        return cachedSupportedItems;
    }

    private static Set<Item> expandSupportedItems(List<String> entries) {
        Set<Item> supported = new LinkedHashSet<>();
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String id = entry.trim();
            if (id.isEmpty()) {
                continue;
            }
            if (id.startsWith("#")) {
                // 标签条目 → 展开为标签下的所有物品
                Identifier loc = Identifier.tryParse(id.substring(1));
                if (loc != null) {
                    TagKey<Item> tagKey = TagKey.create(Registries.ITEM, loc);
                    // 26.x 起 getTag 改为 getTagOrEmpty，返回 Iterable<Holder<Item>>
                    for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagKey)) {
                        supported.add(holder.value());
                    }
                }
            } else {
                // 物品 ID 条目 → 直接加入（查不到时返回 Optional.empty）
                Identifier loc = Identifier.tryParse(id);
                if (loc != null) {
                    Item item = BuiltInRegistries.ITEM.get(loc).map(net.minecraft.core.Holder::value).orElse(null);
                    if (item != null && item != net.minecraft.world.item.Items.AIR) {
                        supported.add(item);
                    }
                }
            }
        }
        return supported;
    }

    /**
     * 从 ItemStack 读取 BLOCK_ENTITY_DATA 组件对应的 CompoundTag（MC 26.x 起 BlockEntityTag 已被 DataComponent 取代）。
     * 缺失时返回 null。
     */
    private static CompoundTag readBlockEntityTag(ItemStack stack) {
        TypedEntityData<?> beData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return beData != null ? beData.getUnsafe() : null;
    }
}