package cn.sd.jrz.autoresource.client.compat.create;

import cn.sd.jrz.autoresource.compat.create.WaterWheelMotorEntity;
import cn.sd.jrz.autoresource.setup.Registration;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Create 联动客户端注册：屏幕工厂与方块实体渲染器。
 * 本类位于 client source set 且仅经反射按类名字符串触发（见 CreateCompat#invokeClient），
 * 避免通用侧字节码引用 Create 依赖类型。
 */
public final class CreateRegistrationClient {

    private CreateRegistrationClient() {
    }

    /**
     * 注册水车马达屏幕（仅当 WATER_WHEEL_MOTOR_MENU 就绪时由客户端入口反射调用）
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerScreens() {
        if (Registration.WATER_WHEEL_MOTOR_MENU != null) {
            // 运行时元素类型即 WaterWheelMotorMenu，注册处只能以原始类型桥接泛型
            MenuType rawMenuType = Registration.WATER_WHEEL_MOTOR_MENU;
            MenuScreens.register(rawMenuType, WaterWheelMotorScreen::new);
        }
    }

    /**
     * 注册水车马达渲染器（四面转速文字）
     */
    @SuppressWarnings("unchecked")
    public static void registerRenderers() {
        if (Registration.WATER_WHEEL_MOTOR_ENTITY != null) {
            BlockEntityType<WaterWheelMotorEntity> entityType =
                    (BlockEntityType<WaterWheelMotorEntity>) Registration.WATER_WHEEL_MOTOR_ENTITY;
            BlockEntityRendererRegistry.register(entityType, WaterWheelMotorRenderer::new);
        }
    }
}
