package net.styna.stynastmc.classicbar;

import org.slf4j.Logger;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.Identifier;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.classicbar.config.ClassicBarConfig;

/**
 * Port of Classic Bar by Tfarcenim: replaces the vanilla status icons with bars.
 */
public final class ClassicBar {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Identifier LAYER_ID = StynaSTMC.id("classicbar");

    private ClassicBar() {
    }

    public static boolean hidesLayer(Identifier layer) {
        return BarHud.replaces(layer) || (isContextualBar(layer) && BarHud.hidesExperienceBar());
    }

    private static boolean isContextualBar(Identifier layer) {
        return layer.equals(HudLayers.CONTEXTUAL_INFO_BAR) || layer.equals(HudLayers.CONTEXTUAL_INFO_BAR_BACKGROUND);
    }

    public static <S> LiteralArgumentBuilder<S> command() {
        return LiteralArgumentBuilder.<S>literal("classicbar")
                .then(LiteralArgumentBuilder.<S>literal("reload").executes(c -> {
                    BarHud.reload();
                    return 1;
                }))
                .then(LiteralArgumentBuilder.<S>literal("reset").executes(c -> {
                    ClassicBarConfig.resetToDefaults();
                    BarHud.reload();
                    return 1;
                }));
    }
}
