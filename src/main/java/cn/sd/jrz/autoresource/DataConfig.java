package cn.sd.jrz.autoresource;

import cn.sd.jrz.autoresource.setup.Registration;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.LongSupplier;

public abstract class DataConfig {
    public static final DataConfig ENERGY_GENERATOR_FE = new DataConfig(
            () -> Config.get().energy.fe.min, () -> Config.get().energy.fe.max,
            () -> Config.get().energy.fe.second, () -> Config.get().energy.fe.step) {
        // 加速物品缓存：配置值变化时重新解析（可被 /reload 修改），避免 GUI 每帧查注册表
        private String cachedStarId;
        private Item cachedStarItem;

        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.ENERGY_GENERATOR_FE_ENTITY;
        }

        @Override
        public Item getStarItem() {
            String id = Config.get().energy.fe.starItem;
            if (cachedStarItem == null || !id.equals(cachedStarId)) {
                cachedStarId = id;
                Identifier loc = Identifier.tryParse(id);
                Item resolved = loc != null ? BuiltInRegistries.ITEM.getValue(loc) : null;
                // Fabric 注册表查不到时返回 AIR 而非 null，一并回退默认
                cachedStarItem = resolved == null || resolved == Items.AIR ? Items.NETHER_STAR : resolved;
            }
            return cachedStarItem;
        }

        @Override
        public ChatFormatting getThemeColor() {
            // 能源/电力 → 红色
            return ChatFormatting.RED;
        }
    };
    public static final DataConfig LIQUID_GENERATOR_WATER = new DataConfig(
            () -> Config.get().liquid.water.min, () -> Config.get().liquid.water.max,
            () -> Config.get().liquid.water.second, () -> Config.get().liquid.water.step) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.LIQUID_GENERATOR_WATER_ENTITY;
        }

        @Override
        public Fluid getFluid() {
            return Fluids.WATER;
        }

        @Override
        public Block getBlock() {
            return Blocks.WATER;
        }

        @Override
        public ChatFormatting getThemeColor() {
            // 水 → 青色
            return ChatFormatting.AQUA;
        }
    };
    public static final DataConfig LIQUID_GENERATOR_LAVA = new DataConfig(
            () -> Config.get().liquid.lava.min, () -> Config.get().liquid.lava.max,
            () -> Config.get().liquid.lava.second, () -> Config.get().liquid.lava.step) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.LIQUID_GENERATOR_LAVA_ENTITY;
        }

        @Override
        public Fluid getFluid() {
            return Fluids.LAVA;
        }

        @Override
        public Block getBlock() {
            return Blocks.LAVA;
        }

        @Override
        public ChatFormatting getThemeColor() {
            // 岩浆 → 金色
            return ChatFormatting.GOLD;
        }
    };

    /**
     * 通用可标记方块生成机（输出种类由标记槽决定）
     */
    public static final DataConfig BLOCK_GENERATOR = new DataConfig(
            () -> Config.get().block.generator.min, () -> Config.get().block.generator.max,
            () -> Config.get().block.generator.second, () -> Config.get().block.generator.step) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_ENTITY;
        }

        @Override
        public ChatFormatting getThemeColor() {
            // 方块/自然 → 绿色
            return ChatFormatting.GREEN;
        }
    };

    private final LongSupplier min;
    private final LongSupplier max;
    private final LongSupplier second;
    private final LongSupplier step;

    public DataConfig(LongSupplier min, LongSupplier max, LongSupplier second, LongSupplier step) {
        this.min = min;
        this.max = max;
        this.second = second;
        this.step = step;
    }

    public long getMin() {
        return Tool.suit(min.getAsLong());
    }

    public long getMax() {
        return Tool.suit(max.getAsLong());
    }

    public long getSecond() {
        return Tool.suit(second.getAsLong());
    }

    public long getStep() {
        return Tool.suit(step.getAsLong());
    }

    public abstract BlockEntityType<?> getEntityType();

    public Fluid getFluid() {
        return Fluids.EMPTY;
    }

    public Block getBlock() {
        return Blocks.AIR;
    }

    /**
     * 加速增长所需物品（放入后增长量变为当前发电量的 1%），仅 FE 发电机使用，默认返回空
     */
    @Nullable
    public Item getStarItem() {
        return null;
    }

    /**
     * 机器主题色：用于物品名称与 tooltip 数值行的着色，子类按机器类型各自定义
     */
    public ChatFormatting getThemeColor() {
        return ChatFormatting.WHITE;
    }

    /**
     * 判断物品是否为合法的方块生成机产品（可放入标记槽）：支持配置的物品 ID（如 minecraft:dirt）或 # 标签（如 #minecraft:planks）
     */
    public static boolean isBlockGeneratorItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (String entry : Config.get().block.items) {
            if (entry == null) {
                continue;
            }
            String id = entry.trim();
            if (id.isEmpty()) {
                continue;
            }
            if (id.startsWith("#")) {
                // 标签形式：#minecraft:planks
                Identifier loc = Identifier.tryParse(id.substring(1));
                if (loc != null && stack.is(TagKey.create(Registries.ITEM, loc))) {
                    return true;
                }
            } else {
                // 物品 ID 形式：minecraft:dirt
                Identifier loc = Identifier.tryParse(id);
                if (loc != null && stack.getItem() == BuiltInRegistries.ITEM.getValue(loc)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 配置的方块生成机可生成产品列表（原始配置内容，用于物品 tooltip 展示）
     */
    public static List<String> getBlockGeneratorItems() {
        return Config.get().block.items;
    }
}
