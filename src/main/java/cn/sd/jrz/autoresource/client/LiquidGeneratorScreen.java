package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.menu.LiquidGeneratorMenu;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * 流体生成器 GUI（水源机/岩浆机）。
 * <p>
 * 展示当前流体量、当前产量、下次增长量、增长百分比（含进度条），
 * 并提供六个流体传输面的独立开关以及输入/输出槽。数值使用单位缩写（K/M/G/T/P/E）避免 long 大数溢出。
 */
@OnlyIn(Dist.CLIENT)
public class LiquidGeneratorScreen extends AbstractGeneratorScreen<LiquidGeneratorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("autoresource", "textures/gui/liquid_generator_gui.png");

    private StateButton faceDown;
    private StateButton faceUp;
    private StateButton faceNorth;
    private StateButton faceSouth;
    private StateButton faceWest;
    private StateButton faceEast;
    private StateButton placeButton;

    public LiquidGeneratorScreen(LiquidGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 233;
        this.inventoryLabelY = 137;
    }

    @Override
    protected void init() {
        super.init();
        // 六个传输面（宽 44 以容纳英文面名）
        this.faceDown = new StateButton(this.leftPos + 17, this.topPos + 76, 44, 12, this.menu.isFaceEnabled(Direction.DOWN), Component.translatable("screen.autoresource.energy_generator.face.down"), button -> sendButton(LiquidGeneratorMenu.BUTTON_TRANSFER_DOWN));
        this.faceUp = new StateButton(this.leftPos + 66, this.topPos + 76, 44, 12, this.menu.isFaceEnabled(Direction.UP), Component.translatable("screen.autoresource.energy_generator.face.up"), button -> sendButton(LiquidGeneratorMenu.BUTTON_TRANSFER_UP));
        this.faceNorth = new StateButton(this.leftPos + 115, this.topPos + 76, 44, 12, this.menu.isFaceEnabled(Direction.NORTH), Component.translatable("screen.autoresource.energy_generator.face.north"), button -> sendButton(LiquidGeneratorMenu.BUTTON_TRANSFER_NORTH));
        this.faceSouth = new StateButton(this.leftPos + 17, this.topPos + 92, 44, 12, this.menu.isFaceEnabled(Direction.SOUTH), Component.translatable("screen.autoresource.energy_generator.face.south"), button -> sendButton(LiquidGeneratorMenu.BUTTON_TRANSFER_SOUTH));
        this.faceWest = new StateButton(this.leftPos + 66, this.topPos + 92, 44, 12, this.menu.isFaceEnabled(Direction.WEST), Component.translatable("screen.autoresource.energy_generator.face.west"), button -> sendButton(LiquidGeneratorMenu.BUTTON_TRANSFER_WEST));
        this.faceEast = new StateButton(this.leftPos + 115, this.topPos + 92, 44, 12, this.menu.isFaceEnabled(Direction.EAST), Component.translatable("screen.autoresource.energy_generator.face.east"), button -> sendButton(LiquidGeneratorMenu.BUTTON_TRANSFER_EAST));
        this.addRenderableWidget(this.faceDown);
        this.addRenderableWidget(this.faceUp);
        this.addRenderableWidget(this.faceNorth);
        this.addRenderableWidget(this.faceSouth);
        this.addRenderableWidget(this.faceWest);
        this.addRenderableWidget(this.faceEast);
        // "下方生成流体"按钮（位于输出槽下方，开启后向机器下方空气方块放置对应流体）
        this.placeButton = new StateButton(this.leftPos + 72, this.topPos + 133, 96, 12, this.menu.isPlaceFluidBelow(), Component.translatable("screen.autoresource.liquid_generator.place_below"), button -> sendButton(LiquidGeneratorMenu.BUTTON_PLACE_FLUID));
        this.addRenderableWidget(this.placeButton);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        // 增长进度条（颜色随对应流体变化：水源机蓝色、岩浆机岩浆橙）
        int trackLeft = this.leftPos + 12;
        int trackRight = this.leftPos + 164;
        int trackTop = this.topPos + 60;
        guiGraphics.fill(trackLeft, trackTop, trackRight, trackTop + 4, 0xFF555555);
        int percent = growthPercent();
        if (percent > 0) {
            int fill = (trackRight - trackLeft) * percent / 100;
            guiGraphics.fill(trackLeft, trackTop, trackLeft + fill, trackTop + 4, progressColor());
        }
    }

    /**
     * 进度条填充色：根据本机流体返回对应颜色（水=蓝、岩浆=橙），其余默认绿色
     */
    private int progressColor() {
        Fluid fluid = this.menu.getFluid();
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
            return 0xFF3F76E4; // 水蓝色
        }
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            return 0xFFFF8800; // 岩浆橙
        }
        return 0xFF00AA00;
    }

    @Override
    protected void renderLabels(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        LiquidGeneratorMenu menu = this.menu;
        boolean maxed = menu.getOutput() >= menu.getMax();
        // 信息面板（流体/产量/下次增长均以 B 为单位，大数值用单位缩写）
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.liquid_generator.liquid", formatBuckets(menu.getLiquid())), 12, 19, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.liquid_generator.output", formatBuckets(menu.getOutput())), 12, 29, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.liquid_generator.next", maxed ? Component.translatable("screen.autoresource.liquid_generator.next_max") : Component.literal(formatBuckets(menu.getStep()))), 12, 39, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.liquid_generator.growth", growthPercent()), 12, 49, TEXT_COLOR, false);
        // 输入槽标签（贴近输入槽右侧，与发电机槽位标签位置一致）
        Component inputLabel = Component.translatable("screen.autoresource.liquid_generator.input");
        guiGraphics.drawString(this.font, inputLabel, 28, 116, TEXT_COLOR, false);
        // 输出槽标签：右对齐贴近输出槽
        Component outputLabel = Component.translatable("screen.autoresource.liquid_generator.output_slot");
        guiGraphics.drawString(this.font, outputLabel, 150 - this.font.width(outputLabel), 116, TEXT_COLOR, false);
    }

    @Override
    protected void refreshButtonStates() {
        this.faceDown.setState(this.menu.isFaceEnabled(Direction.DOWN));
        this.faceUp.setState(this.menu.isFaceEnabled(Direction.UP));
        this.faceNorth.setState(this.menu.isFaceEnabled(Direction.NORTH));
        this.faceSouth.setState(this.menu.isFaceEnabled(Direction.SOUTH));
        this.faceWest.setState(this.menu.isFaceEnabled(Direction.WEST));
        this.faceEast.setState(this.menu.isFaceEnabled(Direction.EAST));
        this.placeButton.setState(this.menu.isPlaceFluidBelow());
    }

    /**
     * 以 B（桶）为单位展示流体数量：小数值保留两位小数，大数值使用单位缩写
     */
    private static String formatBuckets(long mb) {
        if (mb < 10_000) {
            return String.format("%.2f", mb / 1000.0);
        }
        return Tool.formatLong(mb / 1000);
    }
}
