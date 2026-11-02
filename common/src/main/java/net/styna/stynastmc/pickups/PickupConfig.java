package net.styna.stynastmc.pickups;

import net.styna.stynastmc.config.StynaConfigSpec;

public final class PickupConfig {
    public static final StynaConfigSpec SPEC;
    public static final StynaConfigSpec.BooleanValue ENABLED;
    public static final StynaConfigSpec.BooleanValue ITEMS;
    public static final StynaConfigSpec.BooleanValue XP;
    public static final StynaConfigSpec.BooleanValue SHOW_TOTAL;
    public static final StynaConfigSpec.BooleanValue OTHER_MODS;
    public static final StynaConfigSpec.EnumValue<Corner> CORNER;
    public static final StynaConfigSpec.IntValue X_OFFSET;
    public static final StynaConfigSpec.IntValue Y_OFFSET;
    public static final StynaConfigSpec.IntValue MAX_ENTRIES;
    public static final StynaConfigSpec.IntValue MAX_NAME_WIDTH;
    public static final StynaConfigSpec.DoubleValue DISPLAY_TIME;

    public enum Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;

        public boolean right() {
            return this == TOP_RIGHT || this == BOTTOM_RIGHT;
        }

        public boolean bottom() {
            return this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
        }
    }

    private PickupConfig() {
    }

    static {
        StynaConfigSpec.Builder builder = new StynaConfigSpec.Builder();
        builder.push("general");
        ENABLED = builder.comment("Show a notification when you pick something up.")
                .define("enabled", true);
        ITEMS = builder.comment("Notify for picked up items.")
                .define("items", true);
        XP = builder.comment("Notify for picked up experience.")
                .define("xp", true);
        SHOW_TOTAL = builder.comment("Show how many of the item you now carry.")
                .define("show_total", true);
        OTHER_MODS = builder.comment("Also catch items other mods pick up without the vanilla pickup, like backpack pickup upgrades.")
                .define("other_mods", true);
        builder.pop();

        builder.push("layout");
        CORNER = builder.comment("Screen corner the notifications stack from.")
                .defineEnum("corner", Corner.BOTTOM_RIGHT);
        X_OFFSET = builder.comment("Distance from the side of the screen.")
                .defineInRange("offset_x", 0, -256, 256);
        Y_OFFSET = builder.comment("Distance from the top or bottom of the screen.")
                .defineInRange("offset_y", 32, -256, 256);
        MAX_ENTRIES = builder.comment("Most notifications shown at once.")
                .defineInRange("max_entries", 9, 1, 32);
        MAX_NAME_WIDTH = builder.comment("Longer item names are cut off at this width.")
                .defineInRange("max_name_width", 100, 20, 400);
        builder.pop();

        builder.push("timing");
        DISPLAY_TIME = builder.comment("Seconds a notification stays after its last pickup.")
                .defineInRange("display_time", 4.0, 1.0, 60.0);
        builder.pop();
        SPEC = builder.build();
    }

    public static boolean on(StynaConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() && value.get();
    }
}
