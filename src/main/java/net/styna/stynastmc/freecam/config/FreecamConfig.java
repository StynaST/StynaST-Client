package net.styna.stynastmc.freecam.config;

import net.styna.stynastmc.config.StynaConfigSpec;

/** Freecam settings. The camera always collides with blocks on this version, so there is no noclip option. */
public final class FreecamConfig {
    public static final StynaConfigSpec SPEC;

    public static final StynaConfigSpec.EnumValue<FlightMode> FLIGHT_MODE;
    public static final StynaConfigSpec.DoubleValue HORIZONTAL_SPEED;
    public static final StynaConfigSpec.DoubleValue VERTICAL_SPEED;
    public static final StynaConfigSpec.EnumValue<Perspective> PERSPECTIVE;
    public static final StynaConfigSpec.BooleanValue SHOW_PLAYER;
    public static final StynaConfigSpec.BooleanValue SHOW_HAND;
    public static final StynaConfigSpec.BooleanValue FULL_BRIGHT;
    public static final StynaConfigSpec.BooleanValue OUTLINE_PLAYER;
    public static final StynaConfigSpec.BooleanValue DISABLE_ON_DAMAGE;
    public static final StynaConfigSpec.BooleanValue FREEZE_PLAYER;
    public static final StynaConfigSpec.BooleanValue ALLOW_INTERACT;
    public static final StynaConfigSpec.EnumValue<InteractionMode> INTERACTION_MODE;
    public static final StynaConfigSpec.BooleanValue NOTIFY_FREECAM;
    public static final StynaConfigSpec.BooleanValue NOTIFY_TRIPOD;

    public enum InteractionMode {
        CAMERA, PLAYER
    }

    private FreecamConfig() {
    }

    static {
        StynaConfigSpec.Builder builder = new StynaConfigSpec.Builder();
        builder.push("movement");
        FLIGHT_MODE = builder.comment("CREATIVE keeps vanilla flight feel, DEFAULT uses the freecam flight model.")
                .defineEnum("flight_mode", FlightMode.DEFAULT);
        HORIZONTAL_SPEED = builder.defineInRange("horizontal_speed", 1.0, 0.01, 20.0);
        VERTICAL_SPEED = builder.defineInRange("vertical_speed", 1.0, 0.01, 20.0);
        builder.pop();

        builder.push("visual");
        PERSPECTIVE = builder.comment("Camera perspective entered on toggle.")
                .defineEnum("perspective", Perspective.INSIDE);
        SHOW_PLAYER = builder.define("show_player", true);
        SHOW_HAND = builder.define("show_hand", false);
        FULL_BRIGHT = builder.define("full_bright", false);
        OUTLINE_PLAYER = builder.define("outline_player", false);
        builder.pop();

        builder.push("utility");
        DISABLE_ON_DAMAGE = builder.define("disable_on_damage", true);
        FREEZE_PLAYER = builder.comment("Stop the real player from moving at all while in freecam.")
                .define("freeze_player", false);
        ALLOW_INTERACT = builder.comment("Allow attacking, mining and using items while in freecam.")
                .define("allow_interact", false);
        INTERACTION_MODE = builder.comment("Where interactions originate when allow_interact is on.")
                .defineEnum("interaction_mode", InteractionMode.CAMERA);
        builder.pop();

        builder.push("notification");
        NOTIFY_FREECAM = builder.define("notify_freecam", true);
        NOTIFY_TRIPOD = builder.define("notify_tripod", true);
        builder.pop();
        SPEC = builder.build();
    }

    public static boolean preventInteractions() {
        return !ALLOW_INTERACT.get();
    }

    public static boolean interactFromPlayer() {
        return ALLOW_INTERACT.get() && INTERACTION_MODE.get() == InteractionMode.PLAYER;
    }
}
