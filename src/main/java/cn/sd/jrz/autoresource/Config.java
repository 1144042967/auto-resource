package cn.sd.jrz.autoresource;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.List;

public class Config {

    public static ForgeConfigSpec.LongValue FE_MIN;
    public static ForgeConfigSpec.LongValue FE_MAX;
    public static ForgeConfigSpec.LongValue FE_SECOND;
    public static ForgeConfigSpec.LongValue FE_STEP;
    public static ForgeConfigSpec.ConfigValue<String> FE_STAR_ITEM;
    /**
     * 反射绕过第三方MOD机器能量限制（补满到容量）总开关，默认开启。
     */
    public static ForgeConfigSpec.BooleanValue FE_BYPASS_ENABLED;

    public static ForgeConfigSpec.LongValue WATER_MIN;
    public static ForgeConfigSpec.LongValue WATER_MAX;
    public static ForgeConfigSpec.LongValue WATER_SECOND;
    public static ForgeConfigSpec.LongValue WATER_STEP;

    public static ForgeConfigSpec.LongValue LAVA_MIN;
    public static ForgeConfigSpec.LongValue LAVA_MAX;
    public static ForgeConfigSpec.LongValue LAVA_SECOND;
    public static ForgeConfigSpec.LongValue LAVA_STEP;

    public static ForgeConfigSpec.LongValue BLOCK_MIN;
    public static ForgeConfigSpec.LongValue BLOCK_MAX;
    public static ForgeConfigSpec.LongValue BLOCK_SECOND;
    public static ForgeConfigSpec.LongValue BLOCK_STEP;
    /**
     * 方块生成机可生成的产品列表：支持物品 ID（如 minecraft:dirt）或物品标签（以 # 开头，如 #minecraft:planks）
     */
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCK_GENERATOR_ITEMS;

    public static ForgeConfigSpec SERVER_CONFIG;

    static {
        ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();

        SERVER_BUILDER.comment("Energy Generator Settings").push("Energy");

        SERVER_BUILDER.push("FE");
        FE_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.").defineInRange("min", 1, 1, Long.MAX_VALUE);
        FE_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        FE_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 1, 1, Long.MAX_VALUE);
        FE_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.").defineInRange("step", 1, 0, Long.MAX_VALUE);
        FE_STAR_ITEM = SERVER_BUILDER.comment("The item that boosts the growth rate. When placed in the boost slot, each increase becomes 1% of the current output. Use item registry name, e.g. minecraft:nether_star.").define("star_item", "minecraft:nether_star");
        FE_BYPASS_ENABLED = SERVER_BUILDER.comment("Bypass the energy capacity/receive limits of third-party machines (Mekanism, Thermal Expansion, Industrial Foregoing, Draconic Evolution, Flux Networks) by directly refilling their internal energy to capacity via reflection when they refuse to accept energy. Default: true.").define("bypass_enabled", true);
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
                                // 主世界 — 自然生成/基础方块
                                "minecraft:stone", "minecraft:deepslate", "minecraft:dirt", "minecraft:sand", "minecraft:gravel",
                                "minecraft:cobblestone", "minecraft:andesite", "minecraft:granite", "minecraft:diorite", "minecraft:tuff",
                                "minecraft:cobbled_deepslate", "minecraft:clay", "minecraft:red_sand", "minecraft:moss_block", "minecraft:rooted_dirt",
                                "minecraft:mud", "minecraft:dripstone_block", "minecraft:calcite", "minecraft:amethyst_block", "minecraft:obsidian",
                                "minecraft:prismarine",
                                // 主世界 — 建筑加工方块
                                "minecraft:bricks", "minecraft:smooth_stone",
                                // 标签优先：#minecraft:stone_bricks 覆盖全部石砖变种
                                "#minecraft:stone_bricks",
                                "minecraft:mossy_cobblestone", "minecraft:sandstone", "minecraft:smooth_sandstone", "minecraft:red_sandstone",
                                "minecraft:polished_andesite", "minecraft:polished_granite", "minecraft:polished_diorite",
                                "minecraft:polished_deepslate", "minecraft:deepslate_bricks", "minecraft:mud_bricks",
                                // 下界
                                "minecraft:netherrack", "minecraft:basalt", "minecraft:smooth_basalt", "minecraft:soul_sand", "minecraft:soul_soil",
                                "minecraft:blackstone", "minecraft:polished_blackstone_bricks", "minecraft:polished_blackstone",
                                "minecraft:nether_bricks", "minecraft:magma_block",
                                // 末地
                                "minecraft:end_stone", "minecraft:end_stone_bricks"),
                        obj -> obj instanceof String);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.pop();

        SERVER_CONFIG = SERVER_BUILDER.build();
    }

    public static void init(FMLJavaModLoadingContext context) {
        // 47.2.20 的 FMLJavaModLoadingContext 不继承 ModLoadingContext，需通过 ModLoadingContext.get() 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG);
    }
}
