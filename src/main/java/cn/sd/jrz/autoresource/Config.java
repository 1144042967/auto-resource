package cn.sd.jrz.autoresource;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

    public static ModConfigSpec.LongValue FE_MIN;
    public static ModConfigSpec.LongValue FE_MAX;
    public static ModConfigSpec.LongValue FE_SECOND;
    public static ModConfigSpec.LongValue FE_STEP;
    public static ModConfigSpec.LongValue FE_BEACON_STEP;

    public static ModConfigSpec.LongValue WATER_MIN;
    public static ModConfigSpec.LongValue WATER_MAX;
    public static ModConfigSpec.LongValue WATER_SECOND;
    public static ModConfigSpec.LongValue WATER_STEP;

    public static ModConfigSpec.LongValue LAVA_MIN;
    public static ModConfigSpec.LongValue LAVA_MAX;
    public static ModConfigSpec.LongValue LAVA_SECOND;
    public static ModConfigSpec.LongValue LAVA_STEP;

    public static ModConfigSpec.LongValue DIRT_MIN;
    public static ModConfigSpec.LongValue DIRT_MAX;
    public static ModConfigSpec.LongValue DIRT_SECOND;
    public static ModConfigSpec.LongValue DIRT_STEP;

    public static ModConfigSpec.LongValue COBBLESTONE_MIN;
    public static ModConfigSpec.LongValue COBBLESTONE_MAX;
    public static ModConfigSpec.LongValue COBBLESTONE_SECOND;
    public static ModConfigSpec.LongValue COBBLESTONE_STEP;

    public static ModConfigSpec.LongValue STONE_MIN;
    public static ModConfigSpec.LongValue STONE_MAX;
    public static ModConfigSpec.LongValue STONE_SECOND;
    public static ModConfigSpec.LongValue STONE_STEP;

    public static ModConfigSpec.LongValue SMOOTH_STONE_MIN;
    public static ModConfigSpec.LongValue SMOOTH_STONE_MAX;
    public static ModConfigSpec.LongValue SMOOTH_STONE_SECOND;
    public static ModConfigSpec.LongValue SMOOTH_STONE_STEP;

    public static ModConfigSpec.LongValue CLAY_MIN;
    public static ModConfigSpec.LongValue CLAY_MAX;
    public static ModConfigSpec.LongValue CLAY_SECOND;
    public static ModConfigSpec.LongValue CLAY_STEP;

    public static ModConfigSpec.LongValue SAND_MIN;
    public static ModConfigSpec.LongValue SAND_MAX;
    public static ModConfigSpec.LongValue SAND_SECOND;
    public static ModConfigSpec.LongValue SAND_STEP;

    public static ModConfigSpec.LongValue GRAVEL_MIN;
    public static ModConfigSpec.LongValue GRAVEL_MAX;
    public static ModConfigSpec.LongValue GRAVEL_SECOND;
    public static ModConfigSpec.LongValue GRAVEL_STEP;

    public static ModConfigSpec.LongValue GRANITE_MIN;
    public static ModConfigSpec.LongValue GRANITE_MAX;
    public static ModConfigSpec.LongValue GRANITE_SECOND;
    public static ModConfigSpec.LongValue GRANITE_STEP;

    public static ModConfigSpec.LongValue DIORITE_MIN;
    public static ModConfigSpec.LongValue DIORITE_MAX;
    public static ModConfigSpec.LongValue DIORITE_SECOND;
    public static ModConfigSpec.LongValue DIORITE_STEP;

    public static ModConfigSpec.LongValue ANDESITE_MIN;
    public static ModConfigSpec.LongValue ANDESITE_MAX;
    public static ModConfigSpec.LongValue ANDESITE_SECOND;
    public static ModConfigSpec.LongValue ANDESITE_STEP;

    public static ModConfigSpec.LongValue CALCITE_MIN;
    public static ModConfigSpec.LongValue CALCITE_MAX;
    public static ModConfigSpec.LongValue CALCITE_SECOND;
    public static ModConfigSpec.LongValue CALCITE_STEP;

    public static ModConfigSpec.LongValue TUFF_MIN;
    public static ModConfigSpec.LongValue TUFF_MAX;
    public static ModConfigSpec.LongValue TUFF_SECOND;
    public static ModConfigSpec.LongValue TUFF_STEP;

    public static ModConfigSpec.LongValue COBBLED_DEEPSLATE_MIN;
    public static ModConfigSpec.LongValue COBBLED_DEEPSLATE_MAX;
    public static ModConfigSpec.LongValue COBBLED_DEEPSLATE_SECOND;
    public static ModConfigSpec.LongValue COBBLED_DEEPSLATE_STEP;

    public static ModConfigSpec.LongValue PRISMARINE_MIN;
    public static ModConfigSpec.LongValue PRISMARINE_MAX;
    public static ModConfigSpec.LongValue PRISMARINE_SECOND;
    public static ModConfigSpec.LongValue PRISMARINE_STEP;

    public static ModConfigSpec.LongValue OBSIDIAN_MIN;
    public static ModConfigSpec.LongValue OBSIDIAN_MAX;
    public static ModConfigSpec.LongValue OBSIDIAN_SECOND;
    public static ModConfigSpec.LongValue OBSIDIAN_STEP;

    public static ModConfigSpec.LongValue NETHERRACK_MIN;
    public static ModConfigSpec.LongValue NETHERRACK_MAX;
    public static ModConfigSpec.LongValue NETHERRACK_SECOND;
    public static ModConfigSpec.LongValue NETHERRACK_STEP;

    public static ModConfigSpec.LongValue SOUL_SAND_MIN;
    public static ModConfigSpec.LongValue SOUL_SAND_MAX;
    public static ModConfigSpec.LongValue SOUL_SAND_SECOND;
    public static ModConfigSpec.LongValue SOUL_SAND_STEP;

    public static ModConfigSpec.LongValue SOUL_SOIL_MIN;
    public static ModConfigSpec.LongValue SOUL_SOIL_MAX;
    public static ModConfigSpec.LongValue SOUL_SOIL_SECOND;
    public static ModConfigSpec.LongValue SOUL_SOIL_STEP;

    public static ModConfigSpec.LongValue BLACKSTONE_MIN;
    public static ModConfigSpec.LongValue BLACKSTONE_MAX;
    public static ModConfigSpec.LongValue BLACKSTONE_SECOND;
    public static ModConfigSpec.LongValue BLACKSTONE_STEP;

    public static ModConfigSpec.LongValue BASALT_MIN;
    public static ModConfigSpec.LongValue BASALT_MAX;
    public static ModConfigSpec.LongValue BASALT_SECOND;
    public static ModConfigSpec.LongValue BASALT_STEP;

    public static ModConfigSpec.LongValue END_STONE_MIN;
    public static ModConfigSpec.LongValue END_STONE_MAX;
    public static ModConfigSpec.LongValue END_STONE_SECOND;
    public static ModConfigSpec.LongValue END_STONE_STEP;

    public static ModConfigSpec SERVER_CONFIG;

    static {
        ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();

        SERVER_BUILDER.comment("Energy Generator Settings").push("Energy");

        SERVER_BUILDER.push("FE");
        FE_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.").defineInRange("min", 1, 1, Long.MAX_VALUE);
        FE_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        FE_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 1, 1, Long.MAX_VALUE);
        FE_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.").defineInRange("step", 1, 0, Long.MAX_VALUE);
        FE_BEACON_STEP = SERVER_BUILDER.comment("Control the proportion of each production increase affected by the beacon.The actual data needs to be divided by 10000.").defineInRange("beacon_step", 100, 0, Long.MAX_VALUE);
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

        SERVER_BUILDER.push("Dirt");
        DIRT_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        DIRT_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        DIRT_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        DIRT_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Cobblestone");
        COBBLESTONE_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        COBBLESTONE_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        COBBLESTONE_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        COBBLESTONE_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Stone");
        STONE_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        STONE_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        STONE_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        STONE_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Smooth Stone");
        SMOOTH_STONE_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        SMOOTH_STONE_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        SMOOTH_STONE_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        SMOOTH_STONE_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Clay");
        CLAY_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        CLAY_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        CLAY_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        CLAY_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Sand");
        SAND_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        SAND_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        SAND_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        SAND_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Gravel");
        GRAVEL_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        GRAVEL_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        GRAVEL_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        GRAVEL_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Granite");
        GRANITE_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        GRANITE_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        GRANITE_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        GRANITE_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Diorite");
        DIORITE_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        DIORITE_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        DIORITE_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        DIORITE_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Andesite");
        ANDESITE_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        ANDESITE_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        ANDESITE_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        ANDESITE_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Calcite");
        CALCITE_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        CALCITE_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        CALCITE_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        CALCITE_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Tuff");
        TUFF_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        TUFF_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        TUFF_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        TUFF_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Cobbled Deepslate");
        COBBLED_DEEPSLATE_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        COBBLED_DEEPSLATE_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        COBBLED_DEEPSLATE_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        COBBLED_DEEPSLATE_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Prismarine");
        PRISMARINE_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        PRISMARINE_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        PRISMARINE_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        PRISMARINE_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Obsidian");
        OBSIDIAN_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        OBSIDIAN_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        OBSIDIAN_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        OBSIDIAN_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Netherrack");
        NETHERRACK_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        NETHERRACK_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        NETHERRACK_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        NETHERRACK_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Soul Sand");
        SOUL_SAND_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        SOUL_SAND_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        SOUL_SAND_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        SOUL_SAND_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Soul Soil");
        SOUL_SOIL_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        SOUL_SOIL_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        SOUL_SOIL_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        SOUL_SOIL_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Blackstone");
        BLACKSTONE_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        BLACKSTONE_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        BLACKSTONE_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        BLACKSTONE_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("Basalt");
        BASALT_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        BASALT_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        BASALT_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        BASALT_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.push("End Stone");
        END_STONE_MIN = SERVER_BUILDER.comment("Control the minimum rate of production.The actual data needs to be divided by 1000.").defineInRange("min", 50, 1, Long.MAX_VALUE);
        END_STONE_MAX = SERVER_BUILDER.comment("Control the maximum rate of production.The actual data needs to be divided by 1000.").defineInRange("max", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        END_STONE_SECOND = SERVER_BUILDER.comment("Control the number of seconds it takes to increase production each time.").defineInRange("second", 10, 1, Long.MAX_VALUE);
        END_STONE_STEP = SERVER_BUILDER.comment("Control the numerical increase in production each time.The actual data needs to be divided by 1000.").defineInRange("step", 50, 0, Long.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.pop();

        SERVER_CONFIG = SERVER_BUILDER.build();
    }

    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG);
    }
}
