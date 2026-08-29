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
import cn.sd.jrz.autoresource.compat.energy.EnergyCompat;
import cn.sd.jrz.autoresource.items.BlockGeneratorItem;
import cn.sd.jrz.autoresource.items.EnergyGeneratorItem;
import cn.sd.jrz.autoresource.items.LiquidGeneratorItem;
import cn.sd.jrz.autoresource.menu.BlockGeneratorMenu;
import cn.sd.jrz.autoresource.menu.EnergyGeneratorMenu;
import cn.sd.jrz.autoresource.menu.LiquidGeneratorMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;

/**
 * 中心注册表：Fabric 使用 Registry.register 直接注册（对应 Forge 版的 DeferredRegister），
 * 所有机器方块属性保持一致：蓝色、活塞推动销毁、硬度 0.5/爆炸抗性 3、发光 7。
 */
public class Registration {

    public static void init() {
        // FE 发电机：前置 teamreborn energy 未安装时不注册（直接引用 EnergyGenerator* 会触发
        // 缺失的 EnergyStorage 类加载 → NoClassDefFoundError）
        if (EnergyCompat.isEnergyLoaded()) {
            registerEnergyGenerator();
        }

        // 机械动力联动：仅当 Create 加载时经反射注册（不能在字节码里引用 Create 类，否则无 Create 时 NoClassDefFoundError）。
        if (CreateCompat.isCreateLoaded()) {
            CreateCompat.invokeRegistration("register", new Class<?>[0], new Object[0]);
        }

        // 方块实体持有的能量/流体/物品存储向周边暴露（六面均可访问）
        TransferSetup.init();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(AutoResource.MODID, path);
    }

    private static final BlockBehaviour.Properties BLOCK_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(DyeColor.BLUE)
            .pushReaction(PushReaction.DESTROY)
            .strength(0.5f, 3.0f)
            .lightLevel(state -> 7);

    // ==================== Blocks ====================

    public static final Block LIQUID_GENERATOR_WATER = Registry.register(BuiltInRegistries.BLOCK, id("liquid_generator_water"),
            new LiquidGeneratorBlock(BLOCK_PROPERTIES, DataConfig.LIQUID_GENERATOR_WATER));
    public static final Block LIQUID_GENERATOR_LAVA = Registry.register(BuiltInRegistries.BLOCK, id("liquid_generator_lava"),
            new LiquidGeneratorBlock(BLOCK_PROPERTIES, DataConfig.LIQUID_GENERATOR_LAVA));
    public static final Block BLOCK_GENERATOR = Registry.register(BuiltInRegistries.BLOCK, id("block_generator"),
            new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR));

    // ==================== Items ====================

    public static final Item LIQUID_GENERATOR_WATER_ITEM = Registry.register(BuiltInRegistries.ITEM, id("liquid_generator_water"),
            new LiquidGeneratorItem(LIQUID_GENERATOR_WATER, DataConfig.LIQUID_GENERATOR_WATER));
    public static final Item LIQUID_GENERATOR_LAVA_ITEM = Registry.register(BuiltInRegistries.ITEM, id("liquid_generator_lava"),
            new LiquidGeneratorItem(LIQUID_GENERATOR_LAVA, DataConfig.LIQUID_GENERATOR_LAVA));
    public static final Item BLOCK_GENERATOR_ITEM = Registry.register(BuiltInRegistries.ITEM, id("block_generator"),
            new BlockGeneratorItem(BLOCK_GENERATOR, DataConfig.BLOCK_GENERATOR));

    // ==================== Block Entities ====================

    public static final BlockEntityType<LiquidGeneratorEntity> LIQUID_GENERATOR_WATER_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("liquid_generator_water"),
            BlockEntityType.Builder.of((pos, state) -> new LiquidGeneratorEntity(pos, state, DataConfig.LIQUID_GENERATOR_WATER), LIQUID_GENERATOR_WATER).build(null));
    public static final BlockEntityType<LiquidGeneratorEntity> LIQUID_GENERATOR_LAVA_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("liquid_generator_lava"),
            BlockEntityType.Builder.of((pos, state) -> new LiquidGeneratorEntity(pos, state, DataConfig.LIQUID_GENERATOR_LAVA), LIQUID_GENERATOR_LAVA).build(null));
    public static final BlockEntityType<BlockGeneratorEntity> BLOCK_GENERATOR_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("block_generator"),
            BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR), BLOCK_GENERATOR).build(null));

    // ==================== Menus（扩展屏幕处理器：打开时携带机器坐标） ====================

    public static final MenuType<LiquidGeneratorMenu> LIQUID_GENERATOR_MENU = Registry.register(BuiltInRegistries.MENU, id("liquid_generator"),
            new ExtendedScreenHandlerType<>((id, inv, buf) -> new LiquidGeneratorMenu(id, inv, buf.readBlockPos())));
    public static final MenuType<BlockGeneratorMenu> BLOCK_GENERATOR_MENU = Registry.register(BuiltInRegistries.MENU, id("block_generator"),
            new ExtendedScreenHandlerType<>((id, inv, buf) -> new BlockGeneratorMenu(id, inv, buf.readBlockPos())));

    // ==================== Create（机械动力）联动 —— 仅当 Create 加载时由 CreateRegistration 反射填充，否则均为 null ====================

    @Nullable
    public static Block WATER_WHEEL_MOTOR;
    @Nullable
    public static Item WATER_WHEEL_MOTOR_ITEM;
    @Nullable
    public static BlockEntityType<?> WATER_WHEEL_MOTOR_ENTITY;
    @Nullable
    public static MenuType<?> WATER_WHEEL_MOTOR_MENU;

    // ==================== FE 发电机 —— 前置 teamreborn energy 加载时注册，否则保持 null ====================

    @Nullable
    public static Block ENERGY_GENERATOR_FE;
    @Nullable
    public static Item ENERGY_GENERATOR_FE_ITEM;
    @Nullable
    public static BlockEntityType<EnergyGeneratorEntity> ENERGY_GENERATOR_FE_ENTITY;
    @Nullable
    public static MenuType<EnergyGeneratorMenu> ENERGY_GENERATOR_MENU;

    /**
     * 注册 FE 发电机四件套（仅当 teamreborn energy 加载时由 {@link #init()} 调用）。
     * 本方法直接引用 EnergyGenerator* 类，必须在前置存在时执行，否则 NoClassDefFoundError。
     */
    private static void registerEnergyGenerator() {
        ENERGY_GENERATOR_FE = Registry.register(BuiltInRegistries.BLOCK, id("energy_generator_fe"),
                new EnergyGeneratorBlock(BLOCK_PROPERTIES, DataConfig.ENERGY_GENERATOR_FE));
        ENERGY_GENERATOR_FE_ITEM = Registry.register(BuiltInRegistries.ITEM, id("energy_generator_fe"),
                new EnergyGeneratorItem(ENERGY_GENERATOR_FE, DataConfig.ENERGY_GENERATOR_FE));
        ENERGY_GENERATOR_FE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("energy_generator_fe"),
                BlockEntityType.Builder.of((pos, state) -> new EnergyGeneratorEntity(pos, state, DataConfig.ENERGY_GENERATOR_FE), ENERGY_GENERATOR_FE).build(null));
        ENERGY_GENERATOR_MENU = Registry.register(BuiltInRegistries.MENU, id("energy_generator"),
                new ExtendedScreenHandlerType<>((id, inv, buf) -> new EnergyGeneratorMenu(id, inv, buf.readBlockPos())));
    }
}
