package net.styna.stynastmc.fonts;

import net.styna.stynastmc.config.StynaConfigSpec;

public final class FontsConfig {
    public static final StynaConfigSpec SPEC;
    public static final StynaConfigSpec.BooleanValue ENABLED;
    public static final StynaConfigSpec.ConfigValue<String> FAMILY;
    public static final StynaConfigSpec.ConfigValue<String> VARIANT;
    public static final StynaConfigSpec.DoubleValue SIZE;
    public static final StynaConfigSpec.DoubleValue OVERSAMPLE;
    public static final StynaConfigSpec.DoubleValue SHIFT_X;
    public static final StynaConfigSpec.DoubleValue SHIFT_Y;

    private FontsConfig() {
    }

    public static FontSettings settings() {
        return new FontSettings(FAMILY.get(), VARIANT.get(), SIZE.get().floatValue(), OVERSAMPLE.get().floatValue(),
                SHIFT_X.get().floatValue(), SHIFT_Y.get().floatValue());
    }

    public static void save(boolean enabled, FontSettings settings) {
        ENABLED.set(enabled);
        FAMILY.set(settings.family());
        VARIANT.set(settings.variant());
        SIZE.set((double) settings.size());
        OVERSAMPLE.set((double) settings.oversample());
        SHIFT_X.set((double) settings.shiftX());
        SHIFT_Y.set((double) settings.shiftY());
        SPEC.save();
    }

    static {
        StynaConfigSpec.Builder builder = new StynaConfigSpec.Builder();
        builder.push("font");
        ENABLED = builder.comment("Replace the default game font with the Google Font below.").define("enabled", false);
        FAMILY = builder.comment("Google Fonts family name, e.g. Roboto.").define("family", "");
        VARIANT = builder.comment("Weight, with an i suffix for italic, e.g. 400 or 700i.").define("variant", "400");
        SIZE = builder.comment("Glyph size in GUI pixels.").defineInRange("size", 10.0, 4.0, 32.0);
        OVERSAMPLE = builder.comment("Render resolution multiplier. Higher is sharper at large GUI scales.").defineInRange("oversample", 4.0, 1.0, 16.0);
        SHIFT_X = builder.defineInRange("shift_x", 0.0, -8.0, 8.0);
        SHIFT_Y = builder.defineInRange("shift_y", 0.0, -8.0, 8.0);
        builder.pop();
        SPEC = builder.build();
    }
}
