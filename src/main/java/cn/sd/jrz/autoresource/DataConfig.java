package cn.sd.jrz.autoresource;

import cn.sd.jrz.autoresource.setup.Registration;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public abstract class DataConfig {
    public static final DataConfig ENERGY_GENERATOR_FE = new DataConfig(Config.FE_MIN, Config.FE_MAX, Config.FE_SECOND, Config.FE_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.ENERGY_GENERATOR_FE_ENTITY.get();
        }

        @Override
        public Item getStarItem() {
            String id = Config.FE_STAR_ITEM.get();
            ResourceLocation loc = ResourceLocation.tryParse(id);
            Item item = loc != null ? ForgeRegistries.ITEMS.getValue(loc) : null;
            return item != null ? item : Items.NETHER_STAR;
        }
    };
    public static final DataConfig LIQUID_GENERATOR_WATER = new DataConfig(Config.WATER_MIN, Config.WATER_MAX, Config.WATER_SECOND, Config.WATER_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.LIQUID_GENERATOR_WATER_ENTITY.get();
        }

        @Override
        public Fluid getFluid() {
            return Fluids.WATER;
        }

        @Override
        public Block getBlock() {
            return Blocks.WATER;
        }
    };
    public static final DataConfig LIQUID_GENERATOR_LAVA = new DataConfig(Config.LAVA_MIN, Config.LAVA_MAX, Config.LAVA_SECOND, Config.LAVA_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.LIQUID_GENERATOR_LAVA_ENTITY.get();
        }

        @Override
        public Fluid getFluid() {
            return Fluids.LAVA;
        }

        @Override
        public Block getBlock() {
            return Blocks.LAVA;
        }
    };

    /** 通用可标记方块生成机（输出种类由标记槽决定） */
    public static final DataConfig BLOCK_GENERATOR = new DataConfig(Config.BLOCK_MIN, Config.BLOCK_MAX, Config.BLOCK_SECOND, Config.BLOCK_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_ENTITY.get();
        }
    };

    private final ForgeConfigSpec.LongValue min;
    private final ForgeConfigSpec.LongValue max;
    private final ForgeConfigSpec.LongValue second;
    private final ForgeConfigSpec.LongValue step;

    public DataConfig(ForgeConfigSpec.LongValue min, ForgeConfigSpec.LongValue max, ForgeConfigSpec.LongValue second, ForgeConfigSpec.LongValue step) {
        this.min = min;
        this.max = max;
        this.second = second;
        this.step = step;
    }

    public long getMin() {
        return Tool.suit(min.get());
    }

    public long getMax() {
        return Tool.suit(max.get());
    }

    public long getSecond() {
        return Tool.suit(second.get());
    }

    public long getStep() {
        return Tool.suit(step.get());
    }

    public abstract BlockEntityType<?> getEntityType();

    public Fluid getFluid() {
        return Fluids.EMPTY;
    }

    public Block getBlock() {
        return Blocks.AIR;
    }

    /** 加速增长所需物品（放入后增长量变为当前发电量的 1%），仅 FE 发电机使用，默认返回空 */
    @Nullable
    public Item getStarItem() {
        return null;
    }

    /** 判断物品是否为合法的方块生成机产品（可放入标记槽）：支持配置的物品 ID（如 minecraft:dirt）或 # 标签（如 #minecraft:planks） */
    public static boolean isBlockGeneratorItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (String entry : Config.BLOCK_GENERATOR_ITEMS.get()) {
            if (entry == null) {
                continue;
            }
            String id = entry.trim();
            if (id.isEmpty()) {
                continue;
            }
            if (id.startsWith("#")) {
                // 标签形式：#minecraft:planks
                ResourceLocation loc = ResourceLocation.tryParse(id.substring(1));
                if (loc != null && stack.is(TagKey.create(Registries.ITEM, loc))) {
                    return true;
                }
            } else {
                // 物品 ID 形式：minecraft:dirt
                ResourceLocation loc = ResourceLocation.tryParse(id);
                if (loc != null && stack.getItem() == ForgeRegistries.ITEMS.getValue(loc)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 配置的方块生成机可生成产品列表（原始配置内容，用于物品 tooltip 展示） */
    public static List<? extends String> getBlockGeneratorItems() {
        return Config.BLOCK_GENERATOR_ITEMS.get();
    }
}
