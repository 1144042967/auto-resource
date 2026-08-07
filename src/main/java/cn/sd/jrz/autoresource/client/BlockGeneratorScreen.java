package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.menu.BlockGeneratorMenu;
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
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * 方块生成器 GUI。
 */
@OnlyIn(Dist.CLIENT)
public class BlockGeneratorScreen extends AbstractContainerScreen<BlockGeneratorMenu> {
    private static final ResourceLocation TEXTURE = Identifier.fromNamespaceAndPath("autoresource", "textures/gui/block_generator_gui.png");
    private static final int TEXT_COLOR = 4210752; // 0x404040 深灰
    private static final int OUTPUT_SLOT_INDEX = 1;

    private StateButton faceDown;
    private StateButton faceUp;
    private StateButton faceNorth;
    private StateButton faceSouth;
    private StateButton faceWest;
    private StateButton faceEast;
    private StateButton placeButton;
    private boolean spaceDown = false;

    public BlockGeneratorScreen(BlockGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 233;
        this.inventoryLabelY = 137;
    }

    @Override
    protected void init() {
        super.init();
        this.faceDown = new StateButton(this.leftPos + 17, this.topPos + 76, 44, 12, this.menu.isFaceEnabled(Direction.DOWN), Component.translatable("screen.autoresource.energy_generator.face.down"), button -> sendButton(BlockGeneratorMenu.BUTTON_TRANSFER_DOWN));
        this.faceUp = new StateButton(this.leftPos + 66, this.topPos + 76, 44, 12, this.menu.isFaceEnabled(Direction.UP), Component.translatable("screen.autoresource.energy_generator.face.up"), button -> sendButton(BlockGeneratorMenu.BUTTON_TRANSFER_UP));
        this.faceNorth = new StateButton(this.leftPos + 115, this.topPos + 76, 44, 12, this.menu.isFaceEnabled(Direction.NORTH), Component.translatable("screen.autoresource.energy_generator.face.north"), button -> sendButton(BlockGeneratorMenu.BUTTON_TRANSFER_NORTH));
        this.faceSouth = new StateButton(this.leftPos + 17, this.topPos + 92, 44, 12, this.menu.isFaceEnabled(Direction.SOUTH), Component.translatable("screen.autoresource.energy_generator.face.south"), button -> sendButton(BlockGeneratorMenu.BUTTON_TRANSFER_SOUTH));
        this.faceWest = new StateButton(this.leftPos + 66, this.topPos + 92, 44, 12, this.menu.isFaceEnabled(Direction.WEST), Component.translatable("screen.autoresource.energy_generator.face.west"), button -> sendButton(BlockGeneratorMenu.BUTTON_TRANSFER_WEST));
        this.faceEast = new StateButton(this.leftPos + 115, this.topPos + 92, 44, 12, this.menu.isFaceEnabled(Direction.EAST), Component.translatable("screen.autoresource.energy_generator.face.east"), button -> sendButton(BlockGeneratorMenu.BUTTON_TRANSFER_EAST));
        this.addRenderableWidget(this.faceDown);
        this.addRenderableWidget(this.faceUp);
        this.addRenderableWidget(this.faceNorth);
        this.addRenderableWidget(this.faceSouth);
        this.addRenderableWidget(this.faceWest);
        this.addRenderableWidget(this.faceEast);
        this.placeButton = new StateButton(this.leftPos + 72, this.topPos + 133, 96, 12, this.menu.isPlaceBlockBelow(), Component.translatable("screen.autoresource.block_generator.place_below"), button -> sendButton(BlockGeneratorMenu.BUTTON_PLACE_BLOCK));
        this.addRenderableWidget(this.placeButton);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            this.spaceDown = true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            this.spaceDown = false;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Slot outputSlot = this.menu.slots.get(OUTPUT_SLOT_INDEX);
            if (this.isHovering(outputSlot.x, outputSlot.y, 16, 16, mouseX, mouseY)) {
                int id;
                if (hasShiftDown()) {
                    id = BlockGeneratorMenu.BUTTON_EXTRACT_STACK;
                } else if (this.spaceDown) {
                    id = BlockGeneratorMenu.BUTTON_EXTRACT_ALL;
                } else {
                    id = BlockGeneratorMenu.BUTTON_EXTRACT_ONE;
                }
                sendButton(id);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
            guiGraphics.fill(trackLeft, trackTop, trackLeft + fill, trackTop + 4, 0xFF00AA00);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        BlockGeneratorMenu menu = this.menu;
        boolean maxed = menu.getOutput() >= menu.getMax();
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.block_generator.block", formatBlocks(menu.getBlock())), 12, 19, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.block_generator.output", formatBlocks(menu.getOutput())), 12, 29, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.block_generator.next", maxed ? Component.translatable("screen.autoresource.block_generator.next_max") : Component.literal(formatBlocks(menu.getStep()))), 12, 37, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.block_generator.growth", growthPercent()), 12, 47, TEXT_COLOR, false);
        Component markerLabel = Component.translatable("screen.autoresource.block_generator.marker");
        guiGraphics.drawString(this.font, markerLabel, 28, 116, TEXT_COLOR, false);
        Component outputLabel = Component.translatable("screen.autoresource.block_generator.output_slot");
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
        this.placeButton.setState(this.menu.isPlaceBlockBelow());
    }

    private int growthPercent() {
        if (this.menu.getOutput() >= this.menu.getMax()) {
            return 100;
        }
        int second = Math.max(1, this.menu.getSecond());
        double percent = this.menu.getTickCount() / (second * 20.0) * 100.0;
        return (int) Math.max(0, Math.min(100, percent));
    }

    private static String formatBlocks(long scaled) {
        if (scaled < 10_000) {
            return String.format("%.2f", scaled / 1000.0);
        }
        return Tool.formatLong(scaled / 1000);
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
            guiGraphics.drawCenteredString(BlockGeneratorScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }
}
