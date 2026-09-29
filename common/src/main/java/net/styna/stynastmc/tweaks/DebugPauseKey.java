package net.styna.stynastmc.tweaks;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class DebugPauseKey {
    public static final KeyMapping KEY = new KeyMapping("key.stynastmc.debug.pause", InputConstants.KEY_ESCAPE, KeyMapping.Category.DEBUG);

    private DebugPauseKey() {
    }

    public static void tick(Minecraft minecraft) {
        boolean pressed = false;
        while (KEY.consumeClick()) {
            pressed = true;
        }
        if (pressed && !KEY.isDefault() && minecraft.gui.screen() == null) {
            minecraft.pauseGame(true);
        }
    }
}
