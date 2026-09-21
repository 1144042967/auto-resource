package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.menu.AbstractGeneratorMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * 机器 GUI 基类：共享按钮点击发送、增长百分比计算、进度条轨道绘制、六方向按钮 hover tooltip
 * 与开关/通用小按钮；子类负责槽位布局、进度条配色、数据展示与开关初始化。
 * 注意：26.x 不再支持 {@code @OnlyIn} 的运行时成员剥离，故不在此类使用该注解。
 */
public abstract class AbstractGeneratorScreen<M extends AbstractGeneratorMenu<?>> extends AbstractContainerScreen<M> {
    protected static final int TEXT_COLOR = 0xFF404040; // 0x404040 深灰（26.x 需带 alpha，否则透明）
    protected static final int FACE_ICON_SIZE = 12; // 方向按钮内的物品图标尺寸（按钮高 12px，图标填满整按钮高度）
    /**
     * 六方向名（小写）的语言键前缀。三个生成机的方向按钮共用这一组键
     */
    protected static final String FACE_NAME_KEY = "screen.autoresource.energy_generator.face.";

    /**
     * 已登记的六方向按钮：extractTooltip 里逐个判 hover 以渲染 tooltip。
     * <b>每次 {@link #init()} 都会清空重来</b>——窗口缩放时 init 会被重复调用，不清会越积越多、留下收不到鼠标事件的幽灵按钮
     */
    private final List<FaceButton> faceButtons = new ArrayList<>();

    protected AbstractGeneratorScreen(M menu, Inventory playerInventory, Component title, int imageWidth, int imageHeight) {
        super(menu, playerInventory, title, imageWidth, imageHeight);
    }

    @Override
    protected void init() {
        super.init();
        this.faceButtons.clear();
    }

    /**
     * 登记一个六方向按钮：既加入渲染列表，也记进 tooltip 集合
     */
    protected void addFaceButton(@Nonnull FaceButton button) {
        this.faceButtons.add(button);
        this.addRenderableWidget(button);
    }

    /**
     * 六方向按钮的 hover 提示，排在槽位提示之前。
     * <p>
     * 26.x 的 tooltip 是"本帧首个生效"（后设的被忽略），所以命中方向按钮后直接 return，
     * 不再调 {@code super}，方向按钮因此天然优先于槽位提示。
     * <p>
     * 此处读 {@code isHovered()} 是安全的：{@code extractTooltip} 在该帧的
     * {@code extractContents}（内含 renderables 遍历，{@code AbstractWidget} 在那里刷新 hover 状态）之后才执行。
     */
    @Override
    protected void extractTooltip(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        for (FaceButton faceButton : this.faceButtons) {
            if (faceButton.isHovered()) {
                guiGraphics.setComponentTooltipForNextFrame(this.font, faceButton.buildTooltip(), mouseX, mouseY);
                return;
            }
        }
        super.extractTooltip(guiGraphics, mouseX, mouseY);
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

        /**
         * 绘制按钮的背景填充与边框（不含居中文字）
         */
        protected void renderButtonBg(GuiGraphicsExtractor guiGraphics, int color) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            // 1px 边框（鼠标悬浮时边框变亮，用于指示可交互）
            int borderColor = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor);
        }

        /**
         * 绘制按钮背景、边框与居中文字
         */
        protected void renderButton(GuiGraphicsExtractor guiGraphics, int color) {
            renderButtonBg(guiGraphics, color);
            guiGraphics.centeredText(AbstractGeneratorScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }

    /**
     * 在按钮中心绘制缩放后的物品图标（26.x 的 {@code guiGraphics.item(stack, x, y)} 不支持 scale 参数，需用 PoseStack 缩放）
     */
    protected void renderFaceIcon(GuiGraphicsExtractor guiGraphics, int x, int y, int buttonWidth, int buttonHeight, ItemStack stack) {
        float scale = FACE_ICON_SIZE / 16.0F;
        int iconX = x + (buttonWidth - FACE_ICON_SIZE) / 2;
        int iconY = y + (buttonHeight - FACE_ICON_SIZE) / 2;
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        poseStack.translate(iconX, iconY);
        poseStack.scale(scale, scale);
        guiGraphics.item(stack, 0, 0);
        poseStack.popMatrix();
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
         * 开关状态直接读菜单而不是缓存的 {@code state} 字段——后者在 {@code extractRenderState} 里晚于
         * tooltip 才刷新，用它慢一帧
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
        protected void extractContents(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButtonBg(guiGraphics, this.state ? 0xFF00AA00 : 0xFFAA0000);
            ItemStack neighbor = AbstractGeneratorScreen.this.menu.getNeighborStack(this.direction);
            if (!neighbor.isEmpty()) {
                renderFaceIcon(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), neighbor);
            } else {
                guiGraphics.centeredText(AbstractGeneratorScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
            }
        }
    }
}
