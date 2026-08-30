package cn.sd.jrz.autoresource.setup;

import cn.sd.jrz.autoresource.AutoResource;
import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.blockentity.BlockGeneratorEntity;
import cn.sd.jrz.autoresource.blockentity.EnergyGeneratorEntity;
import cn.sd.jrz.autoresource.blockentity.LiquidGeneratorEntity;
import cn.sd.jrz.autoresource.blocks.BlockGeneratorBlock;
import cn.sd.jrz.autoresource.blocks.EnergyGeneratorBlock;
import cn.sd.jrz.autoresource.blocks.LiquidGeneratorBlock;
import cn.sd.jrz.autoresource.compat.energy.EnergyCompat;
import cn.sd.jrz.autoresource.items.BlockGeneratorItem;
import cn.sd.jrz.autoresource.items.EnergyGeneratorItem;
import cn.sd.jrz.autoresource.items.LiquidGeneratorItem;
import cn.sd.jrz.autoresource.menu.BlockGeneratorMenu;
import cn.sd.jrz.autoresource.menu.EnergyGeneratorMenu;
import cn.sd.jrz.autoresource.menu.LiquidGeneratorMenu;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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

        // 方块实体持有的能量/流体/物品存储向周边暴露（六面均可访问）
        TransferSetup.init();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(AutoResource.MODID, path);
    }

    // 1.21.11：Block 构造要求 Properties.setId(ResourceKey)，否则抛 "Block id not set"；
    // 因此每个方块用各自路径构建独立的 Properties（共享 BLOCK_PROPERTIES 无法区分 id）
    private static BlockBehaviour.Properties blockProperties(String path) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, id(path)))
                .mapColor(DyeColor.BLUE)
                .pushReaction(PushReaction.DESTROY)
                .strength(0.5f, 3.0f)
                .lightLevel(state -> 7);
    }

    // ==================== Blocks ====================

    public static final Block LIQUID_GENERATOR_WATER = Registry.register(BuiltInRegistries.BLOCK, id("liquid_generator_water"),
            new LiquidGeneratorBlock(blockProperties("liquid_generator_water"), DataConfig.LIQUID_GENERATOR_WATER));
    public static final Block LIQUID_GENERATOR_LAVA = Registry.register(BuiltInRegistries.BLOCK, id("liquid_generator_lava"),
            new LiquidGeneratorBlock(blockProperties("liquid_generator_lava"), DataConfig.LIQUID_GENERATOR_LAVA));
    public static final Block BLOCK_GENERATOR = Registry.register(BuiltInRegistries.BLOCK, id("block_generator"),
            new BlockGeneratorBlock(blockProperties("block_generator"), DataConfig.BLOCK_GENERATOR));

    // ==================== Items ====================

    public static final Item LIQUID_GENERATOR_WATER_ITEM = Registry.register(BuiltInRegistries.ITEM, id("liquid_generator_water"),
            new LiquidGeneratorItem(LIQUID_GENERATOR_WATER, DataConfig.LIQUID_GENERATOR_WATER, ResourceKey.create(Registries.ITEM, id("liquid_generator_water"))));
    public static final Item LIQUID_GENERATOR_LAVA_ITEM = Registry.register(BuiltInRegistries.ITEM, id("liquid_generator_lava"),
            new LiquidGeneratorItem(LIQUID_GENERATOR_LAVA, DataConfig.LIQUID_GENERATOR_LAVA, ResourceKey.create(Registries.ITEM, id("liquid_generator_lava"))));
    public static final Item BLOCK_GENERATOR_ITEM = Registry.register(BuiltInRegistries.ITEM, id("block_generator"),
            new BlockGeneratorItem(BLOCK_GENERATOR, DataConfig.BLOCK_GENERATOR, ResourceKey.create(Registries.ITEM, id("block_generator"))));

    // ==================== Block Entities ====================

    public static final BlockEntityType<LiquidGeneratorEntity> LIQUID_GENERATOR_WATER_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("liquid_generator_water"),
            FabricBlockEntityTypeBuilder.create((pos, state) -> new LiquidGeneratorEntity(pos, state, DataConfig.LIQUID_GENERATOR_WATER), LIQUID_GENERATOR_WATER).build());
    public static final BlockEntityType<LiquidGeneratorEntity> LIQUID_GENERATOR_LAVA_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("liquid_generator_lava"),
            FabricBlockEntityTypeBuilder.create((pos, state) -> new LiquidGeneratorEntity(pos, state, DataConfig.LIQUID_GENERATOR_LAVA), LIQUID_GENERATOR_LAVA).build());
    public static final BlockEntityType<BlockGeneratorEntity> BLOCK_GENERATOR_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("block_generator"),
            FabricBlockEntityTypeBuilder.create((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR), BLOCK_GENERATOR).build());

    // ==================== Menus（扩展屏幕处理器：打开时携带机器坐标，客户端工厂据此定位实体） ====================

    public static final MenuType<LiquidGeneratorMenu> LIQUID_GENERATOR_MENU = Registry.register(BuiltInRegistries.MENU, id("liquid_generator"),
            new ExtendedScreenHandlerType<>((id, inv, pos) -> new LiquidGeneratorMenu(id, inv, pos), BlockPos.STREAM_CODEC));
    public static final MenuType<BlockGeneratorMenu> BLOCK_GENERATOR_MENU = Registry.register(BuiltInRegistries.MENU, id("block_generator"),
            new ExtendedScreenHandlerType<>((id, inv, pos) -> new BlockGeneratorMenu(id, inv, pos), BlockPos.STREAM_CODEC));

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
                new EnergyGeneratorBlock(blockProperties("energy_generator_fe"), DataConfig.ENERGY_GENERATOR_FE));
        ENERGY_GENERATOR_FE_ITEM = Registry.register(BuiltInRegistries.ITEM, id("energy_generator_fe"),
                new EnergyGeneratorItem(ENERGY_GENERATOR_FE, DataConfig.ENERGY_GENERATOR_FE, ResourceKey.create(Registries.ITEM, id("energy_generator_fe"))));
        ENERGY_GENERATOR_FE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("energy_generator_fe"),
                FabricBlockEntityTypeBuilder.create((pos, state) -> new EnergyGeneratorEntity(pos, state, DataConfig.ENERGY_GENERATOR_FE), ENERGY_GENERATOR_FE).build());
        ENERGY_GENERATOR_MENU = Registry.register(BuiltInRegistries.MENU, id("energy_generator"),
                new ExtendedScreenHandlerType<>((id, inv, pos) -> new EnergyGeneratorMenu(id, inv, pos), BlockPos.STREAM_CODEC));
    }
}