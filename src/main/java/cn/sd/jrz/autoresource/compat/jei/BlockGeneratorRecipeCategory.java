package cn.sd.jrz.autoresource.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * 方块生成机的 JEI 配方页（样式对齐 alltheimbaium 的存储方块制造机）。
 * <p>
 * 版面是一整片 {@value #COLS}×{@value #ROWS} 的方块网格，**没有输入列、没有箭头**，网格直接贴左边；
 * 底部留两行说明（先"共 N 个可生成方块"、再"第 X/Y 页"）。三条固定规则：
 * <ul>
 *     <li><b>槽位始终整片画出</b>：{@code COLS × ROWS} 个槽位全部建出来，有方块的填物品、没方块的留空槽背景，
 *         按自然顺序（从左到右、从上到下）排列，不做短行居中。</li>
 *     <li><b>槽位背景用 {@code guiHelper.getSlotDrawable()}</b>：JEI 11.x 没有 {@code setStandardSlotBackground()}，
 *         也不要用 26×26 的输出槽贴图（那个尺寸是原版工作台产物槽那套，会与 18px 网格重叠）。</li>
 *     <li><b>说明文字在 {@link #draw} 里自己画</b>：JEI 11.2 的卡片没有额外的文字插槽，且不同小版本
 *         的 {@code addText} 语义并不一致，自己画最稳。</li>
 * </ul>
 * <p>
 * <b>1.19.2 用的是 JEI 11.x</b>：{@code IRecipeCategory} 只有 {@code getBackground()}（无 {@code getWidth/getHeight}），
 * 且 {@code draw(...)} 的第三个参数是 {@link PoseStack}（不是 1.20 时代的 {@code GuiGraphics}）。
 */
public class BlockGeneratorRecipeCategory implements IRecipeCategory<BlockGeneratorRecipe> {
    private static final Logger log = LoggerFactory.getLogger(BlockGeneratorRecipeCategory.class);

    /** 单个槽位边长（JEI 的 slot.png 是 18×18；产物格也必须用同一尺寸） */
    private static final int SLOT = 18;
    /** 卡片四周留白 */
    private static final int PAD = 4;
    /** 一页的列数 × 行数 = 48 个槽位 */
    private static final int COLS = 8;
    private static final int ROWS = 6;
    /** 说明文字最多几行、行高，以及颜色（JEI 卡片底衬偏亮，用原版深灰） */
    private static final int NOTE_LINES = 2;
    private static final int LINE_H = 10;
    private static final int NOTE_COLOR = 0xFF404040;

    /** 一页能摆下的方块数（方块生成机按这个数分页） */
    public static final int PER_PAGE = COLS * ROWS;

    private final RecipeType<BlockGeneratorRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;
    private final IDrawable slotBackground;
    private final int noteY;
    private final int width;
    private final int height;

    public BlockGeneratorRecipeCategory(@Nonnull IGuiHelper guiHelper, @Nonnull RecipeType<BlockGeneratorRecipe> recipeType,
                                        @Nonnull Component title, @Nonnull ItemStack iconStack) {
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableItemStack(iconStack);
        // JEI 11.x 没有 setStandardSlotBackground()，标准槽位贴图从这里取
        this.slotBackground = guiHelper.getSlotDrawable();
        // 没有输入列也没有箭头，方块网格与说明文字依次往下排
        this.noteY = PAD + ROWS * SLOT + PAD;
        this.width = PAD + COLS * SLOT + PAD;
        this.height = noteY + NOTE_LINES * LINE_H + PAD;
        this.background = guiHelper.createBlankDrawable(width, height);
    }

    @Override
    @Nonnull
    public RecipeType<BlockGeneratorRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    @Nonnull
    public Component getTitle() {
        return title;
    }

    @Override
    @Nonnull
    public IDrawable getIcon() {
        return icon;
    }

    /**
     * 卡片区域（空白 drawable，尺寸即布局尺寸）。JEI 靠它确定配方卡片的范围。
     */
    @Override
    @Nonnull
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(@Nonnull IRecipeLayoutBuilder builder, @Nonnull BlockGeneratorRecipe recipe, @Nonnull IFocusGroup focuses) {
        List<ItemStack> products = recipe.products();
        for (int i = 0; i < PER_PAGE; i++) {
            IRecipeSlotBuilder slot = builder
                    .addSlot(RecipeIngredientRole.OUTPUT, PAD + (i % COLS) * SLOT, PAD + (i / COLS) * SLOT)
                    .setBackground(slotBackground, 0, 0);
            if (i < products.size()) {
                slot.addItemStack(products.get(i));
            }
        }
    }

    @Override
    public void draw(@Nonnull BlockGeneratorRecipe recipe, @Nonnull IRecipeSlotsView slotsView, @Nonnull PoseStack poseStack,
                     double mouseX, double mouseY) {
        try {
            Font font = Minecraft.getInstance().font;
            if (font == null) {
                return;
            }
            List<Component> lines = new ArrayList<>();
            for (Component line : recipe.noteLines()) {
                if (lines.size() >= NOTE_LINES) {
                    break;
                }
                lines.addAll(JeiText.wrap(line, width - PAD * 2, NOTE_LINES - lines.size()));
            }
            for (int i = 0; i < lines.size(); i++) {
                // 说明文字整行居中
                int x = (width - font.width(lines.get(i))) / 2;
                font.draw(poseStack, lines.get(i), Math.max(PAD, x), noteY + i * LINE_H, NOTE_COLOR);
            }
        } catch (Throwable e) {
            log.warn("JEI：{} 的配方说明文字绘制失败", recipeType.getUid(), e);
        }
    }
}
