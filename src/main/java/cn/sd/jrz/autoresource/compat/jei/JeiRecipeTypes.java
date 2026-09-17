package cn.sd.jrz.autoresource.compat.jei;

import cn.sd.jrz.autoresource.AutoResource;
import mezz.jei.api.recipe.RecipeType;

/**
 * 本模组注册到 JEI 的配方类型。
 */
public final class JeiRecipeTypes {

    /** 方块生成机：只列出配置判定为可生成的方块（无输入列，按页拆分） */
    public static final RecipeType<BlockGeneratorRecipe> BLOCK_GENERATOR =
            RecipeType.create(AutoResource.MODID, "block_generator", BlockGeneratorRecipe.class);

    private JeiRecipeTypes() {
    }
}
