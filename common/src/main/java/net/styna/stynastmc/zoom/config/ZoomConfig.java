package net.styna.stynastmc.zoom.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ZoomConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue DEFAULT_LEVEL;
    public static final ModConfigSpec.DoubleValue MAX_LEVEL;
    public static final ModConfigSpec.BooleanValue SCROLL;
    public static final ModConfigSpec.DoubleValue SCROLL_STEP;
    public static final ModConfigSpec.BooleanValue SMOOTH;
    public static final ModConfigSpec.DoubleValue SMOOTH_SPEED;
    public static final ModConfigSpec.BooleanValue SCALE_SENSITIVITY;
    public static final ModConfigSpec.BooleanValue CINEMATIC_CAMERA;

    private ZoomConfig() {
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");
        DEFAULT_LEVEL = builder.comment("Zoom level when the key is first pressed.")
                .defineInRange("default_level", 3.0, 1.0, 50.0);
        MAX_LEVEL = builder.comment("Highest zoom level reachable by scrolling.")
                .defineInRange("max_level", 50.0, 1.0, 100.0);
        builder.pop();

        builder.push("scroll");
        SCROLL = builder.comment("Scroll while zoomed to change the zoom level.")
                .define("enabled", true);
        SCROLL_STEP = builder.comment("Zoom multiplier per scroll notch.")
                .defineInRange("step", 1.1, 1.01, 2.0);
        builder.pop();

        builder.push("feel");
        SMOOTH = builder.comment("Animate zooming in and out.")
                .define("smooth", true);
        SMOOTH_SPEED = builder.comment("Higher is snappier.")
                .defineInRange("smooth_speed", 12.0, 1.0, 50.0);
        SCALE_SENSITIVITY = builder.comment("Lower mouse sensitivity while zoomed so aiming stays steady.")
                .define("scale_sensitivity", true);
        CINEMATIC_CAMERA = builder.comment("Use smooth camera while zoomed.")
                .define("cinematic_camera", false);
        builder.pop();
        SPEC = builder.build();
    }
}
