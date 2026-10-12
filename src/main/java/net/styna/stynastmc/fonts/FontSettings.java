package net.styna.stynastmc.fonts;

import java.util.Objects;

public final class FontSettings {
    public final String family;
    public final String variant;
    public final float size;
    public final float oversample;
    public final float shiftX;
    public final float shiftY;

    public FontSettings(String family, String variant, float size, float oversample, float shiftX, float shiftY) {
        this.family = family;
        this.variant = variant;
        this.size = size;
        this.oversample = oversample;
        this.shiftX = shiftX;
        this.shiftY = shiftY;
    }

    public FontSettings withFamily(String family, String variant) {
        return new FontSettings(family, variant, size, oversample, shiftX, shiftY);
    }

    public FontSettings withVariant(String variant) {
        return new FontSettings(family, variant, size, oversample, shiftX, shiftY);
    }

    public FontSettings withSize(float size) {
        return new FontSettings(family, variant, size, oversample, shiftX, shiftY);
    }

    public FontSettings withOversample(float oversample) {
        return new FontSettings(family, variant, size, oversample, shiftX, shiftY);
    }

    public FontSettings withShift(float shiftX, float shiftY) {
        return new FontSettings(family, variant, size, oversample, shiftX, shiftY);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FontSettings s && family.equals(s.family) && variant.equals(s.variant) && size == s.size
                && oversample == s.oversample && shiftX == s.shiftX && shiftY == s.shiftY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(family, variant, size, oversample, shiftX, shiftY);
    }
}
