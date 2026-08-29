package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.menu.EnergyGeneratorMenu;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * FE 发电机 GUI（26.x 适配）。
 * <p>
 * 展示当前发电量、当前电量、下次增长的发电量、增长百分比（含进度条），
 * 并提供无线充电开关、扫描间隔、区块范围、重复传电次数以及六个输电面的独立开关。
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
        this.wirelessButton = new StateButton(this.leftPos + 108, this.topPos + 77, 40, 12, this.menu.isWirelessOn(), Component.empty(), _ -> sendButton(EnergyGeneratorMenu.BUTTON_WIRELESS));
        this.addRenderableWidget(this.wirelessButton);
        this.addRenderableWidget(new MiniButton(this.leftPos + 108, this.topPos + 93, 16, 12, Component.literal("-"), _ -> sendButton(EnergyGeneratorMenu.BUTTON_INTERVAL_DOWN)));
        this.addRenderableWidget(new MiniButton(this.leftPos + 132, this.topPos + 93, 16, 12, Component.literal("+"), _ -> sendButton(EnergyGeneratorMenu.BUTTON_INTERVAL_UP)));
        this.addRenderableWidget(new MiniButton(this.leftPos + 108, this.topPos + 109, 16, 12, Component.literal("-"), _ -> sendButton(EnergyGeneratorMenu.BUTTON_RANGE_DOWN)));
        this.addRenderableWidget(new MiniButton(this.leftPos + 132, this.topPos + 109, 16, 12, Component.literal("+"), _ -> sendButton(EnergyGeneratorMenu.BUTTON_RANGE_UP)));
        this.addRenderableWidget(new MiniButton(this.leftPos + 128, this.topPos + 173, 16, 12, Component.literal("-"), _ -> sendButton(EnergyGeneratorMenu.BUTTON_REPEAT_DOWN)));
        this.addRenderableWidget(new MiniButton(this.leftPos + 148, this.topPos + 173, 16, 12, Component.literal("+"), _ -> sendButton(EnergyGeneratorMenu.BUTTON_REPEAT_UP)));
        this.faceDown = new FaceButton(this.leftPos + 17, this.topPos + 136, 44, 12, Direction.DOWN, this.menu.isFaceEnabled(Direction.DOWN), Component.translatable("screen.autoresource.energy_generator.face.down"), _ -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_DOWN));
        this.faceUp = new FaceButton(this.leftPos + 66, this.topPos + 136, 44, 12, Direction.UP, this.menu.isFaceEnabled(Direction.UP), Component.translatable("screen.autoresource.energy_generator.face.up"), _ -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_UP));
        this.faceNorth = new FaceButton(this.leftPos + 115, this.topPos + 136, 44, 12, Direction.NORTH, this.menu.isFaceEnabled(Direction.NORTH), Component.translatable("screen.autoresource.energy_generator.face.north"), _ -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_NORTH));
        this.faceSouth = new FaceButton(this.leftPos + 17, this.topPos + 152, 44, 12, Direction.SOUTH, this.menu.isFaceEnabled(Direction.SOUTH), Component.translatable("screen.autoresource.energy_generator.face.south"), _ -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_SOUTH));
        this.faceWest = new FaceButton(this.leftPos + 66, this.topPos + 152, 44, 12, Direction.WEST, this.menu.isFaceEnabled(Direction.WEST), Component.translatable("screen.autoresource.energy_generator.face.west"), _ -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_WEST));
        this.faceEast = new FaceButton(this.leftPos + 115, this.topPos + 152, 44, 12, Direction.EAST, this.menu.isFaceEnabled(Direction.EAST), Component.translatable("screen.autoresource.energy_generator.face.east"), _ -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_EAST));
        this.addRenderableWidget(this.faceDown);
        this.addRenderableWidget(this.faceUp);
        this.addRenderableWidget(this.faceNorth);
        this.addRenderableWidget(this.faceSouth);
        this.addRenderableWidget(this.faceWest);
        this.addRenderableWidget(this.faceEast);
    }

    @Override
    public void extractBackground(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        // 增长进度条（绿色）
        drawGrowthBar(guiGraphics, this.topPos + 60, 0xFF00AA00);
    }

    @Override
    protected void extractLabels(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        EnergyGeneratorMenu menu = this.menu;
        boolean maxed = menu.getOutput() >= menu.getMax();
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.energy", Tool.formatLong(menu.getEnergy())), 12, 19, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.output", Tool.formatLong(menu.getOutput())), 12, 29, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.next", maxed ? Component.translatable("screen.autoresource.energy_generator.next_max") : Component.literal(Tool.formatLong(menu.getNextIncrease()))), 12, 39, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.growth", growthPercent()), 12, 49, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.wireless"), 12, 79, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.interval"), 12, 95, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.interval_value", menu.getInterval()), 64, 95, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.range"), 12, 111, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.literal(menu.getRange() + "x" + menu.getRange()), 64, 111, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.repeat"), 12, 175, TEXT_COLOR, false);
        guiGraphics.text(this.font, Component.translatable("screen.autoresource.energy_generator.repeat_value", menu.getRepeat()), 56, 175, TEXT_COLOR, false);
        Item starItem = menu.getStarItem();
        if (starItem != null) {
            // 26.x：Item.getName(stack) 只读 ITEM_NAME 组件，需用 getHoverName 获取物品显示名
            guiGraphics.text(this.font, new ItemStack(starItem).getHoverName(), 28, 195, TEXT_COLOR, false);
        }
        Component chargeLabel = Component.translatable("screen.autoresource.energy_generator.charge_slot");
        guiGraphics.text(this.font, chargeLabel, 150 - this.font.width(chargeLabel), 195, TEXT_COLOR, false);
    }

    @Override
    public void extractRenderState(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
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
}
