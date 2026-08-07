package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.menu.BlockGeneratorMenu;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

/**
 * 方块生成器 GUI（26.x 适配）。
 * <p>
 * 展示当前存量、产量、下次增长量、增长百分比（含进度条），并提供六个传输面开关、
 * 标记槽（放入合法物品后锁定）、输出展示槽（单击提取一个、Shift+单击提取一组、空格+单击提取到背包满）
 * 以及"下方生成方块"开关。
 */
public class BlockGeneratorScreen extends AbstractContainerScreen<BlockGeneratorMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("autoresource", "textures/gui/block_generator_gui.png");
    private static final int TEXT_COLOR = 0xFF404040; // 0x404040 深灰（26.x 需带 alpha，否则透明）
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
        super(menu, playerInventory, title, 176, 233);
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
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_SPACE) {
            this.spaceDown = true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_SPACE) {
            this.spaceDown = false;
        }
        return super.keyReleased(event);
    }

    /**
     * 拦截输出展示槽的点击：单击提取一个、Shift+单击提取一组、空格+单击提取到背包满
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean p) {
        if (event.button() == 0) {
            Slot outputSlot = this.menu.slots.get(OUTPUT_SLOT_INDEX);
            if (this.isHovering(outputSlot.x, outputSlot.y, 16, 16, event.x(), event.y())) {
                int id;
                if ((event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0) {
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
        return super.mouseClicked(event, p);
    }

    /**
     * 发送容器按钮点击到服务端
     */
    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.connection.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, id));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        // 增长进度条
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
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        BlockGeneratorMenu menu = this.menu;
        boolean maxed = menu.getOutput() >= menu.getMax();
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.block_generator.block", formatBlocks(menu.getBlock())), 12, 19, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.block_generator.output", formatBlocks(menu.getOutput())), 12, 29, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.block_generator.next", maxed ? Component.translatable("screen.autoresource.block_generator.next_max") : Component.literal(formatBlocks(menu.getStep()))), 12, 37, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.block_generator.growth", growthPercent()), 12, 47, TEXT_COLOR, false);
        Component markerLabel = Component.translatable("screen.autoresource.block_generator.marker");
        guiGraphics.text(this.font, markerLabel, 28, 116, TEXT_COLOR, false);
        Component outputLabel = Component.translatable("screen.autoresource.block_generator.output_slot");
        guiGraphics.text(this.font, outputLabel, 150 - this.font.width(outputLabel), 116, TEXT_COLOR, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        // 刷新各开关状态
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
        protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButton(guiGraphics, this.state ? 0xFF00AA00 : 0xFFAA0000);
        }
    }

    private abstract class SimpleButton extends Button {
        SimpleButton(int x, int y, int width, int height, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        }

        protected void renderButton(GuiGraphicsExtractor guiGraphics, int color) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            int borderColor = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor);
            guiGraphics.centeredText(BlockGeneratorScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }
}
