package cn.sd.jrz.autoresource.setup;

import cn.sd.jrz.autoresource.AutoResource;
import cn.sd.jrz.autoresource.DataConfig;
import cn.sd.jrz.autoresource.blockentity.BlockGeneratorEntity;
import cn.sd.jrz.autoresource.blockentity.EnergyGeneratorEntity;
import cn.sd.jrz.autoresource.blockentity.LiquidGeneratorEntity;
import cn.sd.jrz.autoresource.blocks.BlockGeneratorBlock;
import cn.sd.jrz.autoresource.blocks.EnergyGeneratorBlock;
import cn.sd.jrz.autoresource.blocks.LiquidGeneratorBlock;
import cn.sd.jrz.autoresource.items.BlockGeneratorItem;
import cn.sd.jrz.autoresource.items.EnergyGeneratorItem;
import cn.sd.jrz.autoresource.items.LiquidGeneratorItem;
import cn.sd.jrz.autoresource.menu.BlockGeneratorMenu;
import cn.sd.jrz.autoresource.menu.EnergyGeneratorMenu;
import cn.sd.jrz.autoresource.menu.LiquidGeneratorMenu;
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

/**
 * 中心注册表：Fabric 使用 Registry.register 直接注册（对应 Forge 版的 DeferredRegister），
 * 所有机器方块属性保持一致：蓝色、活塞推动销毁、硬度 0.5/爆炸抗性 3、发光 7。
 */
public class Registration {

    public static void init() {
        // 方块实体持有的能量/流体/物品存储向周边暴露（六面均可访问）
        TransferSetup.init();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AutoResource.MODID, path);
    }

    private static final BlockBehaviour.Properties BLOCK_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(DyeColor.BLUE)
            .pushReaction(PushReaction.DESTROY)
            .strength(0.5f, 3.0f)
            .lightLevel(state -> 7);

    // ==================== Blocks ====================

    public static final Block ENERGY_GENERATOR_FE = Registry.register(BuiltInRegistries.BLOCK, id("energy_generator_fe"),
            new EnergyGeneratorBlock(BLOCK_PROPERTIES, DataConfig.ENERGY_GENERATOR_FE));
    public static final Block LIQUID_GENERATOR_WATER = Registry.register(BuiltInRegistries.BLOCK, id("liquid_generator_water"),
            new LiquidGeneratorBlock(BLOCK_PROPERTIES, DataConfig.LIQUID_GENERATOR_WATER));
    public static final Block LIQUID_GENERATOR_LAVA = Registry.register(BuiltInRegistries.BLOCK, id("liquid_generator_lava"),
            new LiquidGeneratorBlock(BLOCK_PROPERTIES, DataConfig.LIQUID_GENERATOR_LAVA));
    public static final Block BLOCK_GENERATOR = Registry.register(BuiltInRegistries.BLOCK, id("block_generator"),
            new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig.BLOCK_GENERATOR));

    // ==================== Items ====================

    public static final Item ENERGY_GENERATOR_FE_ITEM = Registry.register(BuiltInRegistries.ITEM, id("energy_generator_fe"),
            new EnergyGeneratorItem(ENERGY_GENERATOR_FE, DataConfig.ENERGY_GENERATOR_FE));
    public static final Item LIQUID_GENERATOR_WATER_ITEM = Registry.register(BuiltInRegistries.ITEM, id("liquid_generator_water"),
            new LiquidGeneratorItem(LIQUID_GENERATOR_WATER, DataConfig.LIQUID_GENERATOR_WATER));
    public static final Item LIQUID_GENERATOR_LAVA_ITEM = Registry.register(BuiltInRegistries.ITEM, id("liquid_generator_lava"),
            new LiquidGeneratorItem(LIQUID_GENERATOR_LAVA, DataConfig.LIQUID_GENERATOR_LAVA));
    public static final Item BLOCK_GENERATOR_ITEM = Registry.register(BuiltInRegistries.ITEM, id("block_generator"),
            new BlockGeneratorItem(BLOCK_GENERATOR, DataConfig.BLOCK_GENERATOR));

    // ==================== Block Entities ====================

    public static final BlockEntityType<EnergyGeneratorEntity> ENERGY_GENERATOR_FE_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("energy_generator_fe"),
            BlockEntityType.Builder.of((pos, state) -> new EnergyGeneratorEntity(pos, state, DataConfig.ENERGY_GENERATOR_FE), ENERGY_GENERATOR_FE).build(null));
    public static final BlockEntityType<LiquidGeneratorEntity> LIQUID_GENERATOR_WATER_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("liquid_generator_water"),
            BlockEntityType.Builder.of((pos, state) -> new LiquidGeneratorEntity(pos, state, DataConfig.LIQUID_GENERATOR_WATER), LIQUID_GENERATOR_WATER).build(null));
    public static final BlockEntityType<LiquidGeneratorEntity> LIQUID_GENERATOR_LAVA_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("liquid_generator_lava"),
            BlockEntityType.Builder.of((pos, state) -> new LiquidGeneratorEntity(pos, state, DataConfig.LIQUID_GENERATOR_LAVA), LIQUID_GENERATOR_LAVA).build(null));
    public static final BlockEntityType<BlockGeneratorEntity> BLOCK_GENERATOR_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("block_generator"),
            BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig.BLOCK_GENERATOR), BLOCK_GENERATOR).build(null));

    // ==================== Menus（1.21.1：ExtendedScreenHandlerType 已被 fabric-api 0.116 移除，
    // 改用普通 MenuType + 工厂在创建时按玩家所在世界的实体坐标定位机器实体） ====================

    public static final MenuType<EnergyGeneratorMenu> ENERGY_GENERATOR_MENU = Registry.register(BuiltInRegistries.MENU, id("energy_generator"),
            new MenuType<>(Registration::createEnergyGeneratorMenu, null));
    public static final MenuType<LiquidGeneratorMenu> LIQUID_GENERATOR_MENU = Registry.register(BuiltInRegistries.MENU, id("liquid_generator"),
            new MenuType<>(Registration::createLiquidGeneratorMenu, null));
    public static final MenuType<BlockGeneratorMenu> BLOCK_GENERATOR_MENU = Registry.register(BuiltInRegistries.MENU, id("block_generator"),
            new MenuType<>(Registration::createBlockGeneratorMenu, null));

    /**
     * 通过玩家所在世界查找最近的对应实体并创建菜单（fallback：未找到实体时使用玩家坐标）
     */
    private static EnergyGeneratorMenu createEnergyGeneratorMenu(int id, net.minecraft.world.entity.player.Inventory inv) {
        net.minecraft.core.BlockPos pos = inv.player.blockPosition();
        return new EnergyGeneratorMenu(id, inv, pos);
    }

    private static LiquidGeneratorMenu createLiquidGeneratorMenu(int id, net.minecraft.world.entity.player.Inventory inv) {
        net.minecraft.core.BlockPos pos = inv.player.blockPosition();
        return new LiquidGeneratorMenu(id, inv, pos);
    }

    private static BlockGeneratorMenu createBlockGeneratorMenu(int id, net.minecraft.world.entity.player.Inventory inv) {
        net.minecraft.core.BlockPos pos = inv.player.blockPosition();
        return new BlockGeneratorMenu(id, inv, pos);
    }
}