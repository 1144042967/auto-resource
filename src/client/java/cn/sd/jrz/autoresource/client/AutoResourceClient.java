package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.Config;
import cn.sd.jrz.autoresource.compat.create.CreateCompat;
import cn.sd.jrz.autoresource.network.ConfigSync;
import cn.sd.jrz.autoresource.setup.Registration;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

/**
 * 客户端初始化：注册 GUI、方块实体渲染器与配置同步接收器。
 * Create 兼容层的屏幕/渲染器仍以字符串反射触发，避免字节码引用 Create 相关类。
 */
public class AutoResourceClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 屏幕注册（扩展菜单类型的附加数据已在网络层读取）
        MenuScreens.register(Registration.ENERGY_GENERATOR_MENU, EnergyGeneratorScreen::new);
        MenuScreens.register(Registration.LIQUID_GENERATOR_MENU, LiquidGeneratorScreen::new);
        MenuScreens.register(Registration.BLOCK_GENERATOR_MENU, BlockGeneratorScreen::new);

        // 方块生成机的标记物品四侧渲染
        BlockEntityRendererRegistry.register(Registration.BLOCK_GENERATOR_ENTITY, BlockGeneratorRenderer::new);

        // Create 联动：仅当 Create 加载时经反射注册屏幕与渲染器（避免字节码引用 Create 依赖类）
        if (CreateCompat.isCreateLoaded()) {
            if (Registration.WATER_WHEEL_MOTOR_MENU != null) {
                CreateCompat.invokeClient("registerScreens", new Class<?>[0], new Object[0]);
            }
            if (Registration.WATER_WHEEL_MOTOR_ENTITY != null) {
                CreateCompat.invokeClient("registerRenderers", new Class<?>[0], new Object[0]);
            }
        }

        // 服务端配置快照接收：登录后立即替换本地配置
        ClientPlayNetworking.registerGlobalReceiver(ConfigSync.CHANNEL, (client, handler, buf, responseSender) -> {
            Config.Data data = Config.Data.decode(buf);
            client.execute(() -> Config.applyRemote(data));
        });
    }
}
