package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.menu.AbstractGeneratorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 机器 GUI 基类：共享按钮点击发送、增长百分比计算、渲染循环与开关/通用小按钮；子类负责槽位布局、进度条配色与开关初始化。
 * <p>
 * 1.19.2 没有 {@code GuiGraphics}，所有绘制都以 {@link PoseStack} 为第一参数：
 * {@code fill} / {@code drawCenteredString} 走 {@code GuiComponent} 的静态方法（本类继承自它，可直接调），
 * 文字用 {@code Font#draw}（不带阴影，对应 1.20 的 {@code drawString(..., false)}）。
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractGeneratorScreen<M extends AbstractGeneratorMenu<?>> extends AbstractContainerScreen<M> {
    protected static final int TEXT_COLOR = 4210752; // 0x404040 深灰
    /**
     * 方向按钮内物品图标的目标像素尺寸（按钮高 12 px，图标填满整按钮高度，无边距）
     */
    protected static final int FACE_ICON_SIZE = 12;
    /**
     * 六方向名（小写）的语言键前缀。三个生成机的方向按钮共用这一组键，水车马达另有自己的一组
     */
    protected static final String FACE_NAME_KEY = "screen.autoresource.energy_generator.face.";
    /**
     * 已登记的六方向按钮：render 里逐个判 hover 以渲染 tooltip。
     * <b>每次 {@link #init()} 都会清空重来</b>——它在窗口缩放时会被重复调用，不清会越积越多、留下收不到鼠标事件的幽灵按钮
     */
    private final List<FaceButton> faceButtons = new ArrayList<>();

    protected AbstractGeneratorScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.faceButtons.clear();
    }

    /**
     * 在按钮内居中绘制缩放后的物品图标（用于方向按钮显示相邻方块）
     *
     * @param x            按钮左边界
     * @param y            按钮上边界
     * @param buttonWidth  按钮宽度
     * @param buttonHeight 按钮高度
     * @param stack        要渲染的物品栈
     */
    protected void renderFaceIcon(PoseStack poseStack, int x, int y, int buttonWidth, int buttonHeight, ItemStack stack) {
        // 物品默认渲染为 16×16 px；按目标尺寸等比缩放
        float scale = FACE_ICON_SIZE / 16.0F;
        // 居中绘制（缩放后尺寸 = FACE_ICON_SIZE 像素）
        int iconX = x + (buttonWidth - FACE_ICON_SIZE) / 2;
        int iconY = y + (buttonHeight - FACE_ICON_SIZE) / 2;
        // 1.19.2 的 ItemRenderer 用的是 RenderSystem 的模型视图矩阵，**不读传进来的 PoseStack**，
        // 所以缩放必须施加到它上面，画完再还原（renderGuiItem 自己会在其上再叠 z=100 的平移）
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.translate(iconX, iconY, 0.0D);
        modelView.scale(scale, scale, 1.0F);
        RenderSystem.applyModelViewMatrix();
        this.itemRenderer.renderGuiItem(stack, 0, 0);
        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
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
    public void render(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
        // 六方向按钮的 hover tooltip（排在槽位提示之前）
        for (FaceButton faceButton : this.faceButtons) {
            if (faceButton != null && faceButton.isButtonHovered()) {
                renderTooltip(poseStack, faceButton.buildTooltip(), Optional.empty(), mouseX, mouseY, this.font);
            }
        }
        // 渲染鼠标悬浮物品信息提示窗（render 不会自动调用）
        super.renderTooltip(poseStack, mouseX, mouseY);
        // 刷新各开关状态
        refreshButtonStates();
    }

    /**
     * 登记一个六方向按钮：既加入渲染列表，也记进 tooltip 集合
     */
    protected void addFaceButton(@Nonnull FaceButton button) {
        this.faceButtons.add(button);
        this.addRenderableWidget(button);
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
        public void renderButton(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            renderButtonWithLabel(poseStack, this.state ? 0xFF00AA00 : 0xFFAA0000);
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

        /**
         * hover tooltip：内容行（启用/禁用）、输出方向、输出目标三行，版式见 {@link FaceTooltip}
         * <p>
         * 开关状态直接读菜单而不是缓存的 {@code state} 字段——后者在 {@code render()} 里晚于 tooltip 渲染才刷新，用它慢一帧
         */
        @Nonnull
        List<Component> buildTooltip() {
            Direction direction = this.direction;
            String directionName = Component.translatable(FACE_NAME_KEY + direction.getName()).getString();
            String content = Component.translatable(AbstractGeneratorScreen.this.menu.isFaceEnabled(direction)
                    ? "screen.autoresource.output.enabled"
                    : "screen.autoresource.output.disabled").getString();
            ItemStack neighbor = AbstractGeneratorScreen.this.menu.getNeighborStack(direction);
            String target = neighbor.isEmpty() ? null : neighbor.getHoverName().getString();
            return FaceTooltip.build(directionName, target, content);
        }

        @Override
        public void renderButton(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            renderButtonBg(poseStack, this.state ? 0xFF00AA00 : 0xFFAA0000);
            ItemStack neighbor = AbstractGeneratorScreen.this.menu.getNeighborStack(this.direction);
            if (!neighbor.isEmpty()) {
                // 有相邻方块：只显示物品图标，按 FACE_ICON_SIZE 缩放后居中（按钮高 12 px，图标填满整按钮高度）
                renderFaceIcon(poseStack, this.x, this.y, this.getWidth(), this.getHeight(), neighbor);
            } else {
                // 无相邻方块：显示方向名，居中
                drawCenteredString(poseStack, AbstractGeneratorScreen.this.font, this.getMessage(), this.x + this.getWidth() / 2, this.y + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
            }
        }
    }

    /**
     * 带边框与居中文字的通用按钮。
     * <p>
     * 1.19.2 的挂钩是 {@code AbstractWidget#renderButton}（它自己先算好 {@code isHovered} 再调它），
     * 相当于 1.20 的 {@code renderWidget}。覆写后**不调 super**，就等于丢掉原版按钮贴图改自绘。
     */
    protected abstract class SimpleButton extends Button {
        SimpleButton(int x, int y, int width, int height, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress);
        }

        /**
         * 1.19.2 的 {@code isHovered} 是 {@code AbstractWidget} 的 protected 字段（1.20 才是公开的
         * {@code isHovered()} 方法），外部类读不到，这里开一个只读入口供 tooltip 判定使用
         */
        boolean isButtonHovered() {
            return this.isHovered;
        }

        /**
         * 仅绘制按钮背景与边框，不含居中文字（子类自行决定文字或图标）
         */
        protected void renderButtonBg(PoseStack poseStack, int color) {
            fill(poseStack, this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), color);
            // 1px 边框（鼠标悬浮时边框变亮，用于指示可交互）
            int borderColor = this.isHovered ? 0xFFFFFF00 : 0xFF000000;
            fill(poseStack, this.x - 1, this.y - 1, this.x + this.getWidth() + 1, this.y, borderColor);
            fill(poseStack, this.x - 1, this.y + this.getHeight(), this.x + this.getWidth() + 1, this.y + this.getHeight() + 1, borderColor);
            fill(poseStack, this.x - 1, this.y, this.x, this.y + this.getHeight(), borderColor);
            fill(poseStack, this.x + this.getWidth(), this.y, this.x + this.getWidth() + 1, this.y + this.getHeight(), borderColor);
        }

        /**
         * 绘制按钮背景+边框+居中文字
         */
        protected void renderButtonWithLabel(PoseStack poseStack, int color) {
            renderButtonBg(poseStack, color);
            drawCenteredString(poseStack, AbstractGeneratorScreen.this.font, this.getMessage(), this.x + this.getWidth() / 2, this.y + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }
}
