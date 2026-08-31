package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.menu.AbstractGeneratorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

/**
 * 机器 GUI 基类：共享按钮点击发送、增长百分比计算、渲染循环与开关/通用小按钮；子类负责槽位布局、进度条配色与开关初始化。
 */
public abstract class AbstractGeneratorScreen<M extends AbstractGeneratorMenu<?>> extends AbstractContainerScreen<M> {
    protected static final int TEXT_COLOR = 4210752; // 0x404040 深灰
    protected static final int FACE_ICON_SIZE = 12;

    protected AbstractGeneratorScreen(M menu, Inventory playerInventory, Component title, int imageWidth, int imageHeight) {
        super(menu, playerInventory, title, imageWidth, imageHeight);
    }

    protected AbstractGeneratorScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * 发送容器按钮点击到服务端
     */
    protected void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.connection.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, id));
        }
    }

    /**
     * 计算增长百分比（0-100）；达最大产量时固定为 100%
     */
    protected int growthPercent() {
        if (this.menu.getOutput() >= this.menu.getMax()) {
            return 100;
        }
        int second = Math.max(1, this.menu.getSecond());
        double percent = this.menu.getTickCount() / (second * 20.0) * 100.0;
        return (int) Math.max(0, Math.min(100, percent));
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        // 刷新各开关状态
        refreshButtonStates();
    }

    /**
     * 子类刷新各开关的显示状态
     */
    protected abstract void refreshButtonStates();

    /**
     * 按 FACE_ICON_SIZE 缩放绘制 FaceButton 内的物品图标（按钮高度 12，上下至少 2px 边距）
     */
    protected void renderFaceIcon(GuiGraphicsExtractor guiGraphics, int x, int y, int buttonWidth, int buttonHeight, ItemStack stack) {
        float scale = FACE_ICON_SIZE / 16.0F;
        int iconX = x + (buttonWidth - FACE_ICON_SIZE) / 2;
        int iconY = y + (buttonHeight - FACE_ICON_SIZE) / 2;
        // 用 Matrix3x2fStack 缩放绘制 FaceButton 内的物品图标
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        poseStack.translate(iconX, iconY);
        poseStack.scale(scale, scale);
        guiGraphics.item(stack, 0, 0);
        poseStack.popMatrix();
    }

    /**
     * 简化的按钮回调（与 1.21.5 Button.OnPress 同形）
     */
    @FunctionalInterface
    public interface OnPress {
        void onPress(AbstractButton button);
    }

    /**
     * 带状态颜色的开关按钮（开=绿色，关=红色）
     */
    protected class StateButton extends SimpleButton {
        private boolean state;

        StateButton(int x, int y, int width, int height, boolean initial, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress);
            this.state = initial;
        }

        void setState(boolean state) {
            this.state = state;
        }

        @Override
        protected void extractContents(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButton(guiGraphics, this.state ? 0xFF00AA00 : 0xFFAA0000);
        }
    }

    /**
     * 自定义按钮：可定制的渲染、点击与朗读。
     */
    public abstract class SimpleButton extends AbstractButton {
        private final OnPress onPress;

        protected SimpleButton(int x, int y, int width, int height, Component label, OnPress onPress) {
            super(x, y, width, height, label);
            this.onPress = onPress;
        }

        @Override
        public void onPress(net.minecraft.client.input.InputWithModifiers modifiers) {
            this.onPress.onPress(this);
        }

        @Override
        protected void extractContents(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButton(guiGraphics, 0xFF808080);
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        protected void renderButton(GuiGraphicsExtractor guiGraphics, int color) {
            renderButtonBg(guiGraphics, this, color);
            guiGraphics.centeredText(AbstractGeneratorScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }

    /**
     * 渲染按钮底色与 1px 边框（鼠标悬浮时边框变亮，用于指示可交互）。
     * 26.1.2：基类 AbstractGeneratorScreen 不是 AbstractWidget 子类，无法在自身引用按钮位置；
     * 这里把按钮引用作为参数传入，按钮自身继承 AbstractButton/AbstractWidget，自带 getX/getY/getWidth/getHeight/isHovered。
     */
    protected void renderButtonBg(GuiGraphicsExtractor guiGraphics, AbstractButton button, int color) {
        guiGraphics.fill(button.getX(), button.getY(), button.getX() + button.getWidth(), button.getY() + button.getHeight(), color);
        int borderColor = button.isHovered() ? 0xFFFFFF00 : 0xFF000000;
        guiGraphics.fill(button.getX() - 1, button.getY() - 1, button.getX() + button.getWidth() + 1, button.getY(), borderColor);
        guiGraphics.fill(button.getX() - 1, button.getY() + button.getHeight(), button.getX() + button.getWidth() + 1, button.getY() + button.getHeight() + 1, borderColor);
        guiGraphics.fill(button.getX() - 1, button.getY(), button.getX(), button.getY() + button.getHeight(), borderColor);
        guiGraphics.fill(button.getX() + button.getWidth(), button.getY(), button.getX() + button.getWidth() + 1, button.getY() + button.getHeight(), borderColor);
    }

    /**
     * 六方向按钮：该方向有相邻方块时只显示物品图标（居中）；无相邻方块时显示方向名（居中）。绿=生效/红=禁用
     */
    protected class FaceButton extends SimpleButton {
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
        protected void extractContents(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButtonBg(guiGraphics, this, this.state ? 0xFF00AA00 : 0xFFAA0000);
            ItemStack neighbor = AbstractGeneratorScreen.this.menu.getNeighborStack(this.direction);
            if (!neighbor.isEmpty()) {
                renderFaceIcon(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), neighbor);
            } else {
                guiGraphics.centeredText(AbstractGeneratorScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
            }
        }
    }
}