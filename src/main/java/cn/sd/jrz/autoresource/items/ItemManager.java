package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ItemManager {
    public static final CreativeModeTab CREATIVE_MODE_TABS = new CreativeModeTab(12, "autoresource") {

        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Registration.ENERGY_GENERATOR_FE_ITEM.get());
        }
    };
}
