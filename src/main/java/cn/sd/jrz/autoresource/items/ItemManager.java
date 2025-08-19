package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class ItemManager {
    public static final CreativeModeTab CREATIVE_MODE_TABS = new CreativeModeTab(12, "autoresource") {

        @Override
        public @Nonnull ItemStack makeIcon() {
            return new ItemStack(Registration.ENERGY_GENERATOR_FE_ITEM.get());
        }
    };
}
