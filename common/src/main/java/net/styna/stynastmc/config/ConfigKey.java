package net.styna.stynastmc.config;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.styna.stynastmc.StynaSTMC;

public final class ConfigKey {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(StynaSTMC.id("controls"));
    public static final KeyMapping OPEN = new KeyMapping("key.stynastmc.config", InputConstants.KEY_F9, CATEGORY);

    private ConfigKey() {
    }

    public static void tick(Minecraft minecraft) {
        boolean pressed = false;
        while (OPEN.consumeClick()) {
            pressed = true;
        }
        if (pressed && minecraft.gui.screen() == null) {
            minecraft.gui.setScreen(new StynaConfigScreen(null));
        }
    }
}
