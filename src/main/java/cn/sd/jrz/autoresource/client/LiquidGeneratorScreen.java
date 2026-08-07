package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.menu.LiquidGeneratorMenu;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 流体生成器 GUI（水源机/岩浆机）。
 */
@OnlyIn(Dist.CLIENT)
public class LiquidGeneratorScreen extends AbstractContainerScreen<LiquidGeneratorMenu> {
    private static final ResourceLocation TEXTURE = Identifier.fromNamespaceAndPath("autoresource", "textures/gui/liquid_generator_gui.png");
    private static final int TEXT_COLOR = 4210752; // 0x404040 深灰

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
        this.placeButton = new StateButton(this.leftPos + 72, this.topPos + 133, 96, 12, this.menu.isPlaceFluidBelow(), Component.translatable("screen.autoresource.liquid_generator.place_below"), button -> sendButton(LiquidGeneratorMenu.BUTTON_PLACE_FLUID));
        this.addRenderableWidget(this.placeButton);
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.connection.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, id));
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
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

    private int progressColor() {
        Fluid fluid = this.menu.getFluid();
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
            return 0xFF3F76E4;
        }
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            return 0xFFFF8800;
        }
        return 0xFF00AA00;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        LiquidGeneratorMenu menu = this.menu;
        boolean maxed = menu.getOutput() >= menu.getMax();
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.liquid_generator.liquid", formatBuckets(menu.getLiquid())), 12, 19, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.liquid_generator.output", formatBuckets(menu.getOutput())), 12, 29, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.liquid_generator.next", maxed ? Component.translatable("screen.autoresource.liquid_generator.next_max") : Component.literal(formatBuckets(menu.getStep()))), 12, 39, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.liquid_generator.growth", growthPercent()), 12, 49, TEXT_COLOR, false);
        Component inputLabel = Component.translatable("screen.autoresource.liquid_generator.input");
        guiGraphics.drawString(this.font, inputLabel, 28, 116, TEXT_COLOR, false);
        Component outputLabel = Component.translatable("screen.autoresource.liquid_generator.output_slot");
        guiGraphics.drawString(this.font, outputLabel, 150 - this.font.width(outputLabel), 116, TEXT_COLOR, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        this.faceDown.setState(this.menu.isFaceEnabled(Direction.DOWN));
        this.faceUp.setState(this.menu.isFaceEnabled(Direction.UP));
        this.faceNorth.setState(this.menu.isFaceEnabled(Direction.NORTH));
        this.faceSouth.setState(this.menu.isFaceEnabled(Direction.SOUTH));
        this.faceWest.setState(this.menu.isFaceEnabled(Direction.WEST));
        this.faceEast.setState(this.menu.isFaceEnabled(Direction.EAST));
        this.placeButton.setState(this.menu.isPlaceFluidBelow());
    }

    private int growthPercent() {
        if (this.menu.getOutput() >= this.menu.getMax()) {
            return 100;
        }
        int second = Math.max(1, this.menu.getSecond());
        double percent = this.menu.getTickCount() / (second * 20.0) * 100.0;
        return (int) Math.max(0, Math.min(100, percent));
    }

    private static String formatBuckets(long mb) {
        if (mb < 10_000) {
            return String.format("%.2f", mb / 1000.0);
        }
        return Tool.formatLong(mb / 1000);
    }

    private class StateButton extends SimpleButton {
        private boolean state;

        StateButton(int x, int y, int width, int height, boolean initial, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress);
            this.state = initial;
        }

        void setState(boolean state) {
            this.state = state;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButton(guiGraphics, this.state ? 0xFF00AA00 : 0xFFAA0000);
        }
    }

    private abstract class SimpleButton extends Button {
        SimpleButton(int x, int y, int width, int height, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        }

        protected void renderButton(GuiGraphics guiGraphics, int color) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            int borderColor = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor);
            guiGraphics.drawCenteredString(LiquidGeneratorScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }
}
