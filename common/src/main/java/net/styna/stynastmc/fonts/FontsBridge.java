package net.styna.stynastmc.fonts;

import net.minecraft.client.Minecraft;

public final class FontsBridge {
    private FontsBridge() {
    }

    public static void onConfigLoading() {
        // Usually fires before the game exists; the first font reload then applies the saved font instead.
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.execute(FontRuntime::applyConfigured);
        }
    }
}
