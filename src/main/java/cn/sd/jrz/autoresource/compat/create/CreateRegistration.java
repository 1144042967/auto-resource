package cn.sd.jrz.autoresource.compat.create;

import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/**
 * Create 联动的注册入口。本类直接引用 Create 类，只能由 CreateCompat.invokeRegistration
 * 经 Class.forName 反射在 Create 加载时调用，保证无 Create 时类加载安全。
 */
public final class CreateRegistration {

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
    public static void register() {
        Registration.WATER_WHEEL_MOTOR = Registry.register(BuiltInRegistries.BLOCK, Registration.id("water_wheel_motor"),
                new WaterWheelMotorBlock(MOTOR_PROPERTIES));
        Registration.WATER_WHEEL_MOTOR_ITEM = Registry.register(BuiltInRegistries.ITEM, Registration.id("water_wheel_motor"),
                new WaterWheelMotorItem(Registration.WATER_WHEEL_MOTOR));
        // vanilla 的 BlockEntitySupplier 为 (pos, state) 两参；实体构造器需要 BlockEntityType，运行时经静态字段解析
        Registration.WATER_WHEEL_MOTOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Registration.id("water_wheel_motor"),
                BlockEntityType.Builder.of(
                        (pos, state) -> new WaterWheelMotorEntity(Registration.WATER_WHEEL_MOTOR_ENTITY, pos, state),
                        Registration.WATER_WHEEL_MOTOR).build(null));
        Registration.WATER_WHEEL_MOTOR_MENU = Registry.register(BuiltInRegistries.MENU, Registration.id("water_wheel_motor"),
                new MenuType<>((id, inv) -> new WaterWheelMotorMenu(id, inv, inv.player.blockPosition()), null));
    }
}
