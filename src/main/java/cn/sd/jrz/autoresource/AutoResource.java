package cn.sd.jrz.autoresource;

import cn.sd.jrz.autoresource.items.ItemManager;
import cn.sd.jrz.autoresource.setup.Registration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AutoResource.MODID)
public class AutoResource {
    public static final String MODID = "autoresource";

    public AutoResource() {
        Config.init();
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        Registration.init(bus);
        ItemManager.init(bus);
    }
}
