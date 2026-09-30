package net.styna.stynastmc.tweaks;

import net.styna.stynastmc.config.StynaConfigSpec;

/** File only, no settings tab. Mirrors options ShulkerBoxTooltip ships. */
public final class ContainerPreviewConfig {
    public static final StynaConfigSpec SPEC;
    public static final StynaConfigSpec.BooleanValue ENABLED;
    public static final StynaConfigSpec.BooleanValue ALWAYS_ON;

    private ContainerPreviewConfig() {
    }

    static {
        StynaConfigSpec.Builder builder = new StynaConfigSpec.Builder();
        builder.push("container_preview");
        ENABLED = builder.comment("Preview shulker box and container contents. Shift shows a compact view, Alt+Shift the full grid.")
                .define("enabled", true);
        ALWAYS_ON = builder.comment("Show the compact preview without holding Shift.")
                .define("always_on", false);
        builder.pop();
        SPEC = builder.build();
    }

    public static boolean on(StynaConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
