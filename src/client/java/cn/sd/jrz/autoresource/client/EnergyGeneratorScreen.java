package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.menu.EnergyGeneratorMenu;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
        super(menu, playerInventory, title, 176, 310);
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
    public void extractContents(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 26.x 起 AbstractContainerScreen 没有 renderBg/renderLabels 分拆，背景与文字统一在 extractContents 内绘制
        super.extractContents(guiGraphics, mouseX, mouseY, partialTick);
        // 背景纹理
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 0.0F, 0.0F, this.imageWidth, this.imageHeight);
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
        // 文字标签（按原 renderLabels 的相对坐标，与父类一致基于 leftPos/topPos 偏移）
        EnergyGeneratorMenu menu = this.menu;
        boolean maxed = menu.getOutput() >= menu.getMax();
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.energy", Tool.formatLong(menu.getEnergy())), this.leftPos + 12, this.topPos + 19, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.output", Tool.formatLong(menu.getOutput())), this.leftPos + 12, this.topPos + 29, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.next", maxed ? Component.translatable("screen.autoresource.energy_generator.next_max") : Component.literal(Tool.formatLong(menu.getNextIncrease()))), this.leftPos + 12, this.topPos + 39, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.growth", growthPercent()), this.leftPos + 12, this.topPos + 49, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.wireless"), this.leftPos + 12, this.topPos + 79, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.interval"), this.leftPos + 12, this.topPos + 95, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.interval_value", menu.getInterval()), this.leftPos + 64, this.topPos + 95, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.range"), this.leftPos + 12, this.topPos + 111, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.literal(menu.getRange() + "x" + menu.getRange()), this.leftPos + 64, this.topPos + 111, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.repeat"), this.leftPos + 12, this.topPos + 175, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.repeat_value", menu.getRepeat()), this.leftPos + 56, this.topPos + 175, TEXT_COLOR, false);
        Item starItem = menu.getStarItem();
        if (starItem != null) {
            // 26.x 起 Item.getDescription() 移除，且 getName 现在需要 ItemStack 参数
            guiGraphics.text(this.font, starItem.getName(ItemStack.EMPTY), this.leftPos + 28, this.topPos + 195, TEXT_COLOR, false);
        }
        Component chargeLabel = Component.translatable("screen.autoresource.energy_generator.charge_slot");
        guiGraphics.text(this.font, chargeLabel, this.leftPos + 150 - this.font.width(chargeLabel), this.topPos + 195, TEXT_COLOR, false);
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
        protected void extractContents(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButtonInternal(guiGraphics, 0xFF808080);
        }
    }
}