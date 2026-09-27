package net.styna.stynastmc.classicbar.api;

import java.util.LinkedHashMap;
import java.util.Map;

import net.styna.stynastmc.classicbar.HudLayers;
import net.styna.stynastmc.classicbar.api.colorprovider.DualEffectColorProvider;
import net.styna.stynastmc.classicbar.api.colorprovider.SingleColorProvider;
import net.styna.stynastmc.classicbar.api.colorprovider.StackingColorProvider;
import net.styna.stynastmc.classicbar.api.colorprovider.StackingEffectColorProvider;
import net.styna.stynastmc.classicbar.api.colorprovider.TransitioningColorProvider;
import net.styna.stynastmc.classicbar.api.colorprovider.TransitioningEffectColorProvider;
import net.styna.stynastmc.classicbar.impl.overlays.vanilla.Absorption;
import net.styna.stynastmc.classicbar.impl.overlays.vanilla.Air;
import net.styna.stynastmc.classicbar.impl.overlays.vanilla.Armor;
import net.styna.stynastmc.classicbar.impl.overlays.vanilla.Experience;
import net.styna.stynastmc.classicbar.impl.overlays.vanilla.Food;
import net.styna.stynastmc.classicbar.impl.overlays.vanilla.Health;
import net.styna.stynastmc.classicbar.impl.overlays.vanilla.MountHealth;

public final class BarRegistry {
    /** Default bars, keyed by name. Written to config files on first load. */
    public static final Map<String, BarOverlay> REGISTRY = new LinkedHashMap<>();

    private BarRegistry() {
    }

    public static void registerBar(BarOverlay defaults) {
        REGISTRY.put(defaults.name(), defaults);
    }

    static {
        registerBar(new Health(BarSettings.builder()
                .setDisablesOverlay(HudLayers.PLAYER_HEALTH)
                .setColorProvider(TransitioningEffectColorProvider.DEFAULT)
                .build()));
        registerBar(new MountHealth(BarSettings.builder()
                .setDisablesOverlay(HudLayers.VEHICLE_HEALTH)
                .setSide(BarSide.RIGHT)
                .setColorProvider(TransitioningColorProvider.DEFAULT)
                .build()));
        registerBar(new Food(BarSettings.builder()
                .setSide(BarSide.RIGHT)
                .setColorProvider(DualEffectColorProvider.FOOD)
                .setDisablesOverlay(HudLayers.FOOD_LEVEL)
                .build(), true, true, true));
        registerBar(new Air(BarSettings.builder()
                .setDisablesOverlay(HudLayers.AIR_LEVEL)
                .setSide(BarSide.RIGHT)
                .setColorProvider(new SingleColorProvider(Color.hex2Color("#00E6E6")))
                .build()));
        registerBar(new Armor(BarSettings.builder()
                .setDisablesOverlay(HudLayers.ARMOR_LEVEL)
                .setColorProvider(StackingColorProvider.DEFAULT_ARMOR)
                .build()));
        registerBar(new Absorption(BarSettings.builder()
                .fitted()
                .setColorProvider(StackingEffectColorProvider.DEFAULT_ABSORPTION)
                .build()));
        registerBar(new Experience(BarSettings.builder()
                .setDisablesOverlay(HudLayers.EXPERIENCE_LEVEL)
                .setSide(BarSide.RIGHT)
                .setColorProvider(new SingleColorProvider(Color.hex2Color("#80FF20")))
                .build()));
    }
}
