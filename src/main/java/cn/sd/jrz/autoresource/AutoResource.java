package cn.sd.jrz.autoresource;

import cn.sd.jrz.autoresource.items.ItemManager;
import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AutoResource.MODID)
public class AutoResource {
    public static final String MODID = "autoresource";

    public AutoResource() {
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
        Config.init();
        Registration.init(context);
        ItemManager.init(context);
    }
}
