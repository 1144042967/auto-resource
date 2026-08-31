package cn.sd.jrz.autoresource;

import cn.sd.jrz.autoresource.network.ConfigSync;
import cn.sd.jrz.autoresource.setup.ItemManager;
import cn.sd.jrz.autoresource.setup.Registration;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoResource implements ModInitializer {
    public static final String MODID = "autoresource";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    @Override
    public void onInitialize() {
        Config.load();
        // 中心注册表：依次注册 FE 发电机（前置 teamreborn energy 加载时）、机械动力联动（前置 Create 加载时）、本体三件套
        Registration.init();
        ItemManager.init();
        // 玩家登录时向客户端下发配置快照
        ConfigSync.init();
    }

    public static String modId() {
        return MODID;
    }
}
