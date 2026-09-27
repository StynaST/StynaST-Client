package net.styna.stynastmc.classicbar;

import net.minecraft.resources.Identifier;

/** Vanilla HUD layer ids, matching NeoForge's VanillaGuiLayers. Fabric maps these to its own element ids. */
public final class HudLayers {
    public static final Identifier PLAYER_HEALTH = Identifier.withDefaultNamespace("player_health");
    public static final Identifier ARMOR_LEVEL = Identifier.withDefaultNamespace("armor_level");
    public static final Identifier FOOD_LEVEL = Identifier.withDefaultNamespace("food_level");
    public static final Identifier VEHICLE_HEALTH = Identifier.withDefaultNamespace("vehicle_health");
    public static final Identifier AIR_LEVEL = Identifier.withDefaultNamespace("air_level");
    public static final Identifier CONTEXTUAL_INFO_BAR_BACKGROUND = Identifier.withDefaultNamespace("contextual_info_bar_background");
    public static final Identifier EXPERIENCE_LEVEL = Identifier.withDefaultNamespace("experience_level");
    public static final Identifier CONTEXTUAL_INFO_BAR = Identifier.withDefaultNamespace("contextual_info_bar");

    private HudLayers() {
    }
}
