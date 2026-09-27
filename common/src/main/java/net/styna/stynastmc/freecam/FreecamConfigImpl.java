package net.styna.stynastmc.freecam;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.styna.stynastmc.config.StynaConfigSpec;
import net.styna.stynastmc.freecam.config.ModConfig;
import net.styna.stynastmc.freecam.config.model.FlightMode;
import net.styna.stynastmc.freecam.config.model.Perspective;

/**
 * Backs the vendored Freecam config interface with the shared config spec.
 * Defaults match upstream Freecam.
 */
public final class FreecamConfigImpl implements ModConfig {
    public static final FreecamConfigImpl INSTANCE = new FreecamConfigImpl();

    public static final StynaConfigSpec SPEC;

    private static final StynaConfigSpec.EnumValue<FlightMode> FLIGHT_MODE;
    private static final StynaConfigSpec.DoubleValue HORIZONTAL_SPEED;
    private static final StynaConfigSpec.DoubleValue VERTICAL_SPEED;
    private static final StynaConfigSpec.BooleanValue IGNORE_ALL_COLLISION;
    private static final StynaConfigSpec.BooleanValue ALWAYS_CHECK_COLLISION;
    private static final StynaConfigSpec.EnumValue<Perspective> PERSPECTIVE;
    private static final StynaConfigSpec.BooleanValue SHOW_PLAYER;
    private static final StynaConfigSpec.BooleanValue SHOW_HAND;
    private static final StynaConfigSpec.BooleanValue FULL_BRIGHT;
    private static final StynaConfigSpec.BooleanValue SHOW_SUBMERSION;
    private static final StynaConfigSpec.BooleanValue OUTLINE_PLAYER;
    private static final StynaConfigSpec.BooleanValue DISABLE_ON_DAMAGE;
    private static final StynaConfigSpec.BooleanValue FREEZE_PLAYER;
    private static final StynaConfigSpec.BooleanValue ALLOW_INTERACT;
    private static final StynaConfigSpec.EnumValue<InteractionMode> INTERACTION_MODE;
    private static final StynaConfigSpec.BooleanValue NOTIFY_FREECAM;
    private static final StynaConfigSpec.BooleanValue NOTIFY_TRIPOD;

    public enum InteractionMode {
        CAMERA, PLAYER
    }

    private FreecamConfigImpl() {
    }

    static {
        StynaConfigSpec.Builder builder = new StynaConfigSpec.Builder();
        builder.push("movement");
        FLIGHT_MODE = builder.comment("CREATIVE keeps vanilla flight feel, DEFAULT uses the freecam flight model.")
                .defineEnum("flight_mode", FlightMode.DEFAULT);
        HORIZONTAL_SPEED = builder.defineInRange("horizontal_speed", 1.0, 0.01, 20.0);
        VERTICAL_SPEED = builder.defineInRange("vertical_speed", 1.0, 0.01, 20.0);
        builder.pop();

        builder.push("collision");
        IGNORE_ALL_COLLISION = builder.comment("Fly through blocks. Singleplayer only.")
                .define("ignore_all", true);
        ALWAYS_CHECK_COLLISION = builder.comment("Check collision at the spot freecam starts in.")
                .define("always_check", false);
        builder.pop();

        builder.push("visual");
        PERSPECTIVE = builder.comment("Camera perspective entered on toggle.")
                .defineEnum("perspective", Perspective.INSIDE);
        SHOW_PLAYER = builder.define("show_player", true);
        SHOW_HAND = builder.define("show_hand", false);
        FULL_BRIGHT = builder.define("full_bright", false);
        SHOW_SUBMERSION = builder.comment("Show water/lava overlay while the camera is submerged.")
                .define("show_submersion", false);
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

    @Override
    public FlightMode getFlightMode() {
        return FLIGHT_MODE.get();
    }

    @Override
    public double getHorizontalSpeed() {
        return HORIZONTAL_SPEED.get();
    }

    @Override
    public double getVerticalSpeed() {
        return VERTICAL_SPEED.get();
    }

    @Override
    public boolean ignoreAllCollision() {
        return IGNORE_ALL_COLLISION.get() && !onRemoteServer();
    }

    @Override
    public boolean shouldCheckInitialCollision() {
        return ALWAYS_CHECK_COLLISION.get() || onRemoteServer();
    }

    @Override
    public boolean ignoreCollisionWith(Block block) {
        return ignoreAllCollision();
    }

    // Noclip only allowed in singleplayer/own LAN world
    private static boolean onRemoteServer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && !mc.hasSingleplayerServer();
    }

    @Override
    public Perspective getInitialPerspective() {
        return PERSPECTIVE.get();
    }

    @Override
    public boolean shouldShowPlayer() {
        return SHOW_PLAYER.get();
    }

    @Override
    public boolean shouldShowHand() {
        return SHOW_HAND.get();
    }

    @Override
    public boolean isFullBrightEnabled() {
        return FULL_BRIGHT.get();
    }

    @Override
    public boolean shouldShowSubmersionFog() {
        return SHOW_SUBMERSION.get();
    }

    @Override
    public boolean shouldDisableOnDamage() {
        return DISABLE_ON_DAMAGE.get();
    }

    @Override
    public boolean shouldFreezePlayer() {
        return FREEZE_PLAYER.get();
    }

    @Override
    public boolean shouldPreventInteractions() {
        return !ALLOW_INTERACT.get();
    }

    @Override
    public boolean allowInteractionsFromCamera() {
        return ALLOW_INTERACT.get() && INTERACTION_MODE.get() == InteractionMode.CAMERA;
    }

    @Override
    public boolean allowInteractionsFromPlayer() {
        return ALLOW_INTERACT.get() && INTERACTION_MODE.get() == InteractionMode.PLAYER;
    }

    @Override
    public boolean isRestrictedOnServer(String serverIp) {
        return false;
    }

    @Override
    public boolean shouldNotifyFreecam() {
        return NOTIFY_FREECAM.get();
    }

    @Override
    public boolean shouldNotifyTripod() {
        return NOTIFY_TRIPOD.get();
    }

    @Override
    public boolean shouldOutlinePlayer() {
        return OUTLINE_PLAYER.get();
    }
}
