package cn.sd.jrz.autoresource.setup;

import cn.sd.jrz.autoresource.AutoResource;
import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.blockentity.BlockGeneratorEntity;
import cn.sd.jrz.autoresource.blockentity.EnergyGeneratorEntity;
import cn.sd.jrz.autoresource.blockentity.LiquidGeneratorEntity;
import cn.sd.jrz.autoresource.blocks.BlockGeneratorBlock;
import cn.sd.jrz.autoresource.blocks.EnergyGeneratorBlock;
import cn.sd.jrz.autoresource.blocks.LiquidGeneratorBlock;
import cn.sd.jrz.autoresource.compat.create.CreateCompat;
import cn.sd.jrz.autoresource.items.BlockGeneratorItem;
import cn.sd.jrz.autoresource.items.EnergyGeneratorItem;
import cn.sd.jrz.autoresource.items.LiquidGeneratorItem;
import cn.sd.jrz.autoresource.menu.BlockGeneratorMenu;
import cn.sd.jrz.autoresource.menu.EnergyGeneratorMenu;
import cn.sd.jrz.autoresource.menu.LiquidGeneratorMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;

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

        // 机械动力联动：仅当 Create 加载时经反射注册（不能在字节码里引用 Create 类，否则无 Create 时 NoClassDefFoundError）。
        if (CreateCompat.isCreateLoaded()) {
            CreateCompat.invokeRegistration("register",
                    new Class<?>[]{FMLJavaModLoadingContext.class}, new Object[]{context});
        }
    }

    private static final BlockBehaviour.Properties BLOCK_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(DyeColor.BLUE)
            .pushReaction(PushReaction.DESTROY)
            .strength(0.5f, 3.0f)
            .lightLevel(state -> 7);

    // Blocks

    public static final RegistryObject<Block> ENERGY_GENERATOR_FE = BLOCKS.register("energy_generator_fe", () -> new EnergyGeneratorBlock(BLOCK_PROPERTIES, DataConfig.ENERGY_GENERATOR_FE));
    public static final RegistryObject<Block> LIQUID_GENERATOR_WATER = BLOCKS.register("liquid_generator_water", () -> new LiquidGeneratorBlock(BLOCK_PROPERTIES, DataConfig.LIQUID_GENERATOR_WATER));
    public static final RegistryObject<Block> LIQUID_GENERATOR_LAVA = BLOCKS.register("liquid_generator_lava", () -> new LiquidGeneratorBlock(BLOCK_PROPERTIES, DataConfig.LIQUID_GENERATOR_LAVA));
    public static final RegistryObject<Block> BLOCK_GENERATOR = BLOCKS.register("block_generator", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR));

    // Items

    public static final RegistryObject<Item> ENERGY_GENERATOR_FE_ITEM = ITEMS.register("energy_generator_fe", () -> new EnergyGeneratorItem(ENERGY_GENERATOR_FE.get(), DataConfig.ENERGY_GENERATOR_FE));
    public static final RegistryObject<Item> LIQUID_GENERATOR_WATER_ITEM = ITEMS.register("liquid_generator_water", () -> new LiquidGeneratorItem(LIQUID_GENERATOR_WATER.get(), DataConfig.LIQUID_GENERATOR_WATER));
    public static final RegistryObject<Item> LIQUID_GENERATOR_LAVA_ITEM = ITEMS.register("liquid_generator_lava", () -> new LiquidGeneratorItem(LIQUID_GENERATOR_LAVA.get(), DataConfig.LIQUID_GENERATOR_LAVA));
    public static final RegistryObject<Item> BLOCK_GENERATOR_ITEM = ITEMS.register("block_generator", () -> new BlockGeneratorItem(BLOCK_GENERATOR.get(), DataConfig.BLOCK_GENERATOR));

    // Tile Entities

    public static final RegistryObject<BlockEntityType<EnergyGeneratorEntity>> ENERGY_GENERATOR_FE_ENTITY = BLOCK_ENTITIES.register("energy_generator_fe", () -> BlockEntityType.Builder.of((pos, state) -> new EnergyGeneratorEntity(pos, state, DataConfig.ENERGY_GENERATOR_FE), ENERGY_GENERATOR_FE.get()).build(null));
    public static final RegistryObject<BlockEntityType<LiquidGeneratorEntity>> LIQUID_GENERATOR_WATER_ENTITY = BLOCK_ENTITIES.register("liquid_generator_water", () -> BlockEntityType.Builder.of((pos, state) -> new LiquidGeneratorEntity(pos, state, DataConfig.LIQUID_GENERATOR_WATER), LIQUID_GENERATOR_WATER.get()).build(null));
    public static final RegistryObject<BlockEntityType<LiquidGeneratorEntity>> LIQUID_GENERATOR_LAVA_ENTITY = BLOCK_ENTITIES.register("liquid_generator_lava", () -> BlockEntityType.Builder.of((pos, state) -> new LiquidGeneratorEntity(pos, state, DataConfig.LIQUID_GENERATOR_LAVA), LIQUID_GENERATOR_LAVA.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> BLOCK_GENERATOR_ENTITY = BLOCK_ENTITIES.register("block_generator", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR), BLOCK_GENERATOR.get()).build(null));

    // Menus

    public static final RegistryObject<MenuType<EnergyGeneratorMenu>> ENERGY_GENERATOR_MENU = CONTAINERS.register("energy_generator", () -> IForgeMenuType.create((id, inv, buf) -> new EnergyGeneratorMenu(id, inv, buf.readBlockPos())));
    public static final RegistryObject<MenuType<LiquidGeneratorMenu>> LIQUID_GENERATOR_MENU = CONTAINERS.register("liquid_generator", () -> IForgeMenuType.create((id, inv, buf) -> new LiquidGeneratorMenu(id, inv, buf.readBlockPos())));
    public static final RegistryObject<MenuType<BlockGeneratorMenu>> BLOCK_GENERATOR_MENU = CONTAINERS.register("block_generator", () -> IForgeMenuType.create((id, inv, buf) -> new BlockGeneratorMenu(id, inv, buf.readBlockPos())));

    // Create（机械动力）联动 —— 仅当 Create 加载时在 init() 中注册，否则均为 null

    @Nullable
    public static RegistryObject<Block> WATER_WHEEL_MOTOR;
    @Nullable
    public static RegistryObject<Item> WATER_WHEEL_MOTOR_ITEM;
    @Nullable
    public static RegistryObject<BlockEntityType<?>> WATER_WHEEL_MOTOR_ENTITY;
    @Nullable
    public static RegistryObject<MenuType<?>> WATER_WHEEL_MOTOR_MENU;
}
