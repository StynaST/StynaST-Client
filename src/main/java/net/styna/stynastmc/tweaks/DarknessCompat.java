package net.styna.stynastmc.tweaks;

import java.lang.reflect.Field;

import net.minecraftforge.fml.common.Loader;
import net.styna.stynastmc.StynaSTMC;

/**
 * Turns the Darkness mod off by blacklisting every dimension: an inverted, empty blacklist makes its lightmap hook bail out.
 * Its hook class loads before mixins can reach it, so this edits its config fields instead and puts them back when toggled off.
 */
public final class DarknessCompat {
    private static Field invert;
    private static Field byId;
    private static Field byName;
    private static boolean missing;
    private static boolean applied;
    private static boolean savedInvert;
    private static int[] savedIds;
    private static String[] savedNames;

    private DarknessCompat() {
    }

    public static void tick() {
        if (missing || !resolve()) {
            return;
        }
        boolean wanted = TweaksConfig.on(TweaksConfig.DISABLE_DARKNESS);
        try {
            if (wanted && !applied) {
                savedInvert = invert.getBoolean(null);
                savedIds = (int[]) byId.get(null);
                savedNames = (String[]) byName.get(null);
                applied = true;
            }
            if (wanted) {
                // Reasserted every tick in case Darkness resyncs its config.
                invert.setBoolean(null, true);
                byId.set(null, new int[0]);
                byName.set(null, new String[0]);
            } else if (applied) {
                invert.setBoolean(null, savedInvert);
                byId.set(null, savedIds);
                byName.set(null, savedNames);
                applied = false;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            missing = true;
            StynaSTMC.LOGGER.warn("Could not toggle the Darkness mod", e);
        }
    }

    private static boolean resolve() {
        if (invert != null) {
            return true;
        }
        if (!Loader.isModLoaded("darkness")) {
            missing = true;
            return false;
        }
        try {
            Class<?> config = Class.forName("io.github.yamporg.darkness.ModConfig");
            invert = config.getField("invertBlacklist");
            byId = config.getField("blacklistByID");
            byName = config.getField("blacklistByName");
            return true;
        } catch (ReflectiveOperationException e) {
            missing = true;
            StynaSTMC.LOGGER.warn("Darkness mod config not found", e);
            return false;
        }
    }
}
