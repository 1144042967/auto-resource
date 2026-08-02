package cn.sd.jrz.autoresource;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class Config {

    public static ForgeConfigSpec.LongValue FE_MIN;
    public static ForgeConfigSpec.LongValue FE_MAX;
    public static ForgeConfigSpec.LongValue FE_SECOND;
    public static ForgeConfigSpec.LongValue FE_STEP;
    public static ForgeConfigSpec.ConfigValue<String> FE_STAR_ITEM;

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
        SERVER_BUILDER.pop();

        SERVER_BUILDER.pop();

        SERVER_CONFIG = SERVER_BUILDER.build();
    }

    public static void init(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG);
    }
}
