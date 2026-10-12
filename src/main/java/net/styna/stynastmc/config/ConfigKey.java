package net.styna.stynastmc.config;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

public final class ConfigKey {
    public static final String CATEGORY = "key.category.stynastmc.controls";
    public static final KeyBinding OPEN = new KeyBinding("key.stynastmc.config", Keyboard.KEY_F9, CATEGORY);

    private ConfigKey() {
    }

    public static void tick(Minecraft minecraft) {
        boolean pressed = false;
        while (OPEN.isPressed()) {
            pressed = true;
        }
        if (pressed && minecraft.currentScreen == null) {
            minecraft.displayGuiScreen(new StynaConfigScreen(null));
        }
    }
}
