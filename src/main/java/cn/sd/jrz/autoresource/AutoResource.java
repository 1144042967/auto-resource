package cn.sd.jrz.autoresource;

import cn.sd.jrz.autoresource.setup.ItemManager;
import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AutoResource.MODID)
public class AutoResource {
    public static final String MODID = "autoresource";

    public AutoResource() {
        // 47.2.20 的 FMLModContainer 只支持无参构造器（不支持 FMLJavaModLoadingContext 构造器注入），
        // 需通过静态 get() 获取加载上下文。
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
        Config.init(context);
        Registration.init(context);
        ItemManager.init(context);
    }
}
