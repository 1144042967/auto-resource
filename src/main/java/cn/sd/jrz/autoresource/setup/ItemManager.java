package cn.sd.jrz.autoresource.setup;

import cn.sd.jrz.autoresource.AutoResource;
import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ItemManager {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AutoResource.MODID);

    private static final RegistryObject<CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("autoresource", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.autoresource"))
            .icon(() -> new ItemStack(Registration.ENERGY_GENERATOR_FE_ITEM.get()))
            .displayItems((parameters, output) -> {
                output.accept(Registration.ENERGY_GENERATOR_FE_ITEM.get());
                output.accept(Registration.LIQUID_GENERATOR_WATER_ITEM.get());
                output.accept(Registration.LIQUID_GENERATOR_LAVA_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_ITEM.get());
                // Create 联动：仅当 Create 加载时显示水车马达
                if (Registration.WATER_WHEEL_MOTOR_ITEM != null) {
                    output.accept(Registration.WATER_WHEEL_MOTOR_ITEM.get());
                }
            })
            .build()
    );

    public static void init(FMLJavaModLoadingContext context) {
        CREATIVE_MODE_TABS.register(context.getModEventBus());
    }
}
