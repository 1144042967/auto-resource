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
        Registration.init();
        ItemManager.init();
        // 玩家登录时向客户端下发配置快照
        ConfigSync.init();
    }

    public static String modId() {
        return MODID;
    }
}
