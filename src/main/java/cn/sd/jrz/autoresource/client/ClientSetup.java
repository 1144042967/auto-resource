package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.AutoResource;
import cn.sd.jrz.autoresource.compat.create.CreateCompat;
import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** 客户端初始化：注册 GUI 与方块实体渲染器 */
@Mod.EventBusSubscriber(modid = AutoResource.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(Registration.ENERGY_GENERATOR_MENU.get(), EnergyGeneratorScreen::new);
            MenuScreens.register(Registration.LIQUID_GENERATOR_MENU.get(), LiquidGeneratorScreen::new);
            MenuScreens.register(Registration.BLOCK_GENERATOR_MENU.get(), BlockGeneratorScreen::new);
            // Create 联动：仅当 Create 加载时经反射注册水车马达 GUI（避免 ClientSetup 字节码引用 Create 类）
            if (CreateCompat.isCreateLoaded() && Registration.WATER_WHEEL_MOTOR_MENU != null) {
                CreateCompat.invokeRegistration("registerClient", new Class<?>[0], new Object[0]);
            }
        });
    }

    /** 注册方块生成机的方块实体渲染器（在四个侧面显示标记物品） */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.BLOCK_GENERATOR_ENTITY.get(), BlockGeneratorRenderer::new);
        // Create 联动：水车马达渲染器（四面侧显示转速文字）仅当 Create 加载时经反射注册，
        // 避免 ClientSetup 字节码引用 Create 依赖类（WaterWheelMotorRenderer 实现了 Create 方块实体泛型）
        if (CreateCompat.isCreateLoaded() && Registration.WATER_WHEEL_MOTOR_ENTITY != null) {
            CreateCompat.invokeRegistration("registerRenderers", new Class<?>[]{EntityRenderersEvent.RegisterRenderers.class}, new Object[]{event});
        }
    }
}
