package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.menu.BlockGeneratorMenu;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * 方块生成器 GUI。
 * <p>
 * 展示当前存量、产量、下次增长量、增长百分比（含进度条），并提供六个传输面开关、
 * 标记槽（放入合法物品后锁定）、输出展示槽（单击提取一个、Shift+单击提取一组、空格+单击提取到背包满）
 * 以及"下方生成方块"开关。数值使用单位缩写（K/M/G/T/P/E）避免 long 大数溢出。
 */
@OnlyIn(Dist.CLIENT)
public class BlockGeneratorScreen extends AbstractGeneratorScreen<BlockGeneratorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("autoresource", "textures/gui/block_generator_gui.png");
    /** 输出展示槽在菜单中的槽位索引 */
    private static final int OUTPUT_SLOT_INDEX = 1;

    private StateButton faceDown;
    private StateButton faceUp;
    private StateButton faceNorth;
    private StateButton faceSouth;
    private StateButton faceWest;
    private StateButton faceEast;
    private StateButton placeButton;
    private StateButton outputButton;
    /** 空格键是否按下（空格+单击输出槽 = 提取到背包满） */
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
        // 六个传输面（宽 44 以容纳英文面名）
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
        // "下方生成方块"按钮（位于输出槽下方，开启后向机器下方空气方块放置标记的方块）
        this.placeButton = new StateButton(this.leftPos + 72, this.topPos + 133, 96, 12, this.menu.isPlaceBlockBelow(), Component.translatable("screen.autoresource.block_generator.place_below"), button -> sendButton(BlockGeneratorMenu.BUTTON_PLACE_BLOCK));
        this.addRenderableWidget(this.placeButton);
        // "输出"按钮（GUI 右上角，控制主动输出总开关，默认开启）
        this.outputButton = new StateButton(this.leftPos + 120, this.topPos + 20, 44, 12, this.menu.isOutputEnabled(), Component.translatable("screen.autoresource.block_generator.output_toggle"), button -> sendButton(BlockGeneratorMenu.BUTTON_OUTPUT));
        this.addRenderableWidget(this.outputButton);
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

    /**
     * 拦截输出展示槽的点击：单击提取一个、Shift+单击提取一组、空格+单击提取到背包满
     */
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

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
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
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        BlockGeneratorMenu menu = this.menu;
        boolean maxed = menu.getOutput() >= menu.getMax();
        // 信息面板（存量/产量/下次增长均以 个 为单位，大数值用单位缩写）
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.block_generator.block", formatBlocks(menu.getBlock())), 12, 19, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.block_generator.output", formatBlocks(menu.getOutput())), 12, 29, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.block_generator.next", maxed ? Component.translatable("screen.autoresource.block_generator.next_max") : Component.literal(formatBlocks(menu.getStep()))), 12, 37, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.block_generator.growth", growthPercent()), 12, 47, TEXT_COLOR, false);
        // 标记槽标签（贴近标记槽右侧）
        Component markerLabel = Component.translatable("screen.autoresource.block_generator.marker");
        guiGraphics.drawString(this.font, markerLabel, 28, 116, TEXT_COLOR, false);
        // 输出槽标签：右对齐贴近输出槽
        Component outputLabel = Component.translatable("screen.autoresource.block_generator.output_slot");
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
        this.placeButton.setState(this.menu.isPlaceBlockBelow());
        this.outputButton.setState(this.menu.isOutputEnabled());
    }

    /**
     * 以 个 为单位展示方块数量：小数值保留两位小数，大数值使用单位缩写
     */
    private static String formatBlocks(long scaled) {
        if (scaled < 10_000) {
            return String.format("%.2f", scaled / 1000.0);
        }
        return Tool.formatLong(scaled / 1000);
    }
}
