package cn.sd.jrz.autoresource.setup;

import cn.sd.jrz.autoresource.AutoResource;
import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.blocks.BlockGeneratorBlock;
import cn.sd.jrz.autoresource.blocks.EnergyGeneratorBlock;
import cn.sd.jrz.autoresource.blocks.LiquidGeneratorBlock;
import cn.sd.jrz.autoresource.entities.BlockGeneratorEntity;
import cn.sd.jrz.autoresource.entities.EnergyGeneratorEntity;
import cn.sd.jrz.autoresource.entities.LiquidGeneratorEntity;
import cn.sd.jrz.autoresource.items.BlockGeneratorItem;
import cn.sd.jrz.autoresource.items.EnergyGeneratorItem;
import cn.sd.jrz.autoresource.items.LiquidGeneratorItem;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Registration {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AutoResource.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AutoResource.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AutoResource.MODID);
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, AutoResource.MODID);

    public static void init(FMLJavaModLoadingContext context) {
        BLOCKS.register(context.getModEventBus());
        ITEMS.register(context.getModEventBus());
        BLOCK_ENTITIES.register(context.getModEventBus());
        CONTAINERS.register(context.getModEventBus());
    }

    private static final BlockBehaviour.Properties BLOCK_PROPERTIES = BlockBehaviour.Properties.of(
            new Material(MaterialColor.COLOR_BLUE, false, true, true, true, false, false, PushReaction.DESTROY)
    ).strength(2.5f, 15.0f);

    // Blocks

    public static final RegistryObject<Block> ENERGY_GENERATOR_FE = BLOCKS.register("energy_generator_fe", () -> new EnergyGeneratorBlock(BLOCK_PROPERTIES, DataConfig.ENERGY_GENERATOR_FE));
    public static final RegistryObject<Block> LIQUID_GENERATOR_WATER = BLOCKS.register("liquid_generator_water", () -> new LiquidGeneratorBlock(BLOCK_PROPERTIES, DataConfig.LIQUID_GENERATOR_WATER));
    public static final RegistryObject<Block> LIQUID_GENERATOR_LAVA = BLOCKS.register("liquid_generator_lava", () -> new LiquidGeneratorBlock(BLOCK_PROPERTIES, DataConfig.LIQUID_GENERATOR_LAVA));
    public static final RegistryObject<Block> BLOCK_GENERATOR_DIRT = BLOCKS.register("block_generator_dirt", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_DIRT));
    public static final RegistryObject<Block> BLOCK_GENERATOR_COBBLESTONE = BLOCKS.register("block_generator_cobblestone", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_COBBLESTONE));
    public static final RegistryObject<Block> BLOCK_GENERATOR_STONE = BLOCKS.register("block_generator_stone", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_STONE));
    public static final RegistryObject<Block> BLOCK_GENERATOR_SMOOTH_STONE = BLOCKS.register("block_generator_smooth_stone", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_SMOOTH_STONE));
    public static final RegistryObject<Block> BLOCK_GENERATOR_CLAY = BLOCKS.register("block_generator_clay", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_CLAY));
    public static final RegistryObject<Block> BLOCK_GENERATOR_SAND = BLOCKS.register("block_generator_sand", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_SAND));
    public static final RegistryObject<Block> BLOCK_GENERATOR_GRAVEL = BLOCKS.register("block_generator_gravel", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_GRAVEL));
    public static final RegistryObject<Block> BLOCK_GENERATOR_GRANITE = BLOCKS.register("block_generator_granite", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_GRANITE));
    public static final RegistryObject<Block> BLOCK_GENERATOR_DIORITE = BLOCKS.register("block_generator_diorite", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_DIORITE));
    public static final RegistryObject<Block> BLOCK_GENERATOR_ANDESITE = BLOCKS.register("block_generator_andesite", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_ANDESITE));
    public static final RegistryObject<Block> BLOCK_GENERATOR_CALCITE = BLOCKS.register("block_generator_calcite", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_CALCITE));
    public static final RegistryObject<Block> BLOCK_GENERATOR_TUFF = BLOCKS.register("block_generator_tuff", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_TUFF));
    public static final RegistryObject<Block> BLOCK_GENERATOR_COBBLED_DEEPSLATE = BLOCKS.register("block_generator_cobbled_deepslate", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_COBBLED_DEEPSLATE));
    public static final RegistryObject<Block> BLOCK_GENERATOR_PRISMARINE = BLOCKS.register("block_generator_prismarine", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_PRISMARINE));
    public static final RegistryObject<Block> BLOCK_GENERATOR_OBSIDIAN = BLOCKS.register("block_generator_obsidian", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_OBSIDIAN));
    public static final RegistryObject<Block> BLOCK_GENERATOR_NETHERRACK = BLOCKS.register("block_generator_netherrack", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_NETHERRACK));
    public static final RegistryObject<Block> BLOCK_GENERATOR_SOUL_SAND = BLOCKS.register("block_generator_soul_sand", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_SOUL_SAND));
    public static final RegistryObject<Block> BLOCK_GENERATOR_SOUL_SOIL = BLOCKS.register("block_generator_soul_soil", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_SOUL_SOIL));
    public static final RegistryObject<Block> BLOCK_GENERATOR_BLACKSTONE = BLOCKS.register("block_generator_blackstone", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_BLACKSTONE));
    public static final RegistryObject<Block> BLOCK_GENERATOR_BASALT = BLOCKS.register("block_generator_basalt", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_BASALT));
    public static final RegistryObject<Block> BLOCK_GENERATOR_END_STONE = BLOCKS.register("block_generator_end_stone", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR_END_STONE));

    // Items

    public static final RegistryObject<Item> ENERGY_GENERATOR_FE_ITEM = ITEMS.register("energy_generator_fe", () -> new EnergyGeneratorItem(ENERGY_GENERATOR_FE.get(), DataConfig.ENERGY_GENERATOR_FE));
    public static final RegistryObject<Item> LIQUID_GENERATOR_WATER_ITEM = ITEMS.register("liquid_generator_water", () -> new LiquidGeneratorItem(LIQUID_GENERATOR_WATER.get(), DataConfig.LIQUID_GENERATOR_WATER));
    public static final RegistryObject<Item> LIQUID_GENERATOR_LAVA_ITEM = ITEMS.register("liquid_generator_lava", () -> new LiquidGeneratorItem(LIQUID_GENERATOR_LAVA.get(), DataConfig.LIQUID_GENERATOR_LAVA));
    public static final RegistryObject<Item> BLOCK_GENERATOR_DIRT_ITEM = ITEMS.register("block_generator_dirt", () -> new BlockGeneratorItem(BLOCK_GENERATOR_DIRT.get(), DataConfig.BLOCK_GENERATOR_DIRT));
    public static final RegistryObject<Item> BLOCK_GENERATOR_COBBLESTONE_ITEM = ITEMS.register("block_generator_cobblestone", () -> new BlockGeneratorItem(BLOCK_GENERATOR_COBBLESTONE.get(), DataConfig.BLOCK_GENERATOR_COBBLESTONE));
    public static final RegistryObject<Item> BLOCK_GENERATOR_STONE_ITEM = ITEMS.register("block_generator_stone", () -> new BlockGeneratorItem(BLOCK_GENERATOR_STONE.get(), DataConfig.BLOCK_GENERATOR_STONE));
    public static final RegistryObject<Item> BLOCK_GENERATOR_SMOOTH_STONE_ITEM = ITEMS.register("block_generator_smooth_stone", () -> new BlockGeneratorItem(BLOCK_GENERATOR_SMOOTH_STONE.get(), DataConfig.BLOCK_GENERATOR_SMOOTH_STONE));
    public static final RegistryObject<Item> BLOCK_GENERATOR_CLAY_ITEM = ITEMS.register("block_generator_clay", () -> new BlockGeneratorItem(BLOCK_GENERATOR_CLAY.get(), DataConfig.BLOCK_GENERATOR_CLAY));
    public static final RegistryObject<Item> BLOCK_GENERATOR_SAND_ITEM = ITEMS.register("block_generator_sand", () -> new BlockGeneratorItem(BLOCK_GENERATOR_SAND.get(), DataConfig.BLOCK_GENERATOR_SAND));
    public static final RegistryObject<Item> BLOCK_GENERATOR_GRAVEL_ITEM = ITEMS.register("block_generator_gravel", () -> new BlockGeneratorItem(BLOCK_GENERATOR_GRAVEL.get(), DataConfig.BLOCK_GENERATOR_GRAVEL));
    public static final RegistryObject<Item> BLOCK_GENERATOR_GRANITE_ITEM = ITEMS.register("block_generator_granite", () -> new BlockGeneratorItem(BLOCK_GENERATOR_GRANITE.get(), DataConfig.BLOCK_GENERATOR_GRANITE));
    public static final RegistryObject<Item> BLOCK_GENERATOR_DIORITE_ITEM = ITEMS.register("block_generator_diorite", () -> new BlockGeneratorItem(BLOCK_GENERATOR_DIORITE.get(), DataConfig.BLOCK_GENERATOR_DIORITE));
    public static final RegistryObject<Item> BLOCK_GENERATOR_ANDESITE_ITEM = ITEMS.register("block_generator_andesite", () -> new BlockGeneratorItem(BLOCK_GENERATOR_ANDESITE.get(), DataConfig.BLOCK_GENERATOR_ANDESITE));
    public static final RegistryObject<Item> BLOCK_GENERATOR_CALCITE_ITEM = ITEMS.register("block_generator_calcite", () -> new BlockGeneratorItem(BLOCK_GENERATOR_CALCITE.get(), DataConfig.BLOCK_GENERATOR_CALCITE));
    public static final RegistryObject<Item> BLOCK_GENERATOR_TUFF_ITEM = ITEMS.register("block_generator_tuff", () -> new BlockGeneratorItem(BLOCK_GENERATOR_TUFF.get(), DataConfig.BLOCK_GENERATOR_TUFF));
    public static final RegistryObject<Item> BLOCK_GENERATOR_COBBLED_DEEPSLATE_ITEM = ITEMS.register("block_generator_cobbled_deepslate", () -> new BlockGeneratorItem(BLOCK_GENERATOR_COBBLED_DEEPSLATE.get(), DataConfig.BLOCK_GENERATOR_COBBLED_DEEPSLATE));
    public static final RegistryObject<Item> BLOCK_GENERATOR_PRISMARINE_ITEM = ITEMS.register("block_generator_prismarine", () -> new BlockGeneratorItem(BLOCK_GENERATOR_PRISMARINE.get(), DataConfig.BLOCK_GENERATOR_PRISMARINE));
    public static final RegistryObject<Item> BLOCK_GENERATOR_OBSIDIAN_ITEM = ITEMS.register("block_generator_obsidian", () -> new BlockGeneratorItem(BLOCK_GENERATOR_OBSIDIAN.get(), DataConfig.BLOCK_GENERATOR_OBSIDIAN));
    public static final RegistryObject<Item> BLOCK_GENERATOR_NETHERRACK_ITEM = ITEMS.register("block_generator_netherrack", () -> new BlockGeneratorItem(BLOCK_GENERATOR_NETHERRACK.get(), DataConfig.BLOCK_GENERATOR_NETHERRACK));
    public static final RegistryObject<Item> BLOCK_GENERATOR_SOUL_SAND_ITEM = ITEMS.register("block_generator_soul_sand", () -> new BlockGeneratorItem(BLOCK_GENERATOR_SOUL_SAND.get(), DataConfig.BLOCK_GENERATOR_SOUL_SAND));
    public static final RegistryObject<Item> BLOCK_GENERATOR_SOUL_SOIL_ITEM = ITEMS.register("block_generator_soul_soil", () -> new BlockGeneratorItem(BLOCK_GENERATOR_SOUL_SOIL.get(), DataConfig.BLOCK_GENERATOR_SOUL_SOIL));
    public static final RegistryObject<Item> BLOCK_GENERATOR_BLACKSTONE_ITEM = ITEMS.register("block_generator_blackstone", () -> new BlockGeneratorItem(BLOCK_GENERATOR_BLACKSTONE.get(), DataConfig.BLOCK_GENERATOR_BLACKSTONE));
    public static final RegistryObject<Item> BLOCK_GENERATOR_BASALT_ITEM = ITEMS.register("block_generator_basalt", () -> new BlockGeneratorItem(BLOCK_GENERATOR_BASALT.get(), DataConfig.BLOCK_GENERATOR_BASALT));
    public static final RegistryObject<Item> BLOCK_GENERATOR_END_STONE_ITEM = ITEMS.register("block_generator_end_stone", () -> new BlockGeneratorItem(BLOCK_GENERATOR_END_STONE.get(), DataConfig.BLOCK_GENERATOR_END_STONE));

    // Tile Entities

    public static final RegistryObject<BlockEntityType<EnergyGeneratorEntity>> ENERGY_GENERATOR_FE_ENTITY = BLOCK_ENTITIES.register("energy_generator_fe", () -> BlockEntityType.Builder.of((pos, state) -> new EnergyGeneratorEntity(pos, state, DataConfig.ENERGY_GENERATOR_FE), ENERGY_GENERATOR_FE.get()).build(null));
    public static final RegistryObject<BlockEntityType<LiquidGeneratorEntity>> LIQUID_GENERATOR_WATER_ENTITY = BLOCK_ENTITIES.register("liquid_generator_water", () -> BlockEntityType.Builder.of((pos, state) -> new LiquidGeneratorEntity(pos, state, DataConfig.LIQUID_GENERATOR_WATER), LIQUID_GENERATOR_WATER.get()).build(null));
    public static final RegistryObject<BlockEntityType<LiquidGeneratorEntity>> LIQUID_GENERATOR_LAVA_ENTITY = BLOCK_ENTITIES.register("liquid_generator_lava", () -> BlockEntityType.Builder.of((pos, state) -> new LiquidGeneratorEntity(pos, state, DataConfig.LIQUID_GENERATOR_LAVA), LIQUID_GENERATOR_LAVA.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_DIRT_ENTITY = BLOCK_ENTITIES.register("block_generator_dirt", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_DIRT), BLOCK_GENERATOR_DIRT.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_COBBLESTONE_ENTITY = BLOCK_ENTITIES.register("block_generator_cobblestone", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_COBBLESTONE), BLOCK_GENERATOR_COBBLESTONE.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_STONE_ENTITY = BLOCK_ENTITIES.register("block_generator_stone", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_STONE), BLOCK_GENERATOR_STONE.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_SMOOTH_STONE_ENTITY = BLOCK_ENTITIES.register("block_generator_smooth_stone", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_SMOOTH_STONE), BLOCK_GENERATOR_SMOOTH_STONE.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_CLAY_ENTITY = BLOCK_ENTITIES.register("block_generator_clay", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_CLAY), BLOCK_GENERATOR_CLAY.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_SAND_ENTITY = BLOCK_ENTITIES.register("block_generator_sand", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_SAND), BLOCK_GENERATOR_SAND.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_GRAVEL_ENTITY = BLOCK_ENTITIES.register("block_generator_gravel", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_GRAVEL), BLOCK_GENERATOR_GRAVEL.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_GRANITE_ENTITY = BLOCK_ENTITIES.register("block_generator_granite", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_GRANITE), BLOCK_GENERATOR_GRANITE.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_DIORITE_ENTITY = BLOCK_ENTITIES.register("block_generator_diorite", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_DIORITE), BLOCK_GENERATOR_DIORITE.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_ANDESITE_ENTITY = BLOCK_ENTITIES.register("block_generator_andesite", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_ANDESITE), BLOCK_GENERATOR_ANDESITE.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_CALCITE_ENTITY = BLOCK_ENTITIES.register("block_generator_calcite", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_CALCITE), BLOCK_GENERATOR_CALCITE.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_TUFF_ENTITY = BLOCK_ENTITIES.register("block_generator_tuff", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_TUFF), BLOCK_GENERATOR_TUFF.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_COBBLED_DEEPSLATE_ENTITY = BLOCK_ENTITIES.register("block_generator_cobbled_deepslate", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_COBBLED_DEEPSLATE), BLOCK_GENERATOR_COBBLED_DEEPSLATE.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_PRISMARINE_ENTITY = BLOCK_ENTITIES.register("block_generator_prismarine", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_PRISMARINE), BLOCK_GENERATOR_PRISMARINE.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_OBSIDIAN_ENTITY = BLOCK_ENTITIES.register("block_generator_obsidian", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_OBSIDIAN), BLOCK_GENERATOR_OBSIDIAN.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_NETHERRACK_ENTITY = BLOCK_ENTITIES.register("block_generator_netherrack", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_NETHERRACK), BLOCK_GENERATOR_NETHERRACK.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_SOUL_SAND_ENTITY = BLOCK_ENTITIES.register("block_generator_soul_sand", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_SOUL_SAND), BLOCK_GENERATOR_SOUL_SAND.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_SOUL_SOIL_ENTITY = BLOCK_ENTITIES.register("block_generator_soul_soil", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_SOUL_SOIL), BLOCK_GENERATOR_SOUL_SOIL.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_BLACKSTONE_ENTITY = BLOCK_ENTITIES.register("block_generator_blackstone", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_BLACKSTONE), BLOCK_GENERATOR_BLACKSTONE.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_BASALT_ENTITY = BLOCK_ENTITIES.register("block_generator_basalt", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_BASALT), BLOCK_GENERATOR_BASALT.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_END_STONE_ENTITY = BLOCK_ENTITIES.register("block_generator_end_stone", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR_END_STONE), BLOCK_GENERATOR_END_STONE.get()).build(null));
}
