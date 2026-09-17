package cn.sd.jrz.autoresource;

import cn.sd.jrz.autoresource.setup.ItemManager;
import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AutoResource.MODID)
public class AutoResource {
    public static final String MODID = "autoresource";

    public AutoResource() {
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
        Config.init(ModLoadingContext.get());
        Registration.init(context);
        ItemManager.init(context);
    }
}
