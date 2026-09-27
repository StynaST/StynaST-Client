package net.styna.stynastmc.tweaks;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

/** Rebindable key for vanilla's hardcoded F3 + Esc pause without menu. */
public final class DebugPauseKey {
    public static final KeyMapping KEY = new KeyMapping("key.stynastmc.debug.pause", InputConstants.KEY_ESCAPE, KeyMapping.Category.DEBUG);

    private DebugPauseKey() {
    }
}
