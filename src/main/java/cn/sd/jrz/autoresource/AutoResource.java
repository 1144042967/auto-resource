package cn.sd.jrz.autoresource;

import cn.sd.jrz.autoresource.items.ItemManager;
import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraftforge.fml.common.Mod;

@Mod(AutoResource.MODID)
public class AutoResource {
    public static final String MODID = "autoresource";

    public AutoResource() {
        Config.init();
        Registration.init();
        ItemManager.init();
    }
}
