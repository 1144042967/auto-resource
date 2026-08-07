package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.AutoResource;
import cn.sd.jrz.autoresource.setup.ARRegistration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** 客户端初始化：注册 GUI 与方块实体渲染器 */
@SuppressWarnings("removal")
@EventBusSubscriber(value = Dist.CLIENT, modid = AutoResource.MODID)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ARRegistration.ENERGY_GENERATOR_MENU.get(), EnergyGeneratorScreen::new);
        event.register(ARRegistration.LIQUID_GENERATOR_MENU.get(), LiquidGeneratorScreen::new);
        event.register(ARRegistration.BLOCK_GENERATOR_MENU.get(), BlockGeneratorScreen::new);
    }

    /** 注册方块生成机的方块实体渲染器（在四个侧面显示标记物品） */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ARRegistration.BLOCK_GENERATOR_ENTITY.get(), BlockGeneratorRenderer::new);
    }
}
