package net.styna.stynastmc.classicbar.api;

import com.mojang.serialization.Codec;

import net.minecraft.util.Mth;

public record Color(int a, int r, int g, int b) {
    public static final Color WHITE = fromRGB(255, 255, 255);
    public static final Color BLACK = fromRGB(0, 0, 0);
    public static final Color RED = fromRGB(255, 0, 0);
    public static final Color YELLOW = fromRGB(255, 255, 0);
    public static final Color CLEAR = new Color(0, 0, 0, 0);
    public static final Codec<Color> HEX_CODEC = Codec.STRING.xmap(Color::hex2Color, Color::toHexString);

    public static Color fromRGB(int red, int green, int blue) {
        return fromRGBA(red, green, blue, 255);
    }

    public static Color fromRGBA(int red, int green, int blue, int alpha) {
        return new Color(alpha, red, green, blue);
    }

    public static Color hex2Color(String s) {
        s = s.startsWith("#") ? s.substring(1) : s;
        int i = Integer.parseUnsignedInt(s, 16);
        return fromRGB(i >> 16 & 0xFF, i >> 8 & 0xFF, i & 0xFF);
    }

    public Color withAlpha(float alpha) {
        return fromRGBA(r, g, b, (int) (alpha * 255.0F));
    }

    public Color colorBlend(Color c2, float d) {
        return fromRGB(Mth.lerpInt(d, r, c2.r), Mth.lerpInt(d, g, c2.g), Mth.lerpInt(d, b, c2.b));
    }

    public String toHexString() {
        return String.format("#%06X", argb() & 0xFFFFFF);
    }

    public int argb() {
        return a << 24 | r << 16 | g << 8 | b;
    }
}
