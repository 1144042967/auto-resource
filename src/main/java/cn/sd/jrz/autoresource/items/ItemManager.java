package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ItemManager {
    public static final ItemGroup CREATIVE_MODE_TABS = new ItemGroup(12, "autoresource") {
        @Override
        @Nonnull
        public ItemStack makeIcon() {
            return new ItemStack(Registration.ENERGY_GENERATOR_FE_ITEM.get());
        }
    };
}
