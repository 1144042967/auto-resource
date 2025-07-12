package cn.sd.jrz.autoresource.util;

public class Tool {
    public static long suit(long value) {
        return value < 0 ? Long.MAX_VALUE : value;
    }

    public static long suit(String value) {
        try {
            return suit(Long.parseLong(value));
        } catch (Exception e) {
            return 0;
        }
    }

    public static int suitInt(long value) {
        return value < 0 || value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
