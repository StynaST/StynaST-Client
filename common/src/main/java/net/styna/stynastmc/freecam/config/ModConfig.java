package net.styna.stynastmc.freecam.config;

import net.minecraft.world.level.block.Block;
import net.styna.stynastmc.freecam.FreecamConfigImpl;
import net.styna.stynastmc.freecam.config.model.FlightMode;
import net.styna.stynastmc.freecam.config.model.Perspective;

public interface ModConfig {

    static ModConfig get() {
        return FreecamConfigImpl.INSTANCE;
    }

    FlightMode getFlightMode();

    double getHorizontalSpeed();

    double getVerticalSpeed();

    boolean ignoreAllCollision();

    boolean shouldCheckInitialCollision();

    boolean ignoreCollisionWith(Block block);

    Perspective getInitialPerspective();

    boolean shouldShowPlayer();

    default boolean shouldHidePlayer() {
        return !shouldShowPlayer();
    }

    boolean shouldShowHand();

    default boolean shouldHideHand() {
        return !shouldShowHand();
    }

    boolean isFullBrightEnabled();

    boolean shouldShowSubmersionFog();

    default boolean shouldHideSubmersionFog() {
        return !shouldShowSubmersionFog();
    }

    boolean shouldDisableOnDamage();

    boolean shouldFreezePlayer();

    boolean shouldPreventInteractions();

    boolean allowInteractionsFromCamera();

    boolean allowInteractionsFromPlayer();

    boolean isRestrictedOnServer(String serverIp);

    boolean shouldNotifyFreecam();

    boolean shouldNotifyTripod();

    boolean shouldOutlinePlayer();
}
