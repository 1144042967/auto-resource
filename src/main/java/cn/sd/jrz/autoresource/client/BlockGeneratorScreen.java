package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.menu.BlockGeneratorMenu;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

/**
 * 方块生成器 GUI（26.x 适配）。
 * <p>
 * 展示当前存量、产量、下次增长量、增长百分比（含进度条），并提供主动输出总开关、六个传输面开关、
 * 标记槽（放入合法物品后锁定）、输出展示槽（单击提取一个、Shift+单击提取一组、空格+单击提取到背包满）
 * 以及"下方生成方块"开关。
 */
public class BlockGeneratorScreen extends AbstractGeneratorScreen<BlockGeneratorMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("autoresource", "textures/gui/block_generator_gui.png");
    private static final int OUTPUT_SLOT_INDEX = 1;

    private StateButton outputButton;
    private FaceButton faceDown;
    private FaceButton faceUp;
    private FaceButton faceNorth;
    private FaceButton faceSouth;
    private FaceButton faceWest;
    private FaceButton faceEast;
    private StateButton placeButton;
    private boolean spaceDown = false;

    public BlockGeneratorScreen(BlockGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 233);
        this.inventoryLabelY = 137;
    }

    @Override
    protected void init() {
        super.init();
        // 主动输出总开关（GUI 右上角）
        this.outputButton = new StateButton(this.leftPos + 120, this.topPos + 20, 44, 12, this.menu.isOutputEnabled(), Component.translatable("screen.autoresource.block_generator.output_toggle"), _ -> sendButton(BlockGeneratorMenu.BUTTON_OUTPUT));
        this.addRenderableWidget(this.outputButton);
        this.faceDown = new FaceButton(this.leftPos + 17, this.topPos + 76, 44, 12, Direction.DOWN, this.menu.isFaceEnabled(Direction.DOWN), Component.translatable("screen.autoresource.energy_generator.face.down"), _ -> sendButton(BlockGeneratorMenu.BUTTON_TRANSFER_DOWN));
        this.faceUp = new FaceButton(this.leftPos + 66, this.topPos + 76, 44, 12, Direction.UP, this.menu.isFaceEnabled(Direction.UP), Component.translatable("screen.autoresource.energy_generator.face.up"), _ -> sendButton(BlockGeneratorMenu.BUTTON_TRANSFER_UP));
        this.faceNorth = new FaceButton(this.leftPos + 115, this.topPos + 76, 44, 12, Direction.NORTH, this.menu.isFaceEnabled(Direction.NORTH), Component.translatable("screen.autoresource.energy_generator.face.north"), _ -> sendButton(BlockGeneratorMenu.BUTTON_TRANSFER_NORTH));
        this.faceSouth = new FaceButton(this.leftPos + 17, this.topPos + 92, 44, 12, Direction.SOUTH, this.menu.isFaceEnabled(Direction.SOUTH), Component.translatable("screen.autoresource.energy_generator.face.south"), _ -> sendButton(BlockGeneratorMenu.BUTTON_TRANSFER_SOUTH));
        this.faceWest = new FaceButton(this.leftPos + 66, this.topPos + 92, 44, 12, Direction.WEST, this.menu.isFaceEnabled(Direction.WEST), Component.translatable("screen.autoresource.energy_generator.face.west"), _ -> sendButton(BlockGeneratorMenu.BUTTON_TRANSFER_WEST));
        this.faceEast = new FaceButton(this.leftPos + 115, this.topPos + 92, 44, 12, Direction.EAST, this.menu.isFaceEnabled(Direction.EAST), Component.translatable("screen.autoresource.energy_generator.face.east"), _ -> sendButton(BlockGeneratorMenu.BUTTON_TRANSFER_EAST));
        this.addRenderableWidget(this.faceDown);
        this.addRenderableWidget(this.faceUp);
        this.addRenderableWidget(this.faceNorth);
        this.addRenderableWidget(this.faceSouth);
        this.addRenderableWidget(this.faceWest);
        this.addRenderableWidget(this.faceEast);
        this.placeButton = new StateButton(this.leftPos + 72, this.topPos + 133, 96, 12, this.menu.isPlaceBlockBelow(), Component.translatable("screen.autoresource.block_generator.place_below"), _ -> sendButton(BlockGeneratorMenu.BUTTON_PLACE_BLOCK));
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

    @Override
    public void extractBackground(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        // 增长进度条（绿色）
        drawGrowthBar(guiGraphics, this.topPos + 60, 0xFF00AA00);
    }

    @Override
    protected void extractLabels(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
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
    public void extractRenderState(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        // 刷新各开关状态
        this.outputButton.setState(this.menu.isOutputEnabled());
        this.faceDown.setState(this.menu.isFaceEnabled(Direction.DOWN));
        this.faceUp.setState(this.menu.isFaceEnabled(Direction.UP));
        this.faceNorth.setState(this.menu.isFaceEnabled(Direction.NORTH));
        this.faceSouth.setState(this.menu.isFaceEnabled(Direction.SOUTH));
        this.faceWest.setState(this.menu.isFaceEnabled(Direction.WEST));
        this.faceEast.setState(this.menu.isFaceEnabled(Direction.EAST));
        this.placeButton.setState(this.menu.isPlaceBlockBelow());
    }

    private static String formatBlocks(long scaled) {
        if (scaled < 10_000) {
            return String.format("%.2f", scaled / 1000.0);
        }
        return Tool.formatLong(scaled / 1000);
    }
}
