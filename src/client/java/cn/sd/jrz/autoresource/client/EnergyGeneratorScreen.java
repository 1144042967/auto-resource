package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.menu.EnergyGeneratorMenu;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

import org.jetbrains.annotations.NotNull;

/**
 * FE 发电机 GUI：展示发电量/电量/下次增长/增长百分比，并提供无线充电开关、扫描间隔、区块范围、重复传电次数与六个输电面开关；数值用单位缩写。
 */
public class EnergyGeneratorScreen extends AbstractGeneratorScreen<EnergyGeneratorMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("autoresource", "textures/gui/energy_generator_gui.png");

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
        // 六个输电面（宽 44 以容纳英文面名）
        this.faceDown = new FaceButton(this.leftPos + 17, this.topPos + 136, 44, 12, Direction.DOWN, this.menu.isFaceEnabled(Direction.DOWN), Component.translatable("screen.autoresource.energy_generator.face.down"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_DOWN));
        this.faceUp = new FaceButton(this.leftPos + 66, this.topPos + 136, 44, 12, Direction.UP, this.menu.isFaceEnabled(Direction.UP), Component.translatable("screen.autoresource.energy_generator.face.up"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_UP));
        this.faceNorth = new FaceButton(this.leftPos + 115, this.topPos + 136, 44, 12, Direction.NORTH, this.menu.isFaceEnabled(Direction.NORTH), Component.translatable("screen.autoresource.energy_generator.face.north"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_NORTH));
        this.faceSouth = new FaceButton(this.leftPos + 17, this.topPos + 152, 44, 12, Direction.SOUTH, this.menu.isFaceEnabled(Direction.SOUTH), Component.translatable("screen.autoresource.energy_generator.face.south"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_SOUTH));
        this.faceWest = new FaceButton(this.leftPos + 66, this.topPos + 152, 44, 12, Direction.WEST, this.menu.isFaceEnabled(Direction.WEST), Component.translatable("screen.autoresource.energy_generator.face.west"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_WEST));
        this.faceEast = new FaceButton(this.leftPos + 115, this.topPos + 152, 44, 12, Direction.EAST, this.menu.isFaceEnabled(Direction.EAST), Component.translatable("screen.autoresource.energy_generator.face.east"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_EAST));
        this.addRenderableWidget(this.faceDown);
        this.addRenderableWidget(this.faceUp);
        this.addRenderableWidget(this.faceNorth);
        this.addRenderableWidget(this.faceSouth);
        this.addRenderableWidget(this.faceWest);
        this.addRenderableWidget(this.faceEast);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 注意：1.21.1 的 AbstractContainerScreen.renderBackground(4参) 内部会回调 renderBg，此处只能用只渲染背景的方法，否则无限递归
        renderMenuBackground(guiGraphics);
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
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        EnergyGeneratorMenu menu = this.menu;
        boolean maxed = menu.getOutput() >= menu.getMax();
        // 信息面板（大数值用单位缩写；达最大发电量时下次增长显示"已达最大电量"）
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.energy_generator.energy", Tool.formatLong(menu.getEnergy())), 12, 19, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.energy_generator.output", Tool.formatLong(menu.getOutput())), 12, 29, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.energy_generator.next", maxed ? Component.translatable("screen.autoresource.energy_generator.next_max") : Component.literal(Tool.formatLong(menu.getNextIncrease()))), 12, 39, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.energy_generator.growth", growthPercent()), 12, 49, TEXT_COLOR, false);
        // 无线充电参数（间隔带单位）
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.energy_generator.wireless"), 12, 79, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.energy_generator.interval"), 12, 95, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.energy_generator.interval_value", menu.getInterval()), 64, 95, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.energy_generator.range"), 12, 111, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.literal(menu.getRange() + "x" + menu.getRange()), 64, 111, TEXT_COLOR, false);
        // 重复传电次数（带单位）
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.energy_generator.repeat"), 12, 175, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.autoresource.energy_generator.repeat_value", menu.getRepeat()), 56, 175, TEXT_COLOR, false);
        // 加速槽标签：显示配置的目标物品名
        Item starItem = menu.getStarItem();
        if (starItem != null) {
            // 1.21.11：Item.getDescription() 移除，改用 getName()
            guiGraphics.drawString(this.font, starItem.getName(), 28, 195, TEXT_COLOR, false);
        }
        // 充电槽标签：右对齐贴近充电槽
        Component chargeLabel = Component.translatable("screen.autoresource.energy_generator.charge_slot");
        guiGraphics.drawString(this.font, chargeLabel, 150 - this.font.width(chargeLabel), 195, TEXT_COLOR, false);
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
        protected void renderContents(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButton(guiGraphics, 0xFF808080);
        }
    }
}
