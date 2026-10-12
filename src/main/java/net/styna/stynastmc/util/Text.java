package net.styna.stynastmc.util;

import net.minecraft.client.resources.I18n;

/** Translation shorthands; 1.12 GUIs draw plain formatted strings instead of components. */
public final class Text {
    private Text() {
    }

    public static String tr(String key, Object... args) {
        return I18n.format(key, args);
    }

    public static boolean has(String key) {
        return I18n.hasKey(key);
    }

    /** Wraps text in a 0xRRGGBB color by picking the closest legacy formatting code. */
    public static String colored(String text, int rgb) {
        return closest(rgb) + text + "§r";
    }

    private static final int[] PALETTE = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    public static String closest(int rgb) {
        int best = 15;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < PALETTE.length; i++) {
            int p = PALETTE[i];
            long dr = ((rgb >> 16) & 0xFF) - ((p >> 16) & 0xFF);
            long dg = ((rgb >> 8) & 0xFF) - ((p >> 8) & 0xFF);
            long db = (rgb & 0xFF) - (p & 0xFF);
            long d = dr * dr + dg * dg + db * db;
            if (d < bestDistance) {
                bestDistance = d;
                best = i;
            }
        }
        return "§" + "0123456789abcdef".charAt(best);
    }

    public static String repeat(String s, int count) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < count; i++) {
            out.append(s);
        }
        return out.toString();
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
