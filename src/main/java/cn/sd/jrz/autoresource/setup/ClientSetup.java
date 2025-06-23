package cn.sd.jrz.autoresource.setup;

import cn.sd.jrz.autoresource.client.gui.GeneratorContainerScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientSetup {
    public static void init(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {// Attach our container to the screen
            MenuScreens.register(Registration.ENDER_GENERATOR_CONTAINER.get(), GeneratorContainerScreen::new);
        });
    }
}
