package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.menu.AbstractGeneratorMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * 机器 GUI 基类：共享按钮点击发送、增长百分比计算、渲染循环与开关/通用小按钮；子类负责槽位布局、进度条配色与开关初始化。
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractGeneratorScreen<M extends AbstractGeneratorMenu<?>> extends AbstractContainerScreen<M> {
    protected static final int TEXT_COLOR = 4210752; // 0x404040 深灰
    /**
     * 方向按钮内物品图标的目标像素尺寸（按钮高 12 px，图标填满整按钮高度，无边距）
     */
    protected static final int FACE_ICON_SIZE = 12;

    protected AbstractGeneratorScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * 在按钮内居中绘制缩放后的物品图标（用于方向按钮显示相邻方块）
     * @param x 按钮左边界
     * @param y 按钮上边界
     * @param buttonWidth 按钮宽度
     * @param buttonHeight 按钮高度
     * @param stack 要渲染的物品栈
     */
    protected void renderFaceIcon(GuiGraphics guiGraphics, int x, int y, int buttonWidth, int buttonHeight, ItemStack stack) {
        // 物品默认渲染为 16×16 px；按目标尺寸等比缩放
        float scale = FACE_ICON_SIZE / 16.0F;
        // 居中绘制（缩放后尺寸 = FACE_ICON_SIZE 像素）
        int iconX = x + (buttonWidth - FACE_ICON_SIZE) / 2;
        int iconY = y + (buttonHeight - FACE_ICON_SIZE) / 2;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        // translate 到图标左上角，Z 抬高到按钮背景之上
        poseStack.translate(iconX, iconY, 100.0F);
        poseStack.scale(scale, scale, scale);
        guiGraphics.renderItem(stack, 0, 0);
        poseStack.popPose();
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
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 渲染鼠标悬浮物品信息提示窗（render 不会自动调用）
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        // 刷新各开关状态
        refreshButtonStates();
    }

    /**
     * 子类刷新各开关的显示状态
     */
    protected abstract void refreshButtonStates();

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
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButton(guiGraphics, this.state ? 0xFF00AA00 : 0xFFAA0000);
        }
    }

    /**
     * 灰色小按钮（+/-）
     */
    protected class MiniButton extends SimpleButton {
        MiniButton(int x, int y, int width, int height, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress);
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButton(guiGraphics, 0xFF808080);
        }
    }

    /**
     * 带边框与居中文字的通用按钮
     */
    protected abstract class SimpleButton extends Button {
        SimpleButton(int x, int y, int width, int height, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        }

        protected void renderButtonBg(GuiGraphics guiGraphics, int color) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            // 1px 边框（鼠标悬浮时边框变亮，用于指示可交互）
            int borderColor = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor);
        }

        protected void renderButton(GuiGraphics guiGraphics, int color) {
            renderButtonBg(guiGraphics, color);
            guiGraphics.drawCenteredString(AbstractGeneratorScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
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
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButtonBg(guiGraphics, this.state ? 0xFF00AA00 : 0xFFAA0000);
            ItemStack neighbor = AbstractGeneratorScreen.this.menu.getNeighborStack(this.direction);
            if (!neighbor.isEmpty()) {
                // 有相邻方块：只显示物品图标，按 FACE_ICON_SIZE 缩放后居中（按钮高 12 px，图标填满整按钮高度）
                renderFaceIcon(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), neighbor);
            } else {
                guiGraphics.drawCenteredString(AbstractGeneratorScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
            }
        }
    }
}
