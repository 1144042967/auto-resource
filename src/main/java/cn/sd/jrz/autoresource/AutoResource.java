package cn.sd.jrz.autoresource;

import cn.sd.jrz.autoresource.items.ItemManager;
import cn.sd.jrz.autoresource.setup.ClientSetup;
import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AutoResource.MODID)
public class AutoResource {
    public static final String MODID = "autoresource";
    public static final Logger LOGGER = LogManager.getLogger();

    public AutoResource(FMLJavaModLoadingContext context) {
        Config.init(context);
        Registration.init(context);
        IEventBus bus = context.getModEventBus();
        bus.addListener(ClientSetup::init);
        ItemManager.init(context);
    }
}
