package net.styna.stynastmc.compat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.styna.stynastmc.StynaSTMC;

/** Mods whose features we ship too. When one is installed our copy of that feature is left out entirely. */
public final class ModCompat {
    public static final String CHAT_HEADS = "chat_heads";
    public static final String ENCHANT_DESCRIPTIONS = "enchdesc";
    public static final String FIRST_PERSON = "firstperson";
    public static final String SHULKER_BOX_TOOLTIP = "shulkerboxtooltip";

    private static final Map<String, Boolean> LOADED = new ConcurrentHashMap<>();

    private ModCompat() {
    }

    public static boolean isLoaded(String modId) {
        return LOADED.computeIfAbsent(modId, ModCompat::lookup);
    }

    // Reflection because this runs from mixin plugins, before Platform exists, and common can't see loader APIs.
    private static boolean lookup(String modId) {
        try {
            Class<?> fabric = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = fabric.getMethod("getInstance").invoke(null);
            return (Boolean) fabric.getMethod("isModLoaded", String.class).invoke(loader, modId);
        } catch (ClassNotFoundException ignored) {
            // Not Fabric.
        } catch (ReflectiveOperationException e) {
            StynaSTMC.LOGGER.warn("Could not check for mod {}", modId, e);
            return false;
        }
        try {
            Class<?> fml = Class.forName("net.neoforged.fml.loading.FMLLoader");
            Object loader = fml.getMethod("getCurrent").invoke(null);
            Object mods = fml.getMethod("getLoadingModList").invoke(loader);
            return mods.getClass().getMethod("getModFileById", String.class).invoke(mods, modId) != null;
        } catch (ReflectiveOperationException e) {
            StynaSTMC.LOGGER.warn("Could not check for mod {}", modId, e);
            return false;
        }
    }
}
