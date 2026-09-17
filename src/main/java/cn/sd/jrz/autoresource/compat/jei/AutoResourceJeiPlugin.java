package cn.sd.jrz.autoresource.compat.jei;

import cn.sd.jrz.autoresource.AutoResource;
import cn.sd.jrz.autoresource.items.BlockGeneratorItem;
import cn.sd.jrz.autoresource.setup.Registration;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * JEI 插件：把方块生成机可生成的方块展示出来。
 * <p>
 * 样式对齐 alltheimbaium 的存储方块制造机：**没有输入列**，一整片 8×6 = 48 件一页的方块网格，
 * 底部两行说明（总数、页码），超过一页就拆成多条配方。
 * <p>
 * <b>数据来源</b>：与标记槽的判定同源（配置 {@code block_generator.items}，含 {@code #} 标签展开，
 * 走 {@link BlockGeneratorItem#getSupportedItems()} 的那份缓存）。JEI 跑在客户端，而这是 SERVER 配置——
 * Forge 会把 SERVER 配置同步给客户端（{@code net.minecraftforge.network.ConfigSync}），所以客户端读得到；
 * 标签内容同样由原版随连接同步。因此"整合包改了配置但没重启客户端"是唯一读不到正确值的情况。
 * <p>
 * 收集过程单独 try-catch：解析失败只让该分类空着，绝不让 JEI 崩在配方页上。
 */
@JeiPlugin
public class AutoResourceJeiPlugin implements IModPlugin {
    private static final Logger log = LoggerFactory.getLogger(AutoResourceJeiPlugin.class);

    /** 可生成方块列表最多分多少页（防止极端整合包塞进成千上万条配方） */
    private static final int MAX_PAGES = 128;

    @Override
    @Nonnull
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(AutoResource.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(@Nonnull IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new BlockGeneratorRecipeCategory(
                registration.getJeiHelpers().getGuiHelper(),
                JeiRecipeTypes.BLOCK_GENERATOR,
                Component.translatable("block.autoresource.block_generator"),
                itemIcon(Registration.BLOCK_GENERATOR_ITEM.get())));
    }

    @Override
    public void registerRecipes(@Nonnull IRecipeRegistration registration) {
        registration.addRecipes(JeiRecipeTypes.BLOCK_GENERATOR, safe(this::blockGeneratorRecipes));
    }

    @Override
    public void registerRecipeCatalysts(@Nonnull IRecipeCatalystRegistration registration) {
        // 让方块生成机出现在该配方页的"可制作"列表里
        registration.addRecipeCatalyst(itemIcon(Registration.BLOCK_GENERATOR_ITEM.get()), JeiRecipeTypes.BLOCK_GENERATOR);
    }

    /**
     * 方块生成机：把配置判定为"可生成"的方块按 8×6 分页列出来（没有输入列）。
     * <p>
     * 顺序沿用配置里的书写顺序（也是机器物品 tooltip 里的顺序），分页因此稳定。
     */
    @Nonnull
    private List<BlockGeneratorRecipe> blockGeneratorRecipes() {
        List<ItemStack> accepted = new ArrayList<>();
        for (Item item : BlockGeneratorItem.getSupportedItems()) {
            if (item != null && item != Items.AIR) {
                accepted.add(new ItemStack(item));
            }
        }
        if (accepted.isEmpty()) {
            log.warn("JEI：方块生成机没有解析到可生成的方块（block_generator.items 配置是否已同步到客户端？）");
            return List.of();
        }
        int perPage = BlockGeneratorRecipeCategory.PER_PAGE;
        int pages = (accepted.size() + perPage - 1) / perPage;
        if (pages > MAX_PAGES) {
            log.warn("JEI：方块生成机可生成的方块 {} 件，超过 {} 页上限，只展示前 {} 件",
                    accepted.size(), MAX_PAGES, MAX_PAGES * perPage);
            pages = MAX_PAGES;
        }
        List<BlockGeneratorRecipe> out = new ArrayList<>(pages);
        for (int page = 0; page < pages; page++) {
            List<ItemStack> products = new ArrayList<>(perPage);
            for (int i = page * perPage; i < Math.min(accepted.size(), (page + 1) * perPage); i++) {
                products.add(accepted.get(i));
            }
            List<Component> notes = List.of(
                    Component.translatable("jei.autoresource.block_generator.total", accepted.size()),
                    Component.translatable("jei.autoresource.block_generator.page", page + 1, pages));
            out.add(new BlockGeneratorRecipe(products, notes));
        }
        return out;
    }

    /** 收集失败只让该分类空着，不让 JEI 的配方装载整体崩掉 */
    @Nonnull
    private <T> List<T> safe(@Nonnull Supplier<List<T>> supplier) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            log.warn("JEI：方块生成机配方收集失败，该分类将为空", e);
            return List.of();
        }
    }

    @Nonnull
    private static ItemStack itemIcon(@Nonnull Item item) {
        return new ItemStack(item);
    }
}
