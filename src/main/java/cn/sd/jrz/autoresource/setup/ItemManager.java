package cn.sd.jrz.autoresource.setup;

import cn.sd.jrz.autoresource.AutoResource;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * 创造模式标签页注册：顺序 方块生成 → 水生成 → 岩浆生成 → FE 发电（前置 teamreborn energy 加载时）。
 * 主题图标为方块生成机（不能用 FE 发电机做图标：前置未装时其物品为 null）。
 */
public class ItemManager {

    public static void init() {
        ResourceLocation tabId = ResourceLocation.fromNamespaceAndPath(AutoResource.MODID, "autoresource");
        CreativeModeTab tab = FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.autoresource"))
                .icon(() -> new ItemStack(Registration.BLOCK_GENERATOR_ITEM))
                .displayItems((parameters, output) -> {
                    output.accept(Registration.BLOCK_GENERATOR_ITEM);
                    output.accept(Registration.LIQUID_GENERATOR_WATER_ITEM);
                    output.accept(Registration.LIQUID_GENERATOR_LAVA_ITEM);
                    // FE 发电：仅当 teamreborn energy 加载时显示
                    if (Registration.ENERGY_GENERATOR_FE_ITEM != null) {
                        output.accept(Registration.ENERGY_GENERATOR_FE_ITEM);
                    }
                })
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, tabId, tab);
    }
}
