package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.menu.EnergyGeneratorMenu;
import cn.sd.jrz.autoresource.util.Tool;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * FE 发电机 GUI：展示发电量/电量/下次增长/增长百分比，并提供无线充电开关、扫描间隔、区块范围、重复传电次数与六个输电面开关；数值用单位缩写。
 */
@OnlyIn(Dist.CLIENT)
public class EnergyGeneratorScreen extends AbstractGeneratorScreen<EnergyGeneratorMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("autoresource", "textures/gui/energy_generator_gui.png");

    private StateButton wirelessButton;
    private FaceButton faceDown;
    private FaceButton faceUp;
    private FaceButton faceNorth;
    private FaceButton faceSouth;
    private FaceButton faceWest;
    private FaceButton faceEast;

    public EnergyGeneratorScreen(EnergyGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 310;
        this.inventoryLabelY = 214;
    }

    @Override
    protected void init() {
        super.init();
        // 无线充电开关
        this.wirelessButton = new StateButton(this.leftPos + 108, this.topPos + 77, 40, 12, this.menu.isWirelessOn(), Component.empty(), button -> sendButton(EnergyGeneratorMenu.BUTTON_WIRELESS));
        this.addRenderableWidget(this.wirelessButton);
        // 扫描间隔
        this.addRenderableWidget(new MiniButton(this.leftPos + 108, this.topPos + 93, 16, 12, Component.literal("-"), button -> sendButton(EnergyGeneratorMenu.BUTTON_INTERVAL_DOWN)));
        this.addRenderableWidget(new MiniButton(this.leftPos + 132, this.topPos + 93, 16, 12, Component.literal("+"), button -> sendButton(EnergyGeneratorMenu.BUTTON_INTERVAL_UP)));
        // 区块范围
        this.addRenderableWidget(new MiniButton(this.leftPos + 108, this.topPos + 109, 16, 12, Component.literal("-"), button -> sendButton(EnergyGeneratorMenu.BUTTON_RANGE_DOWN)));
        this.addRenderableWidget(new MiniButton(this.leftPos + 132, this.topPos + 109, 16, 12, Component.literal("+"), button -> sendButton(EnergyGeneratorMenu.BUTTON_RANGE_UP)));
        // 重复传电次数（两个框下方独立一行）
        this.addRenderableWidget(new MiniButton(this.leftPos + 128, this.topPos + 173, 16, 12, Component.literal("-"), button -> sendButton(EnergyGeneratorMenu.BUTTON_REPEAT_DOWN)));
        this.addRenderableWidget(new MiniButton(this.leftPos + 148, this.topPos + 173, 16, 12, Component.literal("+"), button -> sendButton(EnergyGeneratorMenu.BUTTON_REPEAT_UP)));
        // 六个输电面（宽 44 以容纳英文面名；有相邻方块时按钮居中显示方块图标，无相邻方块时显示方向名；hover 显示三行提示）
        this.faceDown = new FaceButton(this.leftPos + 17, this.topPos + 136, 44, 12, Direction.DOWN, this.menu.isFaceEnabled(Direction.DOWN), Component.translatable("screen.autoresource.energy_generator.face.down"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_DOWN));
        this.faceUp = new FaceButton(this.leftPos + 66, this.topPos + 136, 44, 12, Direction.UP, this.menu.isFaceEnabled(Direction.UP), Component.translatable("screen.autoresource.energy_generator.face.up"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_UP));
        this.faceNorth = new FaceButton(this.leftPos + 115, this.topPos + 136, 44, 12, Direction.NORTH, this.menu.isFaceEnabled(Direction.NORTH), Component.translatable("screen.autoresource.energy_generator.face.north"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_NORTH));
        this.faceSouth = new FaceButton(this.leftPos + 17, this.topPos + 152, 44, 12, Direction.SOUTH, this.menu.isFaceEnabled(Direction.SOUTH), Component.translatable("screen.autoresource.energy_generator.face.south"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_SOUTH));
        this.faceWest = new FaceButton(this.leftPos + 66, this.topPos + 152, 44, 12, Direction.WEST, this.menu.isFaceEnabled(Direction.WEST), Component.translatable("screen.autoresource.energy_generator.face.west"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_WEST));
        this.faceEast = new FaceButton(this.leftPos + 115, this.topPos + 152, 44, 12, Direction.EAST, this.menu.isFaceEnabled(Direction.EAST), Component.translatable("screen.autoresource.energy_generator.face.east"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_EAST));
        addFaceButton(this.faceDown);
        addFaceButton(this.faceUp);
        addFaceButton(this.faceNorth);
        addFaceButton(this.faceSouth);
        addFaceButton(this.faceWest);
        addFaceButton(this.faceEast);
    }

    @Override
    protected void renderBg(@Nonnull PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        renderBackground(poseStack);
        // 1.19.2 的 blit 不收 ResourceLocation，纹理要先绑到 0 号纹理单元
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(poseStack, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        // 增长进度条
        int trackLeft = this.leftPos + 12;
        int trackRight = this.leftPos + 164;
        int trackTop = this.topPos + 60;
        fill(poseStack, trackLeft, trackTop, trackRight, trackTop + 4, 0xFF555555);
        int percent = growthPercent();
        if (percent > 0) {
            int filled = (trackRight - trackLeft) * percent / 100;
            fill(poseStack, trackLeft, trackTop, trackLeft + filled, trackTop + 4, 0xFF00AA00);
        }
    }

    @Override
    protected void renderLabels(@Nonnull PoseStack poseStack, int mouseX, int mouseY) {
        super.renderLabels(poseStack, mouseX, mouseY);
        EnergyGeneratorMenu menu = this.menu;
        boolean maxed = menu.getOutput() >= menu.getMax();
        // 信息面板（大数值用单位缩写；达最大发电量时下次增长显示"已达最大电量"）
        this.font.draw(poseStack, Component.translatable("screen.autoresource.energy_generator.energy", Tool.formatLong(menu.getEnergy())), 12, 19, TEXT_COLOR);
        this.font.draw(poseStack, Component.translatable("screen.autoresource.energy_generator.output", Tool.formatLong(menu.getOutput())), 12, 29, TEXT_COLOR);
        this.font.draw(poseStack, Component.translatable("screen.autoresource.energy_generator.next", maxed ? Component.translatable("screen.autoresource.energy_generator.next_max") : Component.literal(Tool.formatLong(menu.getNextIncrease()))), 12, 39, TEXT_COLOR);
        this.font.draw(poseStack, Component.translatable("screen.autoresource.energy_generator.growth", growthPercent()), 12, 49, TEXT_COLOR);
        // 无线充电参数（间隔带单位）
        this.font.draw(poseStack, Component.translatable("screen.autoresource.energy_generator.wireless"), 12, 79, TEXT_COLOR);
        this.font.draw(poseStack, Component.translatable("screen.autoresource.energy_generator.interval"), 12, 95, TEXT_COLOR);
        this.font.draw(poseStack, Component.translatable("screen.autoresource.energy_generator.interval_value", menu.getInterval()), 64, 95, TEXT_COLOR);
        this.font.draw(poseStack, Component.translatable("screen.autoresource.energy_generator.range"), 12, 111, TEXT_COLOR);
        this.font.draw(poseStack, Component.literal(menu.getRange() + "x" + menu.getRange()), 64, 111, TEXT_COLOR);
        // 重复传电次数（带单位）
        this.font.draw(poseStack, Component.translatable("screen.autoresource.energy_generator.repeat"), 12, 175, TEXT_COLOR);
        this.font.draw(poseStack, Component.translatable("screen.autoresource.energy_generator.repeat_value", menu.getRepeat()), 56, 175, TEXT_COLOR);
        // 加速槽标签：显示配置的目标物品名
        Item starItem = menu.getStarItem();
        if (starItem != null) {
            this.font.draw(poseStack, starItem.getDescription(), 28, 195, TEXT_COLOR);
        }
        // 充电槽标签：右对齐贴近充电槽
        Component chargeLabel = Component.translatable("screen.autoresource.energy_generator.charge_slot");
        this.font.draw(poseStack, chargeLabel, 150 - this.font.width(chargeLabel), 195, TEXT_COLOR);
    }

    @Override
    protected void refreshButtonStates() {
        boolean wireless = this.menu.isWirelessOn();
        this.wirelessButton.setState(wireless);
        this.wirelessButton.setMessage(Component.translatable(wireless ? "screen.autoresource.energy_generator.wireless_on" : "screen.autoresource.energy_generator.wireless_off"));
        this.faceDown.setState(this.menu.isFaceEnabled(Direction.DOWN));
        this.faceUp.setState(this.menu.isFaceEnabled(Direction.UP));
        this.faceNorth.setState(this.menu.isFaceEnabled(Direction.NORTH));
        this.faceSouth.setState(this.menu.isFaceEnabled(Direction.SOUTH));
        this.faceWest.setState(this.menu.isFaceEnabled(Direction.WEST));
        this.faceEast.setState(this.menu.isFaceEnabled(Direction.EAST));
    }

    /**
     * 灰色小按钮（+/-）
     */
    private class MiniButton extends SimpleButton {
        MiniButton(int x, int y, int width, int height, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress);
        }

        @Override
        public void renderButton(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            renderButtonWithLabel(poseStack, 0xFF808080);
        }
    }
}
