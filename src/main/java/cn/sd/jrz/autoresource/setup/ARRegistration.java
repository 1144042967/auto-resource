package cn.sd.jrz.autoresource.setup;

import cn.sd.jrz.autoresource.AutoResource;
import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.blocks.BlockGeneratorBlock;
import cn.sd.jrz.autoresource.blocks.EnergyGeneratorBlock;
import cn.sd.jrz.autoresource.blocks.LiquidGeneratorBlock;
import cn.sd.jrz.autoresource.connection.BlockConnection;
import cn.sd.jrz.autoresource.connection.EnergyConnection;
import cn.sd.jrz.autoresource.connection.LiquidConnection;
import cn.sd.jrz.autoresource.entities.BlockGeneratorEntity;
import cn.sd.jrz.autoresource.entities.EnergyGeneratorEntity;
import cn.sd.jrz.autoresource.entities.LiquidGeneratorEntity;
import cn.sd.jrz.autoresource.items.BlockGeneratorItem;
import cn.sd.jrz.autoresource.items.EnergyGeneratorItem;
import cn.sd.jrz.autoresource.items.LiquidGeneratorItem;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiFunction;

public class ARRegistration {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AutoResource.MODID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.Blocks.createBlocks(AutoResource.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.Items.createItems(AutoResource.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AutoResource.MODID);

    public static void init(IEventBus bus) {
        DATA_COMPONENT_TYPES.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(ARRegistration::initCapabilities);
    }

    private static void initCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ENERGY_GENERATOR_FE_ENTITY.get(), (entity, direction) -> new EnergyConnection(entity));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, LIQUID_GENERATOR_WATER_ENTITY.get(), (entity, direction) -> new LiquidConnection(entity));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, LIQUID_GENERATOR_LAVA_ENTITY.get(), (entity, direction) -> new LiquidConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_DIRT_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_COBBLESTONE_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_STONE_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_SMOOTH_STONE_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_CLAY_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_SAND_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_GRAVEL_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_GRANITE_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_DIORITE_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_ANDESITE_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_CALCITE_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_TUFF_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_COBBLED_DEEPSLATE_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_PRISMARINE_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_OBSIDIAN_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_NETHERRACK_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_SOUL_SAND_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_SOUL_SOIL_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_BLACKSTONE_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_BASALT_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_END_STONE_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
    }

    // DataComponentType

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BLOCK_DATA = DATA_COMPONENT_TYPES.register("block_data", () -> DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

    // Blocks

    public static final DeferredHolder<Block, EnergyGeneratorBlock> ENERGY_GENERATOR_FE = registerBlock("energy_generator_fe", DataConfig.ENERGY_GENERATOR_FE, EnergyGeneratorBlock::new);
    public static final DeferredHolder<Block, LiquidGeneratorBlock> LIQUID_GENERATOR_WATER = registerBlock("liquid_generator_water", DataConfig.LIQUID_GENERATOR_WATER, LiquidGeneratorBlock::new);
    public static final DeferredHolder<Block, LiquidGeneratorBlock> LIQUID_GENERATOR_LAVA = registerBlock("liquid_generator_lava", DataConfig.LIQUID_GENERATOR_LAVA, LiquidGeneratorBlock::new);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_DIRT = registerBlockBlock("block_generator_dirt", DataConfig.BLOCK_GENERATOR_DIRT);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_COBBLESTONE = registerBlockBlock("block_generator_cobblestone", DataConfig.BLOCK_GENERATOR_COBBLESTONE);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_STONE = registerBlockBlock("block_generator_stone", DataConfig.BLOCK_GENERATOR_STONE);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_SMOOTH_STONE = registerBlockBlock("block_generator_smooth_stone", DataConfig.BLOCK_GENERATOR_SMOOTH_STONE);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_CLAY = registerBlockBlock("block_generator_clay", DataConfig.BLOCK_GENERATOR_CLAY);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_SAND = registerBlockBlock("block_generator_sand", DataConfig.BLOCK_GENERATOR_SAND);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_GRAVEL = registerBlockBlock("block_generator_gravel", DataConfig.BLOCK_GENERATOR_GRAVEL);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_GRANITE = registerBlockBlock("block_generator_granite", DataConfig.BLOCK_GENERATOR_GRANITE);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_DIORITE = registerBlockBlock("block_generator_diorite", DataConfig.BLOCK_GENERATOR_DIORITE);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_ANDESITE = registerBlockBlock("block_generator_andesite", DataConfig.BLOCK_GENERATOR_ANDESITE);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_CALCITE = registerBlockBlock("block_generator_calcite", DataConfig.BLOCK_GENERATOR_CALCITE);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_TUFF = registerBlockBlock("block_generator_tuff", DataConfig.BLOCK_GENERATOR_TUFF);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_COBBLED_DEEPSLATE = registerBlockBlock("block_generator_cobbled_deepslate", DataConfig.BLOCK_GENERATOR_COBBLED_DEEPSLATE);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_PRISMARINE = registerBlockBlock("block_generator_prismarine", DataConfig.BLOCK_GENERATOR_PRISMARINE);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_OBSIDIAN = registerBlockBlock("block_generator_obsidian", DataConfig.BLOCK_GENERATOR_OBSIDIAN);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_NETHERRACK = registerBlockBlock("block_generator_netherrack", DataConfig.BLOCK_GENERATOR_NETHERRACK);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_SOUL_SAND = registerBlockBlock("block_generator_soul_sand", DataConfig.BLOCK_GENERATOR_SOUL_SAND);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_SOUL_SOIL = registerBlockBlock("block_generator_soul_soil", DataConfig.BLOCK_GENERATOR_SOUL_SOIL);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_BLACKSTONE = registerBlockBlock("block_generator_blackstone", DataConfig.BLOCK_GENERATOR_BLACKSTONE);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_BASALT = registerBlockBlock("block_generator_basalt", DataConfig.BLOCK_GENERATOR_BASALT);
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR_END_STONE = registerBlockBlock("block_generator_end_stone", DataConfig.BLOCK_GENERATOR_END_STONE);

    // Items

    public static final DeferredHolder<Item, EnergyGeneratorItem> ENERGY_GENERATOR_FE_ITEM = registerItem("energy_generator_fe", DataConfig.ENERGY_GENERATOR_FE, ENERGY_GENERATOR_FE, EnergyGeneratorItem::new);
    public static final DeferredHolder<Item, LiquidGeneratorItem> LIQUID_GENERATOR_WATER_ITEM = registerItem("liquid_generator_water", DataConfig.LIQUID_GENERATOR_WATER, LIQUID_GENERATOR_WATER, LiquidGeneratorItem::new);
    public static final DeferredHolder<Item, LiquidGeneratorItem> LIQUID_GENERATOR_LAVA_ITEM = registerItem("liquid_generator_lava", DataConfig.LIQUID_GENERATOR_LAVA, LIQUID_GENERATOR_LAVA, LiquidGeneratorItem::new);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_DIRT_ITEM = registerBlockItem("block_generator_dirt", DataConfig.BLOCK_GENERATOR_DIRT, BLOCK_GENERATOR_DIRT);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_COBBLESTONE_ITEM = registerBlockItem("block_generator_cobblestone", DataConfig.BLOCK_GENERATOR_COBBLESTONE, BLOCK_GENERATOR_COBBLESTONE);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_STONE_ITEM = registerBlockItem("block_generator_stone", DataConfig.BLOCK_GENERATOR_STONE, BLOCK_GENERATOR_STONE);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_SMOOTH_STONE_ITEM = registerBlockItem("block_generator_smooth_stone", DataConfig.BLOCK_GENERATOR_SMOOTH_STONE, BLOCK_GENERATOR_SMOOTH_STONE);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_CLAY_ITEM = registerBlockItem("block_generator_clay", DataConfig.BLOCK_GENERATOR_CLAY, BLOCK_GENERATOR_CLAY);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_SAND_ITEM = registerBlockItem("block_generator_sand", DataConfig.BLOCK_GENERATOR_SAND, BLOCK_GENERATOR_SAND);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_GRAVEL_ITEM = registerBlockItem("block_generator_gravel", DataConfig.BLOCK_GENERATOR_GRAVEL, BLOCK_GENERATOR_GRAVEL);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_GRANITE_ITEM = registerBlockItem("block_generator_granite", DataConfig.BLOCK_GENERATOR_GRANITE, BLOCK_GENERATOR_GRANITE);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_DIORITE_ITEM = registerBlockItem("block_generator_diorite", DataConfig.BLOCK_GENERATOR_DIORITE, BLOCK_GENERATOR_DIORITE);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_ANDESITE_ITEM = registerBlockItem("block_generator_andesite", DataConfig.BLOCK_GENERATOR_ANDESITE, BLOCK_GENERATOR_ANDESITE);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_CALCITE_ITEM = registerBlockItem("block_generator_calcite", DataConfig.BLOCK_GENERATOR_CALCITE, BLOCK_GENERATOR_CALCITE);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_TUFF_ITEM = registerBlockItem("block_generator_tuff", DataConfig.BLOCK_GENERATOR_TUFF, BLOCK_GENERATOR_TUFF);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_COBBLED_DEEPSLATE_ITEM = registerBlockItem("block_generator_cobbled_deepslate", DataConfig.BLOCK_GENERATOR_COBBLED_DEEPSLATE, BLOCK_GENERATOR_COBBLED_DEEPSLATE);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_PRISMARINE_ITEM = registerBlockItem("block_generator_prismarine", DataConfig.BLOCK_GENERATOR_PRISMARINE, BLOCK_GENERATOR_PRISMARINE);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_OBSIDIAN_ITEM = registerBlockItem("block_generator_obsidian", DataConfig.BLOCK_GENERATOR_OBSIDIAN, BLOCK_GENERATOR_OBSIDIAN);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_NETHERRACK_ITEM = registerBlockItem("block_generator_netherrack", DataConfig.BLOCK_GENERATOR_NETHERRACK, BLOCK_GENERATOR_NETHERRACK);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_SOUL_SAND_ITEM = registerBlockItem("block_generator_soul_sand", DataConfig.BLOCK_GENERATOR_SOUL_SAND, BLOCK_GENERATOR_SOUL_SAND);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_SOUL_SOIL_ITEM = registerBlockItem("block_generator_soul_soil", DataConfig.BLOCK_GENERATOR_SOUL_SOIL, BLOCK_GENERATOR_SOUL_SOIL);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_BLACKSTONE_ITEM = registerBlockItem("block_generator_blackstone", DataConfig.BLOCK_GENERATOR_BLACKSTONE, BLOCK_GENERATOR_BLACKSTONE);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_BASALT_ITEM = registerBlockItem("block_generator_basalt", DataConfig.BLOCK_GENERATOR_BASALT, BLOCK_GENERATOR_BASALT);
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_END_STONE_ITEM = registerBlockItem("block_generator_end_stone", DataConfig.BLOCK_GENERATOR_END_STONE, BLOCK_GENERATOR_END_STONE);

    // Tile Entities

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyGeneratorEntity>> ENERGY_GENERATOR_FE_ENTITY = registerEntity("energy_generator_fe", DataConfig.ENERGY_GENERATOR_FE, ENERGY_GENERATOR_FE, EnergyGeneratorEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiquidGeneratorEntity>> LIQUID_GENERATOR_WATER_ENTITY = registerEntity("liquid_generator_water", DataConfig.LIQUID_GENERATOR_WATER, LIQUID_GENERATOR_WATER, LiquidGeneratorEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiquidGeneratorEntity>> LIQUID_GENERATOR_LAVA_ENTITY = registerEntity("liquid_generator_lava", DataConfig.LIQUID_GENERATOR_LAVA, LIQUID_GENERATOR_LAVA, LiquidGeneratorEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_DIRT_ENTITY = registerBlockEntity("block_generator_dirt", DataConfig.BLOCK_GENERATOR_DIRT, BLOCK_GENERATOR_DIRT);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_COBBLESTONE_ENTITY = registerBlockEntity("block_generator_cobblestone", DataConfig.BLOCK_GENERATOR_COBBLESTONE, BLOCK_GENERATOR_COBBLESTONE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_STONE_ENTITY = registerBlockEntity("block_generator_stone", DataConfig.BLOCK_GENERATOR_STONE, BLOCK_GENERATOR_STONE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_SMOOTH_STONE_ENTITY = registerBlockEntity("block_generator_smooth_stone", DataConfig.BLOCK_GENERATOR_SMOOTH_STONE, BLOCK_GENERATOR_SMOOTH_STONE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_CLAY_ENTITY = registerBlockEntity("block_generator_clay", DataConfig.BLOCK_GENERATOR_CLAY, BLOCK_GENERATOR_CLAY);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_SAND_ENTITY = registerBlockEntity("block_generator_sand", DataConfig.BLOCK_GENERATOR_SAND, BLOCK_GENERATOR_SAND);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_GRAVEL_ENTITY = registerBlockEntity("block_generator_gravel", DataConfig.BLOCK_GENERATOR_GRAVEL, BLOCK_GENERATOR_GRAVEL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_GRANITE_ENTITY = registerBlockEntity("block_generator_granite", DataConfig.BLOCK_GENERATOR_GRANITE, BLOCK_GENERATOR_GRANITE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_DIORITE_ENTITY = registerBlockEntity("block_generator_diorite", DataConfig.BLOCK_GENERATOR_DIORITE, BLOCK_GENERATOR_DIORITE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_ANDESITE_ENTITY = registerBlockEntity("block_generator_andesite", DataConfig.BLOCK_GENERATOR_ANDESITE, BLOCK_GENERATOR_ANDESITE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_CALCITE_ENTITY = registerBlockEntity("block_generator_calcite", DataConfig.BLOCK_GENERATOR_CALCITE, BLOCK_GENERATOR_CALCITE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_TUFF_ENTITY = registerBlockEntity("block_generator_tuff", DataConfig.BLOCK_GENERATOR_TUFF, BLOCK_GENERATOR_TUFF);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_COBBLED_DEEPSLATE_ENTITY = registerBlockEntity("block_generator_cobbled_deepslate", DataConfig.BLOCK_GENERATOR_COBBLED_DEEPSLATE, BLOCK_GENERATOR_COBBLED_DEEPSLATE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_PRISMARINE_ENTITY = registerBlockEntity("block_generator_prismarine", DataConfig.BLOCK_GENERATOR_PRISMARINE, BLOCK_GENERATOR_PRISMARINE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_OBSIDIAN_ENTITY = registerBlockEntity("block_generator_obsidian", DataConfig.BLOCK_GENERATOR_OBSIDIAN, BLOCK_GENERATOR_OBSIDIAN);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_NETHERRACK_ENTITY = registerBlockEntity("block_generator_netherrack", DataConfig.BLOCK_GENERATOR_NETHERRACK, BLOCK_GENERATOR_NETHERRACK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_SOUL_SAND_ENTITY = registerBlockEntity("block_generator_soul_sand", DataConfig.BLOCK_GENERATOR_SOUL_SAND, BLOCK_GENERATOR_SOUL_SAND);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_SOUL_SOIL_ENTITY = registerBlockEntity("block_generator_soul_soil", DataConfig.BLOCK_GENERATOR_SOUL_SOIL, BLOCK_GENERATOR_SOUL_SOIL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_BLACKSTONE_ENTITY = registerBlockEntity("block_generator_blackstone", DataConfig.BLOCK_GENERATOR_BLACKSTONE, BLOCK_GENERATOR_BLACKSTONE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_BASALT_ENTITY = registerBlockEntity("block_generator_basalt", DataConfig.BLOCK_GENERATOR_BASALT, BLOCK_GENERATOR_BASALT);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_END_STONE_ENTITY = registerBlockEntity("block_generator_end_stone", DataConfig.BLOCK_GENERATOR_END_STONE, BLOCK_GENERATOR_END_STONE);

    private static <T extends Block & EntityBlock> DeferredHolder<Block, T> registerBlock(String name, DataConfig config, BiFunction<BlockBehaviour.Properties, DataConfig, T> creator) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(DyeColor.BLUE).pushReaction(PushReaction.DESTROY).strength(2.5f, 15.0f).setId(ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(AutoResource.MODID, name)));
        return BLOCKS.register(name, () -> creator.apply(properties, config));
    }

    private static DeferredHolder<Block, BlockGeneratorBlock> registerBlockBlock(String name, DataConfig config) {
        return registerBlock(name, config, BlockGeneratorBlock::new);
    }

    private static <T extends BlockItem, B extends Block> DeferredHolder<Item, T> registerItem(String name, DataConfig config, DeferredHolder<Block, B> blockRegistry, ItemCreator<T> creator) {
        Item.Properties properties = new Item.Properties().stacksTo(1).fireResistant().setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(AutoResource.MODID, name)));
        return ITEMS.register(name, () -> creator.create(blockRegistry.get(), properties, config));
    }

    private static DeferredHolder<Item, BlockGeneratorItem> registerBlockItem(String name, DataConfig config, DeferredHolder<Block, BlockGeneratorBlock> blockRegistry) {
        return registerItem(name, config, blockRegistry, BlockGeneratorItem::new);
    }

    private static <T extends BlockEntity, B extends Block> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> registerEntity(String name, DataConfig config, DeferredHolder<Block, B> blockRegistry, EntityCreator<T> creator) {
        return BLOCK_ENTITIES.register(name, () -> new BlockEntityType<>((pos, state) -> creator.create(pos, state, config), blockRegistry.get()));
    }

    private static DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> registerBlockEntity(String name, DataConfig config, DeferredHolder<Block, BlockGeneratorBlock> blockRegistry) {
        return registerEntity(name, config, blockRegistry, BlockGeneratorEntity::new);
    }

    @FunctionalInterface
    private interface ItemCreator<T extends BlockItem> {
        T create(Block block, Item.Properties properties, DataConfig config);
    }

    @FunctionalInterface
    private interface EntityCreator<T> {
        T create(BlockPos pos, BlockState state, DataConfig config);
    }
}
