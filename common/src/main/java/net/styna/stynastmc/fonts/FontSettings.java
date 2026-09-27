package net.styna.stynastmc.fonts;

public record FontSettings(String family, String variant, float size, float oversample, float shiftX, float shiftY) {
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
}
