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
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
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

    public BlockGeneratorItem(Block block, DataConfig config, ResourceKey<Item> registryKey) {
        super(block, new Properties().setId(registryKey).stacksTo(1).fireResistant());
        this.config = config;
    }

    /**
     * 物品名称使用机器主题色
     */
    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        // 26.1.2：Item 默认名称键为 item.<id>（Properties 用 "item" 前缀），而语言文件使用 block.<id>，
        // 故显式改用方块翻译键，避免显示未翻译的键名
        return Component.translatable(this.getBlock().getDescriptionId()).copy().withStyle(config.getThemeColor());
    }

    /**
     * tooltip 仅在客户端渲染调用，且只使用 common 类，无需环境隔离注解
     * 26.1.2：appendHoverText 签名改为 TooltipDisplay + Consumer<Component>；
     * 组件读取改为 TypedEntityData（type + tag）
     */
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> tooltipAdder, @NotNull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flagIn);
        double output = config.getMin() / 1000D;
        long block = 0;
        long tickCount = 0;
        long second = config.getSecond();
        long step = config.getStep();
        // 26.1.2：block_entity_data 组件为 TypedEntityData，getUnsafe 取原始 tag
        TypedEntityData<BlockEntityType<?>> entityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = entityData == null ? new CompoundTag() : entityData.getUnsafe();
        if (!tag.isEmpty()) {
            // 26.1.2：CompoundTag.getLong 返回 Optional，改用 getLongOr（缺字段用默认值）
            output = tag.getLongOr("output", config.getMin()) / 1000D;
            block = tag.getLongOr("block", 0) / 1000;
            tickCount = tag.getLongOr("tickCount", 0);
        }
        double percent = (int) (tickCount / 20.00D / second * 10000) / 100.00D;
        // 数值行使用机器主题色
        tooltipAdder.accept(Component.translatable("item.autoresource.block_generator.tooltip.block", block).withStyle(config.getThemeColor()));
        tooltipAdder.accept(Component.translatable("item.autoresource.block_generator.tooltip.output", output).withStyle(config.getThemeColor()));
        if (output < config.getMax()) {
            tooltipAdder.accept(Component.translatable("item.autoresource.block_generator.tooltip.growth", percent).withStyle(ChatFormatting.GREEN));
        } else {
            tooltipAdder.accept(Component.translatable("item.autoresource.block_generator.tooltip.growth_max").withStyle(ChatFormatting.GOLD));
        }
        tooltipAdder.accept(Component.translatable("item.autoresource.block_generator.tooltip.step", second, step / 1000D).withStyle(ChatFormatting.GRAY));
        // 标记槽内容物描述（兼容未标记/为空的情况）
        if (tag.contains("markerSlot")) {
            MachineSlotStorage marker = new MachineSlotStorage(1).deserializeNBT(tag.getCompoundOrEmpty("markerSlot"), context.registries());
            ItemStack marked = marker.getItem(0);
            if (!marked.isEmpty()) {
                tooltipAdder.accept(Component.translatable("item.autoresource.block_generator.tooltip.marked", marked.getHoverName()).withStyle(ChatFormatting.GOLD));
            } else {
                tooltipAdder.accept(Component.translatable("item.autoresource.block_generator.tooltip.unmarked").withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltipAdder.accept(Component.translatable("item.autoresource.block_generator.tooltip.unmarked").withStyle(ChatFormatting.GRAY));
        }
        // 可生成方块列表（# 标签展开为实际物品；最多展示 MAX_ITEMS 种）
        List<Item> items = new ArrayList<>(getSupportedItems());
        if (!items.isEmpty()) {
            tooltipAdder.accept(Component.translatable("item.autoresource.block_generator.tooltip.blocks").withStyle(ChatFormatting.GRAY));
            int visible = Math.min(items.size(), MAX_ITEMS);
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (int i = 0; i < visible; i++) {
                if (count > 0) {
                    sb.append(", ");
                }
                // 26.1.2：Item.getName(ItemStack) 只读 ITEM_NAME 组件（空栈返回空），须用默认实例的 hover name
                sb.append(items.get(i).getDefaultInstance().getHoverName().getString());
                if (++count == PER_ROW) {
                    tooltipAdder.accept(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
                    sb = new StringBuilder();
                    count = 0;
                }
            }
            if (count > 0) {
                tooltipAdder.accept(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
            }
            if (items.size() > MAX_ITEMS) {
                tooltipAdder.accept(Component.translatable("item.autoresource.block_generator.tooltip.blocks_more", items.size(), MAX_ITEMS).withStyle(ChatFormatting.GRAY));
            }
        }
        tooltipAdder.accept(Component.translatable("item.autoresource.block_generator.tooltip.set_block").withStyle(ChatFormatting.DARK_GRAY));
        tooltipAdder.accept(Component.translatable("item.autoresource.block_generator.tooltip.tip").withStyle(ChatFormatting.DARK_GRAY));
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
                    // 26.1.2：Registry.getTag 移除，改用 getTagOrEmpty 直接迭代
                    for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagKey)) {
                        supported.add(holder.value());
                    }
                }
            } else {
                // 物品 ID 条目 → 直接加入（查不到时返回 AIR，跳过）
                Identifier loc = Identifier.tryParse(id);
                if (loc != null) {
                    // 26.1.2：Registry.get 返回 Optional<Reference<Item>>，改用 getOptional 直接取值
                    Item item = BuiltInRegistries.ITEM.getOptional(loc).orElse(Items.AIR);
                    if (item != Items.AIR) {
                        supported.add(item);
                    }
                }
            }
        }
        return supported;
    }
}