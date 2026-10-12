package net.styna.stynastmc.firstperson;

import java.util.List;

import net.styna.stynastmc.config.StynaConfigSpec;

public final class FirstPersonConfig {
    public enum VanillaHands {
        OFF, ALL, ALL_DOUBLE, ITEMS
    }

    public static final StynaConfigSpec SPEC;
    public static final StynaConfigSpec.BooleanValue ENABLED_BY_DEFAULT;
    public static final StynaConfigSpec.BooleanValue RENDER_STUCK_FEATURES;
    public static final StynaConfigSpec.IntValue X_OFFSET;
    public static final StynaConfigSpec.IntValue SNEAK_X_OFFSET;
    public static final StynaConfigSpec.IntValue SIT_X_OFFSET;
    public static final StynaConfigSpec.EnumValue<VanillaHands> VANILLA_HANDS;
    public static final StynaConfigSpec.BooleanValue DYNAMIC_MODE;
    public static final StynaConfigSpec.BooleanValue SKIP_SWIMMING;
    public static final StynaConfigSpec.ConfigValue<List<? extends String>> AUTO_VANILLA_HANDS;
    public static final StynaConfigSpec.ConfigValue<List<? extends String>> AUTO_DISABLE;

    private FirstPersonConfig() {
    }

    static {
        StynaConfigSpec.Builder builder = new StynaConfigSpec.Builder();
        builder.push("general");
        ENABLED_BY_DEFAULT = builder.comment("Show your body in first person after starting the game. The key toggles it.")
                .define("enabled_by_default", true);
        RENDER_STUCK_FEATURES = builder.comment("Show arrows and bee stingers stuck in your body.")
                .define("render_stuck_features", true);
        builder.pop();

        builder.push("offsets");
        X_OFFSET = builder.comment("Moves the body back and forth relative to the camera while standing.")
                .defineInRange("x_offset", 0, -40, 40);
        SNEAK_X_OFFSET = builder.comment("Moves the body back and forth relative to the camera while sneaking.")
                .defineInRange("sneak_x_offset", 0, -40, 40);
        SIT_X_OFFSET = builder.comment("Moves the body back and forth relative to the camera while riding.")
                .defineInRange("sit_x_offset", 0, -40, 40);
        builder.pop();

        builder.push("vanilla_hands");
        VANILLA_HANDS = builder.comment("Show the vanilla first person hands. OFF uses the body's arms, ITEMS only while holding something.")
                .defineEnum("mode", VanillaHands.OFF);
        DYNAMIC_MODE = builder.comment("Swap between vanilla hands and the body's arms depending on where you look.")
                .define("dynamic_mode", true);
        SKIP_SWIMMING = builder.comment("Hide the vanilla hands while swimming.")
                .define("skip_swimming", true);
        builder.pop();

        builder.push("items");
        AUTO_VANILLA_HANDS = builder.comment("Items that always use the vanilla hands, as namespace:item.")
                .defineList("auto_vanilla_hands", () -> List.of("antiqueatlas:antique_atlas", "twilightforest:filled_magic_map",
                        "twilightforest:filled_maze_map", "twilightforest:filled_ore_map", "create:potato_cannon",
                        "create:extendo_grip", "create:handheld_worldshaper", "map_atlases:atlas"),
                        () -> "minecraft:compass", String.class::isInstance);
        AUTO_DISABLE = builder.comment("Items that turn first person body off while held, as namespace:item.")
                .defineList("auto_disable", () -> List.of("exposure:camera"), () -> "minecraft:spyglass", String.class::isInstance);
        builder.pop();

        SPEC = builder.build();
    }
}
