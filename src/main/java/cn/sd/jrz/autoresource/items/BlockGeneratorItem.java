package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.DataConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
        super(block, new Properties().stacksTo(1).fireResistant());
        this.config = config;
    }

    /** 物品名称使用机器主题色 */
    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(config.getThemeColor());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        double output = config.getMin() / 1000D;
        long block = 0;
        long tickCount = 0;
        long second = config.getSecond();
        long step = config.getStep();
        CompoundTag tag = stack.hasTag() ? stack.getTagElement("BlockEntityTag") : null;
        if (tag != null) {
            if (tag.contains("output", Tag.TAG_LONG)) {
                output = tag.getLong("output") / 1000D;
            }
            if (tag.contains("block", Tag.TAG_LONG)) {
                block = tag.getLong("block") / 1000;
            }
            if (tag.contains("tickCount", Tag.TAG_LONG)) {
                tickCount = tag.getLong("tickCount");
            }
        }
        double percent = (int) (tickCount / 20.00D / second * 10000) / 100.00D;
        // 数值行使用机器主题色
        tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.block", block).withStyle(config.getThemeColor()));
        tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.output", output).withStyle(config.getThemeColor()));
        if (output < config.getMax()) {
            tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.growth", percent).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.growth_max").withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.step", second, step / 1000D).withStyle(ChatFormatting.GRAY));
        // 标记槽内容物描述（兼容未标记/为空的情况）
        if (tag != null && tag.contains("markerSlot", Tag.TAG_COMPOUND)) {
            ItemStackHandler marker = new ItemStackHandler();
            marker.deserializeNBT(tag.getCompound("markerSlot"));
            ItemStack marked = marker.getStackInSlot(0);
            if (!marked.isEmpty()) {
                tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.marked", marked.getHoverName()).withStyle(ChatFormatting.GOLD));
            } else {
                tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.unmarked").withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.unmarked").withStyle(ChatFormatting.GRAY));
        }
        // 可生成方块列表：配置中的 # 标签展开为实际物品；最多展示前 MAX_ITEMS 种，超过则在尾部提示总数量
        List<Item> items = new ArrayList<>(getSupportedItems());
        if (!items.isEmpty()) {
            tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.blocks").withStyle(ChatFormatting.GRAY));
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
                tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.blocks_more", items.size(), MAX_ITEMS).withStyle(ChatFormatting.GRAY));
            }
        }
        tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.set_block").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.autoresource.block_generator.tooltip.tip").withStyle(ChatFormatting.DARK_GRAY));
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
                    for (Item item : ForgeRegistries.ITEMS.tags().getTag(tagKey)) {
                        supported.add(item);
                    }
                }
            } else {
                // 物品 ID 条目 → 直接加入
                ResourceLocation loc = ResourceLocation.tryParse(id);
                if (loc != null) {
                    Item item = ForgeRegistries.ITEMS.getValue(loc);
                    if (item != null) {
                        supported.add(item);
                    }
                }
            }
        }
        return supported;
    }

}