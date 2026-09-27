package net.styna.stynastmc.tweaks;

import net.styna.stynastmc.config.StynaConfigSpec;

public final class TweaksConfig {
    public static final StynaConfigSpec SPEC;
    public static final StynaConfigSpec.BooleanValue ARMOR_HUD;
    public static final StynaConfigSpec.BooleanValue DURABILITY_TOOLTIP;
    public static final StynaConfigSpec.BooleanValue NO_FOG;
    public static final StynaConfigSpec.BooleanValue KEYBIND_SEARCH;

    private TweaksConfig() {
    }

    static {
        StynaConfigSpec.Builder builder = new StynaConfigSpec.Builder();
        builder.push("hud");
        ARMOR_HUD = builder.comment("Show worn armor and its durability next to the hotbar.")
                .define("armor_hud", true);
        DURABILITY_TOOLTIP = builder.comment("Show remaining durability in item tooltips.")
                .define("durability_tooltip", true);
        builder.pop();

        builder.push("world");
        NO_FOG = builder.comment("Remove distance and atmospheric fog. Water, lava and effect fog stay.")
                .define("no_fog", true);
        builder.pop();

        builder.push("screens");
        KEYBIND_SEARCH = builder.comment("Search and conflict filters on the key binds screen.")
                .define("keybind_search", true);
        builder.pop();
        SPEC = builder.build();
    }

    public static boolean on(StynaConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() && value.get();
    }
}
