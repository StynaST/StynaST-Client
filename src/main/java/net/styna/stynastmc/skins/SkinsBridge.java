package net.styna.stynastmc.skins;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/** Glue between the skin system and the rest of the game. */
public final class SkinsBridge {
    private SkinsBridge() {
    }

    public static void onConfigReloading() {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            CustomSkins.refresh();
            RemoteSkins.clear();
            SkinService.forgetToken();
        });
    }

    private static boolean isSelf(UUID uuid) {
        Minecraft minecraft = Minecraft.getMinecraft();
        UUID self = minecraft.player != null ? minecraft.player.getUniqueID() : minecraft.getSession().getProfile().getId();
        return uuid.equals(self);
    }

    /** Our skin texture for whoever owns this UUID, or null to keep vanilla's. */
    public static ResourceLocation texture(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        if (isSelf(uuid)) {
            ResourceLocation local = CustomSkins.texture();
            if (local != null) {
                return local;
            }
            if (!CustomSkins.enabled()) {
                return OfflineSkins.texture(uuid);
            }
        }
        RemoteSkins.Loaded remote = RemoteSkins.get(uuid);
        return remote == null ? OfflineSkins.texture(uuid) : remote.image.texture;
    }

    /** Skin type ("default" or "slim") to pair with {@link #texture}. */
    public static String model(UUID uuid, String fallback) {
        if (uuid == null) {
            return fallback;
        }
        if (isSelf(uuid)) {
            if (CustomSkins.texture() != null) {
                return SkinsConfig.MODEL.get().resolve(fallback);
            }
            if (!CustomSkins.enabled()) {
                return OfflineSkins.model(uuid, fallback);
            }
        }
        RemoteSkins.Loaded remote = RemoteSkins.get(uuid);
        return remote == null ? OfflineSkins.model(uuid, fallback) : remote.model.resolve(fallback);
    }
}
