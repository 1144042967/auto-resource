package cn.sd.jrz.autoresource.util;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Tool {
    public static long suit(long value) {
        return value < 0 ? Long.MAX_VALUE : value;
    }

    public static int suitInt(long value) {
        return value < 0 || value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    /**
     * 归一化无线充电区块范围，只允许 1/3/5（1x1、3x3、5x5 区块）。
     * 配置中若填写了 2 或 4 等非标准值，向上归一到最近的奇数。
     */
    public static int normalizeWirelessRange(int range) {
        return range <= 1 ? 1 : (range <= 3 ? 3 : 5);
    }

    /**
     * 格式化大数值用于 GUI 展示。小于 10000 时原样显示，更大时使用 K/M/G/T/P/E 单位缩写（2 位小数），
     * 避免 long 最大值等超长数字在 GUI 中溢出。
     */
    public static String formatLong(long value) {
        if (value < 0) {
            return Long.toString(value);
        }
        if (value < 10_000L) {
            return Long.toString(value);
        }
        if (value < 1_000_000L) {
            return String.format("%.2fK", value / 1_000.0);
        }
        if (value < 1_000_000_000L) {
            return String.format("%.2fM", value / 1_000_000.0);
        }
        if (value < 1_000_000_000_000L) {
            return String.format("%.2fG", value / 1_000_000_000.0);
        }
        if (value < 1_000_000_000_000_000L) {
            return String.format("%.2fT", value / 1_000_000_000_000.0);
        }
        if (value < 1_000_000_000_000_000_000L) {
            return String.format("%.2fP", value / 1_000_000_000_000_000.0);
        }
        return String.format("%.2fE", value / 1_000_000_000_000_000_000.0);
    }

    public static void takeItem(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            ItemEntity entity = player.drop(stack, false);
            if (entity != null) {
                entity.setNoPickUpDelay();
                entity.setTarget(player.getUUID());
            }
        }
    }
}
