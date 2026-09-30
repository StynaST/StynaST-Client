package net.styna.stynastmc.skins;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerSkin;

/** Glue between the skin system and the rest of the game. */
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

    /** Our skin for whoever owns this UUID: the local selection for ourselves, the skin service for others. */
    public static PlayerSkin forPlayer(UUID uuid, PlayerSkin base) {
        Minecraft minecraft = Minecraft.getInstance();
        UUID self = minecraft.player != null ? minecraft.player.getUUID() : minecraft.getUser().getProfileId();
        return uuid.equals(self) ? CustomSkins.apply(uuid, base) : RemoteSkins.apply(uuid, base);
    }
}
