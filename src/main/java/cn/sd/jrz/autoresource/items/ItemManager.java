package cn.sd.jrz.autoresource.items;

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
            .title(Component.translatable("itemGroup.exponentialpower"))
            .icon(() -> new ItemStack(Registration.ENERGY_GENERATOR_FE_ITEM.get()))
            .displayItems((parameters, output) -> {
                output.accept(Registration.ENERGY_GENERATOR_FE_ITEM.get());
                output.accept(Registration.LIQUID_GENERATOR_WATER_ITEM.get());
                output.accept(Registration.LIQUID_GENERATOR_LAVA_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_DIRT_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_COBBLESTONE_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_STONE_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_SMOOTH_STONE_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_CLAY_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_SAND_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_GRAVEL_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_GRANITE_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_DIORITE_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_ANDESITE_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_CALCITE_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_TUFF_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_COBBLED_DEEPSLATE_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_PRISMARINE_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_OBSIDIAN_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_NETHERRACK_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_SOUL_SAND_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_SOUL_SOIL_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_BLACKSTONE_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_BASALT_ITEM.get());
                output.accept(Registration.BLOCK_GENERATOR_END_STONE_ITEM.get());
            })
            .build()
    );

    public static void init(FMLJavaModLoadingContext context) {
        CREATIVE_MODE_TABS.register(context.getModEventBus());
    }
}
