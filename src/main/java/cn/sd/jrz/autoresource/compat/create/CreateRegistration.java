package cn.sd.jrz.autoresource.compat.create;

import cn.sd.jrz.autoresource.AutoResource;
import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Create 联动的注册入口。本类直接引用 Create 类，只能由 CreateCompat.invokeRegistration
 * 经 Class.forName 反射在 Create 加载时调用，保证无 Create 时类加载安全。
 */
public final class CreateRegistration {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AutoResource.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AutoResource.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AutoResource.MODID);
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, AutoResource.MODID);

    private static final BlockBehaviour.Properties MOTOR_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(DyeColor.BLUE)
            .pushReaction(PushReaction.DESTROY)
            .strength(0.5f, 3.0f)
            // 侧面 LCD 显示窗自发光
            .lightLevel(state -> 7);

    private CreateRegistration() {
    }

    /**
     * 注册水车马达（双端），仅由 Registration.init 反射调用
     */
    public static void register(FMLJavaModLoadingContext context) {
        BLOCKS.register(context.getModEventBus());
        ITEMS.register(context.getModEventBus());
        BLOCK_ENTITIES.register(context.getModEventBus());
        CONTAINERS.register(context.getModEventBus());

        Registration.WATER_WHEEL_MOTOR = BLOCKS.register("water_wheel_motor", () -> new WaterWheelMotorBlock(MOTOR_PROPERTIES));
        Registration.WATER_WHEEL_MOTOR_ITEM = ITEMS.register("water_wheel_motor",
                () -> new WaterWheelMotorItem(Registration.WATER_WHEEL_MOTOR.get()));
        // vanilla 的 BlockEntitySupplier 为 (pos, state) 两参；实体构造器需要 BlockEntityType，运行时经静态字段解析
        Registration.WATER_WHEEL_MOTOR_ENTITY = BLOCK_ENTITIES.register("water_wheel_motor",
                () -> BlockEntityType.Builder.of((pos, state) -> new WaterWheelMotorEntity(Registration.WATER_WHEEL_MOTOR_ENTITY.get(), pos, state), Registration.WATER_WHEEL_MOTOR.get()).build(null));
        Registration.WATER_WHEEL_MOTOR_MENU = CONTAINERS.register("water_wheel_motor",
                () -> IForgeMenuType.create((id, inv, buf) -> new WaterWheelMotorMenu(id, inv, buf.readBlockPos())));
    }

    /**
     * 客户端注册水车马达 GUI，仅由 ClientSetup 反射调用
     */
    @SuppressWarnings("unchecked")
    public static void registerClient() {
        MenuScreens.register((MenuType<? extends WaterWheelMotorMenu>) Registration.WATER_WHEEL_MOTOR_MENU.get(), WaterWheelMotorScreen::new);
    }

    /**
     * 客户端注册水车马达方块实体渲染器（四面侧显示转速文字），仅由 ClientSetup 反射调用
     */
    @SuppressWarnings("unchecked")
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // WATER_WHEEL_MOTOR_ENTITY 声明为 BlockEntityType<?>，需显式强转以匹配泛型（与 registerClient 一致）
        event.registerBlockEntityRenderer((BlockEntityType<WaterWheelMotorEntity>) (BlockEntityType<?>) Registration.WATER_WHEEL_MOTOR_ENTITY.get(), WaterWheelMotorRenderer::new);
    }
}
