package cn.sd.jrz.autoresource;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class Config {

    public static ModConfigSpec.LongValue FE_MIN;
    public static ModConfigSpec.LongValue FE_MAX;
    public static ModConfigSpec.LongValue FE_SECOND;
    public static ModConfigSpec.LongValue FE_STEP;
    /** 加速增长所需物品（放入加速槽后增长量变为当前发电量的 1%） */
    public static ModConfigSpec.ConfigValue<String> FE_STAR_ITEM;

    public static ModConfigSpec.LongValue WATER_MIN;
    public static ModConfigSpec.LongValue WATER_MAX;
    public static ModConfigSpec.LongValue WATER_SECOND;
    public static ModConfigSpec.LongValue WATER_STEP;

    public static ModConfigSpec.LongValue LAVA_MIN;
    public static ModConfigSpec.LongValue LAVA_MAX;
    public static ModConfigSpec.LongValue LAVA_SECOND;
    public static ModConfigSpec.LongValue LAVA_STEP;

    public static ModConfigSpec.LongValue BLOCK_MIN;
    public static ModConfigSpec.LongValue BLOCK_MAX;
    public static ModConfigSpec.LongValue BLOCK_SECOND;
    public static ModConfigSpec.LongValue BLOCK_STEP;
    /** 方块生成机可生成的产品列表：支持物品 ID（如 minecraft:dirt）或物品标签（以 # 开头，如 #minecraft:planks） */
    public static ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_GENERATOR_ITEMS;

    public static ModConfigSpec SERVER_CONFIG;

    static {
        ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();

        SERVER_BUILDER.comment("Energy Generator Settings").push("Energy");

        SERVER_BUILDER.push("FE");
        FE_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.").defineInRange("min", 1, 1, Long.MAX_VALUE);
        FE_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        FE_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 1, 1, Long.MAX_VALUE);
        FE_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.").defineInRange("step", 1, 0, Long.MAX_VALUE);
        FE_STAR_ITEM = SERVER_BUILDER.comment("The item that boosts the growth rate. When placed in the boost slot, each increase becomes 1% of the current output. Use item registry name, e.g. minecraft:nether_star.").define("star_item", "minecraft:nether_star");
        SERVER_BUILDER.pop();

        SERVER_BUILDER.pop();

        SERVER_BUILDER.comment("Liquid Generator Settings").push("Liquid");

        SERVER_BUILDER.push("Water");
        WATER_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        WATER_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        WATER_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        WATER_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Lava");
        LAVA_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        LAVA_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        LAVA_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        LAVA_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.pop();

        SERVER_BUILDER.comment("Block Generator Settings").push("Block");

        SERVER_BUILDER.push("Generator");
        BLOCK_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        BLOCK_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        BLOCK_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        BLOCK_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        BLOCK_GENERATOR_ITEMS = SERVER_BUILDER.comment(
                "List of items (registry names like minecraft:dirt) or item tags (prefixed with # like #minecraft:stone_bricks) that the block generator can produce. The marker slot accepts items matching any entry.")
                .defineList("items",
                        List.of(
                                // 主世界 — 自然生成/基础方块（按常见程度排序）
                                "minecraft:stone", "minecraft:deepslate", "minecraft:dirt", "minecraft:sand", "minecraft:gravel",
                                "minecraft:cobblestone", "minecraft:andesite", "minecraft:granite", "minecraft:diorite", "minecraft:tuff",
                                "minecraft:cobbled_deepslate", "minecraft:clay", "minecraft:red_sand", "minecraft:moss_block", "minecraft:rooted_dirt",
                                "minecraft:mud", "minecraft:dripstone_block", "minecraft:calcite", "minecraft:amethyst_block", "minecraft:obsidian",
                                "minecraft:prismarine",
                                // 主世界 — 建筑加工方块（按常见程度排序）
                                "minecraft:bricks", "minecraft:smooth_stone",
                                // 标签优先：#minecraft:stone_bricks 覆盖石砖/苔石砖/裂纹石砖/錾制石砖
                                "#minecraft:stone_bricks",
                                "minecraft:mossy_cobblestone", "minecraft:sandstone", "minecraft:smooth_sandstone", "minecraft:red_sandstone",
                                "minecraft:polished_andesite", "minecraft:polished_granite", "minecraft:polished_diorite",
                                "minecraft:polished_deepslate", "minecraft:deepslate_bricks", "minecraft:mud_bricks",
                                // 下界 — 按常见程度排序
                                "minecraft:netherrack", "minecraft:basalt", "minecraft:smooth_basalt", "minecraft:soul_sand", "minecraft:soul_soil",
                                "minecraft:blackstone", "minecraft:polished_blackstone_bricks", "minecraft:polished_blackstone",
                                "minecraft:nether_bricks", "minecraft:magma_block",
                                // 末地 — 按常见程度排序
                                "minecraft:end_stone", "minecraft:end_stone_bricks"),
                        obj -> obj instanceof String);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.pop();

        SERVER_CONFIG = SERVER_BUILDER.build();
    }

    public static void init(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG);
    }
}
