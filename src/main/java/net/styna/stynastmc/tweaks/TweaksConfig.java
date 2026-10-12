package net.styna.stynastmc.tweaks;

import net.styna.stynastmc.config.StynaConfigSpec;

public final class TweaksConfig {
    public static final StynaConfigSpec SPEC;
    public static final StynaConfigSpec.BooleanValue ARMOR_HUD;
    public static final StynaConfigSpec.BooleanValue NO_BOBBING;
    public static final StynaConfigSpec.BooleanValue DISABLE_DARKNESS;
    public static final StynaConfigSpec.BooleanValue STATS_SCREEN;

    private TweaksConfig() {
    }

    static {
        StynaConfigSpec.Builder builder = new StynaConfigSpec.Builder();
        builder.push("hud");
        ARMOR_HUD = builder.comment("Show worn armor and its durability next to the hotbar.")
                .define("armor_hud", true);
        builder.pop();

        builder.push("world");
        NO_BOBBING = builder.comment("Disable view bobbing while walking.")
                .define("no_bobbing", false);
        DISABLE_DARKNESS = builder.comment("Turn off the Darkness mod's dark nights and caves, restoring vanilla lighting.")
                .define("disable_darkness", false);
        builder.pop();

        builder.push("screens");
        STATS_SCREEN = builder.comment("Replace the vanilla statistics screen.")
                .define("stats_screen", true);
        builder.pop();

        SPEC = builder.build();
    }

    public static boolean on(StynaConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() && value.get();
    }
}
