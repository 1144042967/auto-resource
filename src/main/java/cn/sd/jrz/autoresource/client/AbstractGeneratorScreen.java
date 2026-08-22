package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.menu.AbstractGeneratorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;

/**
 * 机器 GUI 基类：共享按钮点击发送、增长百分比计算、进度条轨道绘制与开关/通用小按钮；
 * 子类负责槽位布局、进度条配色、数据展示与开关初始化。
 * 注意：26.x 不再支持 {@code @OnlyIn} 的运行时成员剥离，故不在此类使用该注解。
 */
public abstract class AbstractGeneratorScreen<M extends AbstractGeneratorMenu<?>> extends AbstractContainerScreen<M> {
    protected static final int TEXT_COLOR = 0xFF404040; // 0x404040 深灰（26.x 需带 alpha，否则透明）

    protected AbstractGeneratorScreen(M menu, Inventory playerInventory, Component title, int imageWidth, int imageHeight) {
        super(menu, playerInventory, title, imageWidth, imageHeight);
    }

    /**
     * 发送容器按钮点击到服务端
     */
    protected void sendButton(int id) {
        if (this.minecraft.player != null) {
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
        return (int) Math.clamp(percent, 0, 100);
    }

    /**
     * 绘制增长进度条轨道与填充（子类在 extractBackground 中调用；trackTop 为轨道 y 坐标）
     */
    protected void drawGrowthBar(GuiGraphicsExtractor guiGraphics, int trackTop, int fillColor) {
        int trackLeft = this.leftPos + 12;
        int trackRight = this.leftPos + 164;
        guiGraphics.fill(trackLeft, trackTop, trackRight, trackTop + 4, 0xFF555555);
        int percent = growthPercent();
        if (percent > 0) {
            int fill = (trackRight - trackLeft) * percent / 100;
            guiGraphics.fill(trackLeft, trackTop, trackLeft + fill, trackTop + 4, fillColor);
        }
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
        protected void extractContents(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
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
        protected void extractContents(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
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

        protected void renderButton(GuiGraphicsExtractor guiGraphics, int color) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            // 1px 边框（鼠标悬浮时边框变亮，用于指示可交互）
            int borderColor = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor);
            guiGraphics.centeredText(AbstractGeneratorScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }
}
