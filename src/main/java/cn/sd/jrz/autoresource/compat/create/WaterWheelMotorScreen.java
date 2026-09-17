package cn.sd.jrz.autoresource.compat.create;

import cn.sd.jrz.autoresource.client.FaceTooltip;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 水车马达 GUI。布局：上部大框放六面输出方向按钮；中部左侧水车槽位、右侧旋转方向开关；
 * 下部物品栏名称 + 当前转速（3 位补零）。
 */
@OnlyIn(Dist.CLIENT)
public class WaterWheelMotorScreen extends AbstractContainerScreen<WaterWheelMotorMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("autoresource", "textures/gui/water_wheel_motor_gui.png");
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
    /**
     * 已登记的六方向按钮：render 里逐个判 hover 以渲染 tooltip。
     * <b>每次 {@link #init()} 都会清空重来</b>——它在窗口缩放时会被重复调用，不清会越积越多、留下收不到鼠标事件的幽灵按钮
     */
    private final List<FaceButton> faceButtons = new ArrayList<>();

    public WaterWheelMotorScreen(WaterWheelMotorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 177;
        this.inventoryLabelY = 80;
    }

    /**
     * 在按钮内居中绘制缩放后的物品图标（用于方向按钮显示相邻方块）
     */
    private void renderFaceIcon(PoseStack poseStack, int x, int y, int buttonWidth, int buttonHeight, ItemStack stack) {
        float scale = FACE_ICON_SIZE / 16.0F;
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

    @Override
    protected void init() {
        super.init();
        this.faceButtons.clear();
        // 旋转方向开关（水车槽同一行右侧，无标签）
        this.directionButton = new StateButton(this.leftPos + 112, this.topPos + 58, 56, 12, this.menu.isCounterClockwise(), Component.empty(), button -> sendButton(WaterWheelMotorMenu.BUTTON_DIRECTION));
        this.addRenderableWidget(this.directionButton);
        // 六面输出方向：位于上部大框正中（两行三列，框 y=16~51）；有相邻方块时按钮居中显示方块图标，无相邻方块时显示方向名；hover 显示三行提示
        this.faceDown = new FaceButton(this.leftPos + 14, this.topPos + 20, 44, 12, Direction.DOWN, this.menu.getFace() == Direction.DOWN, faceLabel(Direction.DOWN), button -> sendButton(WaterWheelMotorMenu.BUTTON_FACE_DOWN));
        this.faceUp = new FaceButton(this.leftPos + 66, this.topPos + 20, 44, 12, Direction.UP, this.menu.getFace() == Direction.UP, faceLabel(Direction.UP), button -> sendButton(WaterWheelMotorMenu.BUTTON_FACE_UP));
        this.faceNorth = new FaceButton(this.leftPos + 118, this.topPos + 20, 44, 12, Direction.NORTH, this.menu.getFace() == Direction.NORTH, faceLabel(Direction.NORTH), button -> sendButton(WaterWheelMotorMenu.BUTTON_FACE_NORTH));
        this.faceSouth = new FaceButton(this.leftPos + 14, this.topPos + 36, 44, 12, Direction.SOUTH, this.menu.getFace() == Direction.SOUTH, faceLabel(Direction.SOUTH), button -> sendButton(WaterWheelMotorMenu.BUTTON_FACE_SOUTH));
        this.faceWest = new FaceButton(this.leftPos + 66, this.topPos + 36, 44, 12, Direction.WEST, this.menu.getFace() == Direction.WEST, faceLabel(Direction.WEST), button -> sendButton(WaterWheelMotorMenu.BUTTON_FACE_WEST));
        this.faceEast = new FaceButton(this.leftPos + 118, this.topPos + 36, 44, 12, Direction.EAST, this.menu.getFace() == Direction.EAST, faceLabel(Direction.EAST), button -> sendButton(WaterWheelMotorMenu.BUTTON_FACE_EAST));
        addFaceButton(this.faceDown);
        addFaceButton(this.faceUp);
        addFaceButton(this.faceNorth);
        addFaceButton(this.faceSouth);
        addFaceButton(this.faceWest);
        addFaceButton(this.faceEast);
    }

    /**
     * 登记一个六方向按钮：既加入渲染列表，也记进 tooltip 集合
     */
    private void addFaceButton(@Nonnull FaceButton button) {
        this.faceButtons.add(button);
        this.addRenderableWidget(button);
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
    protected void renderBg(@Nonnull PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        renderBackground(poseStack);
        // 面板与槽位背景已烘焙进纹理（water_wheel_motor_gui.png），可用 PS 直接修改
        // 1.19.2 的 blit 不收 ResourceLocation，纹理要先绑到 0 号纹理单元
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(poseStack, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(@Nonnull PoseStack poseStack, int mouseX, int mouseY) {
        super.renderLabels(poseStack, mouseX, mouseY);
        WaterWheelMotorMenu menu = this.menu;
        // 水车槽位提示（槽位右侧）
        this.font.draw(poseStack, Component.translatable("screen.autoresource.water_wheel_motor.wheel_slot"), 28, 61, TEXT_COLOR);
        // 物品栏行右侧显示当前转速（3 位补零，如 004 RPM；右对齐）
        String speedText = Component.translatable("screen.autoresource.water_wheel_motor.speed", String.format("%03d", menu.getSpeed())).getString();
        this.font.draw(poseStack, speedText, 168 - this.font.width(speedText), 80, TEXT_COLOR);
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
        super.renderTooltip(poseStack, mouseX, mouseY);
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
        public void renderButton(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            renderButtonWithLabel(poseStack, this.state ? 0xFF00AA00 : 0xFFAA0000);
        }
    }

    /**
     * 六方向按钮：该方向有相邻方块时只显示物品图标（居中）；无相邻方块时显示方向名（居中）。绿=选中/红=未选
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

        /**
         * hover tooltip：内容行（选中/未选中）、输出方向、输出目标三行，版式见 {@link FaceTooltip}
         * <p>
         * 选中状态直接读菜单而不是缓存的 {@code state} 字段——后者在 {@code render()} 里晚于 tooltip 渲染才刷新，用它慢一帧
         */
        @Nonnull
        List<Component> buildTooltip() {
            Direction direction = this.direction;
            String directionName = faceLabel(direction).getString();
            String content = Component.translatable(WaterWheelMotorScreen.this.menu.getFace() == direction
                    ? "screen.autoresource.output.selected"
                    : "screen.autoresource.output.unselected").getString();
            ItemStack neighbor = WaterWheelMotorScreen.this.menu.getNeighborStack(direction);
            String target = neighbor.isEmpty() ? null : neighbor.getHoverName().getString();
            return FaceTooltip.build(directionName, target, content);
        }

        @Override
        public void renderButton(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            renderButtonBg(poseStack, this.state ? 0xFF00AA00 : 0xFFAA0000);
            ItemStack neighbor = WaterWheelMotorScreen.this.menu.getNeighborStack(this.direction);
            if (!neighbor.isEmpty()) {
                // 有相邻方块：只显示物品图标，按 FACE_ICON_SIZE 缩放后居中（按钮高 12 px，上下至少留 2 px 边距）
                WaterWheelMotorScreen.this.renderFaceIcon(poseStack, this.x, this.y, this.getWidth(), this.getHeight(), neighbor);
            } else {
                // 无相邻方块：显示方向名，居中
                drawCenteredString(poseStack, WaterWheelMotorScreen.this.font, this.getMessage(), this.x + this.getWidth() / 2, this.y + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
            }
        }
    }

    /**
     * 带边框与居中文字的通用按钮。
     * <p>
     * 1.19.2 的挂钩是 {@code AbstractWidget#renderButton}（它自己先算好 {@code isHovered} 再调它），
     * 相当于 1.20 的 {@code renderWidget}。覆写后**不调 super**，就等于丢掉原版按钮贴图改自绘。
     */
    private abstract class SimpleButton extends Button {
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
            // 1px 边框（鼠标悬浮时边框变亮）
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
            drawCenteredString(poseStack, WaterWheelMotorScreen.this.font, this.getMessage(), this.x + this.getWidth() / 2, this.y + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }
}
