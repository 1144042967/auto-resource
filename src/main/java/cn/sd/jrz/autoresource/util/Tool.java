package cn.sd.jrz.autoresource.util;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class Tool {
    public static long suit(long value) {
        return value < 0 ? Long.MAX_VALUE : value;
    }

    public static int suitInt(long value) {
        return value < 0 || value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    public static void takeItem(PlayerEntity player, ItemStack stack) {
        if (!player.addItem(stack)) {
            ItemEntity entity = player.drop(stack, false);
            if (entity != null) {
                entity.setNoPickUpDelay();
            }
        }
    }
}
