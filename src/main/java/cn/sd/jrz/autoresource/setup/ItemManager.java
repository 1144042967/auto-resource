package cn.sd.jrz.autoresource.setup;

import cn.sd.jrz.autoresource.AutoResource;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * 创造模式标签页注册：顺序 FE 发电 → 水生成 → 岩浆生成 → 方块生成
 * <p>26.3 不含水车马达（机械动力飞越版无对应版本）
 */
public class ItemManager {
    private static CreativeModeTab TAB;

    public static void init() {
        Identifier tabId = Identifier.fromNamespaceAndPath(AutoResource.MODID, "autoresource");
        TAB = FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.autoresource"))
                .icon(() -> new ItemStack(Registration.ENERGY_GENERATOR_FE_ITEM))
                .displayItems((parameters, output) -> {
                    output.accept(Registration.ENERGY_GENERATOR_FE_ITEM);
                    output.accept(Registration.LIQUID_GENERATOR_WATER_ITEM);
                    output.accept(Registration.LIQUID_GENERATOR_LAVA_ITEM);
                    output.accept(Registration.BLOCK_GENERATOR_ITEM);
                })
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, tabId, TAB);
    }
}
