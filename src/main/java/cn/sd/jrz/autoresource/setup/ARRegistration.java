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
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
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
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public class ARRegistration {
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
        bus.addListener(ARRegistration::initCapabilities);
    }

    private static void initCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ENERGY_GENERATOR_FE_ENTITY.get(), (entity, _) -> new EnergyConnection(entity));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, LIQUID_GENERATOR_WATER_ENTITY.get(), (entity, _) -> new LiquidConnection(entity));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, LIQUID_GENERATOR_LAVA_ENTITY.get(), (entity, _) -> new LiquidConnection(entity));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BLOCK_GENERATOR_ENTITY.get(), (entity, _) -> new BlockConnection(entity));
    }

    // DataComponentType

    public static final DeferredHolder<DataComponentType<?>, @NotNull DataComponentType<String>> BLOCK_DATA = DATA_COMPONENT_TYPES.register("block_data", () -> DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

    // Blocks

    public static final DeferredHolder<Block, @NotNull EnergyGeneratorBlock> ENERGY_GENERATOR_FE = registerBlock("energy_generator_fe", DataConfig.ENERGY_GENERATOR_FE, EnergyGeneratorBlock::new);
    public static final DeferredHolder<Block, @NotNull LiquidGeneratorBlock> LIQUID_GENERATOR_WATER = registerBlock("liquid_generator_water", DataConfig.LIQUID_GENERATOR_WATER, LiquidGeneratorBlock::new);
    public static final DeferredHolder<Block, @NotNull LiquidGeneratorBlock> LIQUID_GENERATOR_LAVA = registerBlock("liquid_generator_lava", DataConfig.LIQUID_GENERATOR_LAVA, LiquidGeneratorBlock::new);
    public static final DeferredHolder<Block, @NotNull BlockGeneratorBlock> BLOCK_GENERATOR = registerBlock("block_generator", DataConfig.BLOCK_GENERATOR, BlockGeneratorBlock::new);

    // Items

    public static final DeferredHolder<Item, @NotNull EnergyGeneratorItem> ENERGY_GENERATOR_FE_ITEM = registerItem("energy_generator_fe", DataConfig.ENERGY_GENERATOR_FE, ENERGY_GENERATOR_FE, EnergyGeneratorItem::new);
    public static final DeferredHolder<Item, @NotNull LiquidGeneratorItem> LIQUID_GENERATOR_WATER_ITEM = registerItem("liquid_generator_water", DataConfig.LIQUID_GENERATOR_WATER, LIQUID_GENERATOR_WATER, LiquidGeneratorItem::new);
    public static final DeferredHolder<Item, @NotNull LiquidGeneratorItem> LIQUID_GENERATOR_LAVA_ITEM = registerItem("liquid_generator_lava", DataConfig.LIQUID_GENERATOR_LAVA, LIQUID_GENERATOR_LAVA, LiquidGeneratorItem::new);
    public static final DeferredHolder<Item, @NotNull BlockGeneratorItem> BLOCK_GENERATOR_ITEM = registerItem("block_generator", DataConfig.BLOCK_GENERATOR, BLOCK_GENERATOR, BlockGeneratorItem::new);

    // Tile Entities

    public static final DeferredHolder<BlockEntityType<?>, @NotNull BlockEntityType<@NotNull EnergyGeneratorEntity>> ENERGY_GENERATOR_FE_ENTITY = registerEntity("energy_generator_fe", DataConfig.ENERGY_GENERATOR_FE, ENERGY_GENERATOR_FE, EnergyGeneratorEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, @NotNull BlockEntityType<@NotNull LiquidGeneratorEntity>> LIQUID_GENERATOR_WATER_ENTITY = registerEntity("liquid_generator_water", DataConfig.LIQUID_GENERATOR_WATER, LIQUID_GENERATOR_WATER, LiquidGeneratorEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, @NotNull BlockEntityType<@NotNull LiquidGeneratorEntity>> LIQUID_GENERATOR_LAVA_ENTITY = registerEntity("liquid_generator_lava", DataConfig.LIQUID_GENERATOR_LAVA, LIQUID_GENERATOR_LAVA, LiquidGeneratorEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, @NotNull BlockEntityType<@NotNull BlockGeneratorEntity>> BLOCK_GENERATOR_ENTITY = registerEntity("block_generator", DataConfig.BLOCK_GENERATOR, BLOCK_GENERATOR, BlockGeneratorEntity::new);

    // Menus

    public static final DeferredHolder<MenuType<?>, @NotNull MenuType<@NotNull EnergyGeneratorMenu>> ENERGY_GENERATOR_MENU = MENUS.register("energy_generator", () -> IMenuTypeExtension.create((id, inv, buf) -> new EnergyGeneratorMenu(id, inv, buf.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, @NotNull MenuType<@NotNull LiquidGeneratorMenu>> LIQUID_GENERATOR_MENU = MENUS.register("liquid_generator", () -> IMenuTypeExtension.create((id, inv, buf) -> new LiquidGeneratorMenu(id, inv, buf.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, @NotNull MenuType<@NotNull BlockGeneratorMenu>> BLOCK_GENERATOR_MENU = MENUS.register("block_generator", () -> IMenuTypeExtension.create((id, inv, buf) -> new BlockGeneratorMenu(id, inv, buf.readBlockPos())));

    @NotNull
    private static <T extends Block & EntityBlock> DeferredHolder<Block, @NotNull T> registerBlock(String name, DataConfig config, BiFunction<BlockBehaviour.Properties, DataConfig, T> creator) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(DyeColor.BLUE).pushReaction(PushReaction.DESTROY).strength(0.5f, 3.0f).setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(AutoResource.MODID, name)));
        return BLOCKS.register(name, () -> creator.apply(properties, config));
    }

    private static <T extends BlockItem, B extends Block> DeferredHolder<Item, T> registerItem(String name, DataConfig config, DeferredHolder<Block, B> blockRegistry, ItemCreator<T> creator) {
        // 物品模型在 assets/autoresource/items/<name>.json 中定义（26.x 物品模型定义，指向方块模型）
        // useBlockDescriptionPrefix：26.x 中 BlockItem 默认使用 item.<mod>.<id> 语言键，需显式改用 block.<mod>.<id>
        Item.Properties properties = new Item.Properties().stacksTo(1).fireResistant()
                .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(AutoResource.MODID, name)))
                .useBlockDescriptionPrefix();
        return ITEMS.register(name, () -> creator.create(blockRegistry.get(), properties, config));
    }

    private static <T extends BlockEntity, B extends Block> DeferredHolder<BlockEntityType<?>, @NotNull BlockEntityType<T>> registerEntity(String name, DataConfig config, DeferredHolder<Block, B> blockRegistry, EntityCreator<T> creator) {
        return BLOCK_ENTITIES.register(name, () -> new BlockEntityType<>((pos, state) -> creator.create(pos, state, config), blockRegistry.get()));
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
