package cn.sd.jrz.autoresource.items;

import cn.sd.jrz.autoresource.DataConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class EnergyGeneratorItem extends BlockItem {
    private final DataConfig config;

    public EnergyGeneratorItem(Block block, DataConfig config) {
        super(block, new Properties().stacksTo(1).fireResistant());
        this.config = config;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        double output = config.getMin();
        long energy = 0;
        long tickCount = 0;
        long nextIncrease = config.getStep();
        boolean wirelessOn = false;
        long second = config.getSecond();
        long step = config.getStep();
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTagElement("BlockEntityTag");
            if (tag != null) {
                if (tag.contains("output", Tag.TAG_LONG)) {
                    output = tag.getLong("output");
                }
                if (tag.contains("energy", Tag.TAG_LONG)) {
                    energy = tag.getLong("energy");
                }
                if (tag.contains("tickCount", Tag.TAG_LONG)) {
                    tickCount = tag.getLong("tickCount");
                }
                if (tag.contains("nextIncrease", Tag.TAG_LONG)) {
                    nextIncrease = tag.getLong("nextIncrease");
                } else if (tag.contains("beaconIncrease", Tag.TAG_LONG)) {
                    // 兼容旧字段名
                    nextIncrease = tag.getLong("beaconIncrease");
                }
                if (tag.contains("wirelessOn", Tag.TAG_BYTE)) {
                    wirelessOn = tag.getBoolean("wirelessOn");
                }
            }
        }
        double percent = (int) (tickCount / 20.00D / second * 10000) / 100.00D;
        tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.energy", energy));
        tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.output", output));
        if (output >= config.getMax()) {
            tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.next_max"));
        } else {
            tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.next", nextIncrease));
        }
        if (output < config.getMax()) {
            tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.growth", percent));
        } else {
            tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.growth_max"));
        }
        tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.step", second, step));
        tooltip.add(Component.translatable(wirelessOn ? "item.autoresource.energy_generator.tooltip.wireless_on" : "item.autoresource.energy_generator.tooltip.wireless_off"));
        // 加速物品名称来自配置文件，方便其他作者修改
        Item starItem = config.getStarItem();
        if (starItem != null) {
            tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.group_faster", starItem.getDescription()));
        }
        tooltip.add(Component.translatable("item.autoresource.energy_generator.tooltip.tip"));
    }
}
