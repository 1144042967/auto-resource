package cn.sd.jrz.autoresource.client.compat.create;

import cn.sd.jrz.autoresource.compat.create.WaterWheelMotorMenu;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

/**
 * 水车马达 GUI。布局：上部大框放六面输出方向按钮；中部左侧水车槽位、右侧旋转方向开关；
 * 下部物品栏名称 + 当前转速（3 位补零）。
 */
public class WaterWheelMotorScreen extends AbstractContainerScreen<WaterWheelMotorMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("autoresource", "textures/gui/water_wheel_motor_gui.png");
    private static final int TEXT_COLOR = 4210752; // 0x404040 深灰
    private static final int FACE_ICON_SIZE = 12;

    private StateButton directionButton;
    private FaceButton faceDown;
    private FaceButton faceUp;
    private FaceButton faceNorth;
    private FaceButton faceSouth;
    private FaceButton faceWest;
    private FaceButton faceEast;

    public WaterWheelMotorScreen(WaterWheelMotorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 177;
        this.inventoryLabelY = 80;
    }

    @Override
    protected void init() {
        super.init();
        // 旋转方向开关（水车槽同一行右侧，无标签）
        this.directionButton = new StateButton(this.leftPos + 112, this.topPos + 58, 56, 12, this.menu.isCounterClockwise(), Component.empty(), button -> sendButton(WaterWheelMotorMenu.BUTTON_DIRECTION));
        this.addRenderableWidget(this.directionButton);
        // 六面输出方向：位于上部大框正中（两行三列，框 y=16~51）
        this.faceDown = new FaceButton(this.leftPos + 14, this.topPos + 20, 44, 12, Direction.DOWN, this.menu.getFace() == Direction.DOWN, faceLabel(Direction.DOWN), button -> sendButton(WaterWheelMotorMenu.BUTTON_FACE_DOWN));
        this.faceUp = new FaceButton(this.leftPos + 66, this.topPos + 20, 44, 12, Direction.UP, this.menu.getFace() == Direction.UP, faceLabel(Direction.UP), button -> sendButton(WaterWheelMotorMenu.BUTTON_FACE_UP));
        this.faceNorth = new FaceButton(this.leftPos + 118, this.topPos + 20, 44, 12, Direction.NORTH, this.menu.getFace() == Direction.NORTH, faceLabel(Direction.NORTH), button -> sendButton(WaterWheelMotorMenu.BUTTON_FACE_NORTH));
        this.faceSouth = new FaceButton(this.leftPos + 14, this.topPos + 36, 44, 12, Direction.SOUTH, this.menu.getFace() == Direction.SOUTH, faceLabel(Direction.SOUTH), button -> sendButton(WaterWheelMotorMenu.BUTTON_FACE_SOUTH));
        this.faceWest = new FaceButton(this.leftPos + 66, this.topPos + 36, 44, 12, Direction.WEST, this.menu.getFace() == Direction.WEST, faceLabel(Direction.WEST), button -> sendButton(WaterWheelMotorMenu.BUTTON_FACE_WEST));
        this.faceEast = new FaceButton(this.leftPos + 118, this.topPos + 36, 44, 12, Direction.EAST, this.menu.getFace() == Direction.EAST, faceLabel(Direction.EAST), button -> sendButton(WaterWheelMotorMenu.BUTTON_FACE_EAST));
        this.addRenderableWidget(this.faceDown);
        this.addRenderableWidget(this.faceUp);
        this.addRenderableWidget(this.faceNorth);
        this.addRenderableWidget(this.faceSouth);
        this.addRenderableWidget(this.faceWest);
        this.addRenderableWidget(this.faceEast);
    }

    /**
     * 面名标签
     */
    private static Component faceLabel(Direction face) {
        return Component.translatable("screen.autoresource.water_wheel_motor.face." + face.getName());
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
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderBackground(guiGraphics);
        // 面板与槽位背景已烘焙进纹理（water_wheel_motor_gui.png），可用 PS 直接修改
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        WaterWheelMotorMenu menu = this.menu;
        // 水车槽位提示（槽位右侧）
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.water_wheel_motor.wheel_slot"), 28, 61, TEXT_COLOR, false);
        // 物品栏行右侧显示当前转速（3 位补零，如 004 RPM；右对齐）
        String speedText = Component.translatable("screen.autoresource.water_wheel_motor.speed", String.format("%03d", menu.getSpeed())).getString();
        guiGraphics.drawString(this.font, speedText, 168 - this.font.width(speedText), 80, TEXT_COLOR, false);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        refreshButtonStates();
    }

    /**
     * 按 FACE_ICON_SIZE 缩放绘制 FaceButton 内的物品图标
     */
    private void renderFaceIcon(GuiGraphics guiGraphics, int x, int y, int buttonWidth, int buttonHeight, ItemStack stack) {
        float scale = FACE_ICON_SIZE / 16.0F;
        int iconX = x + (buttonWidth - FACE_ICON_SIZE) / 2;
        int iconY = y + (buttonHeight - FACE_ICON_SIZE) / 2;
        // 用 Matrix3x2fStack 缩放绘制 FaceButton 内的物品图标
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        poseStack.translate(iconX, iconY);
        poseStack.scale(scale, scale);
        guiGraphics.renderItem(stack, 0, 0);
        poseStack.popMatrix();
    }

    /**
     * 刷新各开关显示状态
     */
    private void refreshButtonStates() {
        boolean counterClockwise = this.menu.isCounterClockwise();
        this.directionButton.setState(counterClockwise);
        this.directionButton.setMessage(Component.translatable(counterClockwise
                ? "screen.autoresource.water_wheel_motor.ccw"
                : "screen.autoresource.water_wheel_motor.cw"));
        this.faceDown.setState(this.menu.getFace() == Direction.DOWN);
        this.faceUp.setState(this.menu.getFace() == Direction.UP);
        this.faceNorth.setState(this.menu.getFace() == Direction.NORTH);
        this.faceSouth.setState(this.menu.getFace() == Direction.SOUTH);
        this.faceWest.setState(this.menu.getFace() == Direction.WEST);
        this.faceEast.setState(this.menu.getFace() == Direction.EAST);
    }

    /**
     * 带状态颜色的开关按钮（选中=绿色，未选=红色）
     */
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
        protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButton(guiGraphics, this.state ? 0xFF00AA00 : 0xFFAA0000);
        }
    }

    /**
     * 带边框与居中文字的通用按钮
     */
    private abstract class SimpleButton extends Button {
        SimpleButton(int x, int y, int width, int height, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        }

        protected void renderButton(GuiGraphics guiGraphics, int color) {
            renderButtonBg(guiGraphics, color);
            guiGraphics.drawCenteredString(WaterWheelMotorScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }

        /**
         * 绘制按钮背景（填充 + 1px 边框；悬浮时边框变亮）。FaceButton 重用此方法绘制背景，再根据是否有相邻方块决定绘制物品图标或方向名。
         */
        protected void renderButtonBg(GuiGraphics guiGraphics, int color) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            int borderColor = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor);
        }
    }

    /**
     * 六方向按钮：该方向有相邻方块时只显示物品图标（居中）；无相邻方块时显示方向名（居中）。绿=生效/红=禁用
     */
    private class FaceButton extends SimpleButton {
        private final Direction direction;
        private boolean state;

        FaceButton(int x, int y, int width, int height, Direction direction, boolean initial, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress);
            this.direction = direction;
            this.state = initial;
        }

        void setState(boolean state) {
            this.state = state;
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButtonBg(guiGraphics, this.state ? 0xFF00AA00 : 0xFFAA0000);
            ItemStack neighbor = WaterWheelMotorScreen.this.menu.getNeighborStack(this.direction);
            if (!neighbor.isEmpty()) {
                renderFaceIcon(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), neighbor);
            } else {
                guiGraphics.drawCenteredString(WaterWheelMotorScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
            }
        }
    }
}
