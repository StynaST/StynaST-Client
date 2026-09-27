package net.styna.stynastmc;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.Identifier;

public final class StynaSTMC {
    public static final String MODID = "stynastmc";
    public static final Logger LOGGER = LogUtils.getLogger();

    private StynaSTMC() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
