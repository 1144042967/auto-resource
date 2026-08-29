package cn.sd.jrz.autoresource.compat.create;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * 水车马达 GUI。布局：上部大框放六面输出方向按钮；中部左侧水车槽位、右侧旋转方向开关；
 * 下部物品栏名称 + 当前转速（3 位补零）。
 */
@OnlyIn(Dist.CLIENT)
public class WaterWheelMotorScreen extends AbstractContainerScreen<WaterWheelMotorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("autoresource", "textures/gui/water_wheel_motor_gui.png");
    private static final int TEXT_COLOR = 4210752; // 0x404040 深灰
    /**
     * 方向按钮内物品图标的目标像素尺寸（按钮高 12 px，图标填满整按钮高度，无边距）
     */
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

    /**
     * 在按钮内居中绘制缩放后的物品图标（用于方向按钮显示相邻方块）
     */
    private void renderFaceIcon(GuiGraphics guiGraphics, int x, int y, int buttonWidth, int buttonHeight, ItemStack stack) {
        float scale = FACE_ICON_SIZE / 16.0F;
        int iconX = x + (buttonWidth - FACE_ICON_SIZE) / 2;
        int iconY = y + (buttonHeight - FACE_ICON_SIZE) / 2;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(iconX, iconY, 100.0F);
        poseStack.scale(scale, scale, scale);
        guiGraphics.renderItem(stack, 0, 0);
        poseStack.popPose();
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
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 面板与槽位背景已烘焙进纹理（water_wheel_motor_gui.png），可用 PS 直接修改
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        WaterWheelMotorMenu menu = this.menu;
        // 水车槽位提示（槽位右侧）
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.water_wheel_motor.wheel_slot"), 28, 61, TEXT_COLOR, false);
        // 物品栏行右侧显示当前转速（3 位补零，如 004 RPM；右对齐）
        String speedText = Component.translatable("screen.autoresource.water_wheel_motor.speed", String.format("%03d", menu.getSpeed())).getString();
        guiGraphics.drawString(this.font, speedText, 168 - this.font.width(speedText), 80, TEXT_COLOR, false);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        refreshButtonStates();
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
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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

        protected void renderButtonBg(GuiGraphics guiGraphics, int color) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            // 1px 边框（鼠标悬浮时边框变亮）
            int borderColor = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor);
        }

        protected void renderButton(GuiGraphics guiGraphics, int color) {
            renderButtonBg(guiGraphics, color);
            guiGraphics.drawCenteredString(WaterWheelMotorScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
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
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButtonBg(guiGraphics, this.state ? 0xFF00AA00 : 0xFFAA0000);
            ItemStack neighbor = WaterWheelMotorScreen.this.menu.getNeighborStack(this.direction);
            if (!neighbor.isEmpty()) {
                // 有相邻方块：只显示物品图标，按 FACE_ICON_SIZE 缩放后居中（按钮高 12 px，图标填满整按钮高度）
                WaterWheelMotorScreen.this.renderFaceIcon(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), neighbor);
            } else {
                guiGraphics.drawCenteredString(WaterWheelMotorScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
            }
        }
    }
}
