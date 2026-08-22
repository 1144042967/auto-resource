package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.menu.LiquidGeneratorMenu;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nonnull;

/**
 * 流体生成器 GUI（水源机/岩浆机，26.x 适配）。
 * <p>
 * 展示当前流体量、当前产量、下次增长量、增长百分比（含进度条），
 * 并提供主动输出总开关、六个流体传输面的独立开关以及输入/输出槽。
 */
public class LiquidGeneratorScreen extends AbstractGeneratorScreen<LiquidGeneratorMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("autoresource", "textures/gui/liquid_generator_gui.png");

    private StateButton outputButton;
    private StateButton faceDown;
    private StateButton faceUp;
    private StateButton faceNorth;
    private StateButton faceSouth;
    private StateButton faceWest;
    private StateButton faceEast;
    private StateButton placeButton;

    public LiquidGeneratorScreen(LiquidGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 233);
        this.inventoryLabelY = 137;
    }

    @Override
    protected void init() {
        super.init();
        // 主动输出总开关（GUI 右上角）
        this.outputButton = new StateButton(this.leftPos + 120, this.topPos + 20, 44, 12, this.menu.isOutputEnabled(), Component.translatable("screen.autoresource.liquid_generator.output_toggle"), _ -> sendButton(LiquidGeneratorMenu.BUTTON_OUTPUT));
        this.addRenderableWidget(this.outputButton);
        this.faceDown = new StateButton(this.leftPos + 17, this.topPos + 76, 44, 12, this.menu.isFaceEnabled(Direction.DOWN), Component.translatable("screen.autoresource.energy_generator.face.down"), _ -> sendButton(LiquidGeneratorMenu.BUTTON_TRANSFER_DOWN));
        this.faceUp = new StateButton(this.leftPos + 66, this.topPos + 76, 44, 12, this.menu.isFaceEnabled(Direction.UP), Component.translatable("screen.autoresource.energy_generator.face.up"), _ -> sendButton(LiquidGeneratorMenu.BUTTON_TRANSFER_UP));
        this.faceNorth = new StateButton(this.leftPos + 115, this.topPos + 76, 44, 12, this.menu.isFaceEnabled(Direction.NORTH), Component.translatable("screen.autoresource.energy_generator.face.north"), _ -> sendButton(LiquidGeneratorMenu.BUTTON_TRANSFER_NORTH));
        this.faceSouth = new StateButton(this.leftPos + 17, this.topPos + 92, 44, 12, this.menu.isFaceEnabled(Direction.SOUTH), Component.translatable("screen.autoresource.energy_generator.face.south"), _ -> sendButton(LiquidGeneratorMenu.BUTTON_TRANSFER_SOUTH));
        this.faceWest = new StateButton(this.leftPos + 66, this.topPos + 92, 44, 12, this.menu.isFaceEnabled(Direction.WEST), Component.translatable("screen.autoresource.energy_generator.face.west"), _ -> sendButton(LiquidGeneratorMenu.BUTTON_TRANSFER_WEST));
        this.faceEast = new StateButton(this.leftPos + 115, this.topPos + 92, 44, 12, this.menu.isFaceEnabled(Direction.EAST), Component.translatable("screen.autoresource.energy_generator.face.east"), _ -> sendButton(LiquidGeneratorMenu.BUTTON_TRANSFER_EAST));
        this.addRenderableWidget(this.faceDown);
        this.addRenderableWidget(this.faceUp);
        this.addRenderableWidget(this.faceNorth);
        this.addRenderableWidget(this.faceSouth);
        this.addRenderableWidget(this.faceWest);
        this.addRenderableWidget(this.faceEast);
        this.placeButton = new StateButton(this.leftPos + 72, this.topPos + 133, 96, 12, this.menu.isPlaceFluidBelow(), Component.translatable("screen.autoresource.liquid_generator.place_below"), _ -> sendButton(LiquidGeneratorMenu.BUTTON_PLACE_FLUID));
        this.addRenderableWidget(this.placeButton);
    }

    @Override
    public void extractBackground(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        // 增长进度条（颜色随对应流体变化：水源机蓝色、岩浆机岩浆橙）
        drawGrowthBar(guiGraphics, this.topPos + 60, progressColor());
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
    protected void extractLabels(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        LiquidGeneratorMenu menu = this.menu;
        boolean maxed = menu.getOutput() >= menu.getMax();
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.liquid_generator.liquid", formatBuckets(menu.getLiquid())), 12, 19, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.liquid_generator.output", formatBuckets(menu.getOutput())), 12, 29, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.liquid_generator.next", maxed ? Component.translatable("screen.autoresource.liquid_generator.next_max") : Component.literal(formatBuckets(menu.getStep()))), 12, 39, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.liquid_generator.growth", growthPercent()), 12, 49, TEXT_COLOR, false);
        Component inputLabel = Component.translatable("screen.autoresource.liquid_generator.input");
        guiGraphics.text(this.font, inputLabel, 28, 116, TEXT_COLOR, false);
        Component outputLabel = Component.translatable("screen.autoresource.liquid_generator.output_slot");
        guiGraphics.text(this.font, outputLabel, 150 - this.font.width(outputLabel), 116, TEXT_COLOR, false);
    }

    @Override
    public void extractRenderState(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        this.outputButton.setState(this.menu.isOutputEnabled());
        this.faceDown.setState(this.menu.isFaceEnabled(Direction.DOWN));
        this.faceUp.setState(this.menu.isFaceEnabled(Direction.UP));
        this.faceNorth.setState(this.menu.isFaceEnabled(Direction.NORTH));
        this.faceSouth.setState(this.menu.isFaceEnabled(Direction.SOUTH));
        this.faceWest.setState(this.menu.isFaceEnabled(Direction.WEST));
        this.faceEast.setState(this.menu.isFaceEnabled(Direction.EAST));
        this.placeButton.setState(this.menu.isPlaceFluidBelow());
    }

    private static String formatBuckets(long mb) {
        if (mb < 10_000) {
            return String.format("%.2f", mb / 1000.0);
        }
        return Tool.formatLong(mb / 1000);
    }
}
