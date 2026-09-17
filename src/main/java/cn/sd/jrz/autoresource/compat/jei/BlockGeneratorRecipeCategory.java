package cn.sd.jrz.autoresource.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
 *     <li><b>不画自己的 JEI 贴图</b>：背景是 {@code createBlankDrawable}，但<b>必须显式覆写
 *         {@link #getBackground()}</b>——该方法虽自 15.20 起标记待删除，老版本仍靠它确定卡片范围，
 *         不覆写时底衬会小于实际布局。</li>
 *     <li><b>说明文字不用 {@code extras.addText}</b>：那个签名的参数含义在 15.20 / 15.59 之间是反的，
 *         一律在 {@link #draw} 里自己画。</li>
 * </ul>
 */
public class BlockGeneratorRecipeCategory implements IRecipeCategory<BlockGeneratorRecipe> {
    private static final Logger log = LoggerFactory.getLogger(BlockGeneratorRecipeCategory.class);

    /** 单个槽位边长（JEI 的 slot.png 是 18×18；产物格也必须用同一尺寸，不要用 26×26 的输出槽贴图） */
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
    private final int noteY;
    private final int width;
    private final int height;

    public BlockGeneratorRecipeCategory(@Nonnull IGuiHelper guiHelper, @Nonnull RecipeType<BlockGeneratorRecipe> recipeType,
                                        @Nonnull Component title, @Nonnull ItemStack iconStack) {
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableItemStack(iconStack);
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
     * 显式给出卡片区域（空白 drawable，尺寸即布局尺寸）。
     * <p>
     * 该方法在 JEI 15.20 起被标记为待删除，但**老版本仍然依赖它来确定配方卡片的范围**：
     * 不覆写时不同小版本画出来的底衬与实际布局不一致。
     */
    @Override
    @Nonnull
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setRecipe(@Nonnull IRecipeLayoutBuilder builder, @Nonnull BlockGeneratorRecipe recipe, @Nonnull IFocusGroup focuses) {
        List<ItemStack> products = recipe.products();
        for (int i = 0; i < PER_PAGE; i++) {
            IRecipeSlotBuilder slot = builder
                    .addOutputSlot(PAD + (i % COLS) * SLOT, PAD + (i / COLS) * SLOT)
                    .setStandardSlotBackground();
            if (i < products.size()) {
                slot.addItemStack(products.get(i));
            }
        }
    }

    @Override
    public void draw(@Nonnull BlockGeneratorRecipe recipe, @Nonnull IRecipeSlotsView slotsView, @Nonnull GuiGraphics guiGraphics,
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
                guiGraphics.drawString(font, lines.get(i), Math.max(PAD, x),
                        noteY + i * LINE_H, NOTE_COLOR, false);
            }
        } catch (Throwable e) {
            log.warn("JEI：{} 的配方说明文字绘制失败", recipeType.getUid(), e);
        }
    }
}
