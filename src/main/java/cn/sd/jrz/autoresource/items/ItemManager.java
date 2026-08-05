package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.AutoResource;
import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemManager {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AutoResource.MODID);

    private static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("autoresource", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.autoresource"))
            .icon(() -> new ItemStack(Registration.ENERGY_GENERATOR_FE_ITEM.get()))
            .displayItems((parameters, output) -> {
                output.accept(Registration.ENERGY_GENERATOR_FE_ITEM.get());
                output.accept(Registration.LIQUID_GENERATOR_WATER_ITEM.get());
                output.accept(Registration.LIQUID_GENERATOR_LAVA_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_ITEM.get());
            })
            .build()
    );

    public static void init(IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
    }
}
