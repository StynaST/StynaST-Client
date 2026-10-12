package net.styna.stynastmc.tweaks;

import java.util.List;

import net.styna.stynastmc.config.StynaConfigSpec;

public final class TweaksConfig {
    public static final StynaConfigSpec SPEC;
    public static final StynaConfigSpec.BooleanValue ARMOR_HUD;
    public static final StynaConfigSpec.BooleanValue DURABILITY_TOOLTIP;
    public static final StynaConfigSpec.BooleanValue NO_FOG;
    public static final StynaConfigSpec.BooleanValue NO_BOBBING;
    public static final StynaConfigSpec.ConfigValue<List<? extends String>> BOOK_TARGETS;
    public static final StynaConfigSpec.IntValue CYCLE_DELAY;

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
        NO_BOBBING = builder.comment("Disable view bobbing while walking.")
                .define("no_bobbing", false);
        builder.pop();

        builder.push("trading");
        BOOK_TARGETS = builder.comment("Librarian books Trade Cycling auto cycle stops on, as enchantment=minimum level.")
                .defineList("book_targets", () -> List.<String>of(), () -> "minecraft:efficiency=5", String.class::isInstance);
        CYCLE_DELAY = builder.comment("Ticks to wait after new offers arrive before cycling again.")
                .defineInRange("cycle_delay", 2, 0, 20);
        builder.pop();

        SPEC = builder.build();
    }

    public static boolean on(StynaConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() && value.get();
    }
}
