package net.styna.stynastmc.skins;

import net.minecraft.client.Minecraft;

/** Drops cached textures when the skins config is edited outside the screen. */
public final class SkinsBridge {
    private SkinsBridge() {
    }

    public static void onConfigReloading() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.execute(() -> {
                CustomSkins.refresh();
                RemoteSkins.clear();
                SkinService.forgetToken();
            });
        }
    }
}
