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
import cn.sd.jrz.autoresource.menu.BlockGeneratorMenu;
import cn.sd.jrz.autoresource.menu.EnergyGeneratorMenu;
import cn.sd.jrz.autoresource.menu.LiquidGeneratorMenu;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class Registration {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AutoResource.MODID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.Blocks.createBlocks(AutoResource.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.Items.createItems(AutoResource.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AutoResource.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AutoResource.MODID);

    public static void init(IEventBus bus) {
        DATA_COMPONENT_TYPES.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        bus.addListener(Registration::initCapabilities);
    }

    private static void initCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ENERGY_GENERATOR_FE_ENTITY.get(), (entity, direction) -> new EnergyConnection(entity));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, LIQUID_GENERATOR_WATER_ENTITY.get(), (entity, direction) -> new LiquidConnection(entity));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, LIQUID_GENERATOR_LAVA_ENTITY.get(), (entity, direction) -> new LiquidConnection(entity));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BLOCK_GENERATOR_ENTITY.get(), (entity, direction) -> new BlockConnection(entity));
    }

    private static final BlockBehaviour.Properties BLOCK_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(DyeColor.BLUE)
            .pushReaction(PushReaction.DESTROY)
            .strength(0.5f, 3.0f)
            .lightLevel(state -> 7);

    // DataComponentType

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BLOCK_DATA = DATA_COMPONENT_TYPES.register("block_data", () -> DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

    // Blocks

    public static final DeferredHolder<Block, EnergyGeneratorBlock> ENERGY_GENERATOR_FE = BLOCKS.register("energy_generator_fe", () -> new EnergyGeneratorBlock(BLOCK_PROPERTIES, DataConfig.ENERGY_GENERATOR_FE));
    public static final DeferredHolder<Block, LiquidGeneratorBlock> LIQUID_GENERATOR_WATER = BLOCKS.register("liquid_generator_water", () -> new LiquidGeneratorBlock(BLOCK_PROPERTIES, DataConfig.LIQUID_GENERATOR_WATER));
    public static final DeferredHolder<Block, LiquidGeneratorBlock> LIQUID_GENERATOR_LAVA = BLOCKS.register("liquid_generator_lava", () -> new LiquidGeneratorBlock(BLOCK_PROPERTIES, DataConfig.LIQUID_GENERATOR_LAVA));
    public static final DeferredHolder<Block, BlockGeneratorBlock> BLOCK_GENERATOR = BLOCKS.register("block_generator", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR));

    // Items

    public static final DeferredHolder<Item, EnergyGeneratorItem> ENERGY_GENERATOR_FE_ITEM = ITEMS.register("energy_generator_fe", () -> new EnergyGeneratorItem(ENERGY_GENERATOR_FE.get(), DataConfig.ENERGY_GENERATOR_FE));
    public static final DeferredHolder<Item, LiquidGeneratorItem> LIQUID_GENERATOR_WATER_ITEM = ITEMS.register("liquid_generator_water", () -> new LiquidGeneratorItem(LIQUID_GENERATOR_WATER.get(), DataConfig.LIQUID_GENERATOR_WATER));
    public static final DeferredHolder<Item, LiquidGeneratorItem> LIQUID_GENERATOR_LAVA_ITEM = ITEMS.register("liquid_generator_lava", () -> new LiquidGeneratorItem(LIQUID_GENERATOR_LAVA.get(), DataConfig.LIQUID_GENERATOR_LAVA));
    public static final DeferredHolder<Item, BlockGeneratorItem> BLOCK_GENERATOR_ITEM = ITEMS.register("block_generator", () -> new BlockGeneratorItem(BLOCK_GENERATOR.get(), DataConfig.BLOCK_GENERATOR));

    // Tile Entities

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyGeneratorEntity>> ENERGY_GENERATOR_FE_ENTITY = BLOCK_ENTITIES.register("energy_generator_fe", () -> BlockEntityType.Builder.of((pos, state) -> new EnergyGeneratorEntity(pos, state, DataConfig.ENERGY_GENERATOR_FE), ENERGY_GENERATOR_FE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiquidGeneratorEntity>> LIQUID_GENERATOR_WATER_ENTITY = BLOCK_ENTITIES.register("liquid_generator_water", () -> BlockEntityType.Builder.of((pos, state) -> new LiquidGeneratorEntity(pos, state, DataConfig.LIQUID_GENERATOR_WATER), LIQUID_GENERATOR_WATER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiquidGeneratorEntity>> LIQUID_GENERATOR_LAVA_ENTITY = BLOCK_ENTITIES.register("liquid_generator_lava", () -> BlockEntityType.Builder.of((pos, state) -> new LiquidGeneratorEntity(pos, state, DataConfig.LIQUID_GENERATOR_LAVA), LIQUID_GENERATOR_LAVA.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_ENTITY = BLOCK_ENTITIES.register("block_generator", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR), BLOCK_GENERATOR.get()).build(null));

    // Menus

    public static final DeferredHolder<MenuType<?>, MenuType<EnergyGeneratorMenu>> ENERGY_GENERATOR_MENU = MENUS.register("energy_generator", () -> IMenuTypeExtension.create((id, inv, buf) -> new EnergyGeneratorMenu(id, inv, buf.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<LiquidGeneratorMenu>> LIQUID_GENERATOR_MENU = MENUS.register("liquid_generator", () -> IMenuTypeExtension.create((id, inv, buf) -> new LiquidGeneratorMenu(id, inv, buf.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<BlockGeneratorMenu>> BLOCK_GENERATOR_MENU = MENUS.register("block_generator", () -> IMenuTypeExtension.create((id, inv, buf) -> new BlockGeneratorMenu(id, inv, buf.readBlockPos())));
}
