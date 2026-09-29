package net.styna.stynastmc.skins;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.styna.stynastmc.StynaSTMC;

/** HD skins of other players, pulled from the skin service and cached per profile. */
public final class RemoteSkins {
    private static final long RETRY_MS = 5 * 60 * 1000;

    private static final Map<UUID, Loaded> SKINS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_ATTEMPT = new HashMap<>();
    private static final Set<UUID> IN_FLIGHT = new HashSet<>();

    private static long nextQuery;

    private RemoteSkins() {
    }

    private record Loaded(SkinImages.Loaded image, SkinModel model, String hash) {
    }

    private record RemoteTexture(Identifier texturePath) implements ClientAsset.Texture {
        @Override
        public Identifier id() {
            return this.texturePath;
        }
    }

    public static boolean enabled() {
        return SkinsConfig.loaded() && SkinService.configured();
    }

    public static PlayerSkin apply(UUID uuid, PlayerSkin base) {
        if (!enabled()) {
            return base;
        }
        Loaded loaded = SKINS.get(uuid);
        if (loaded == null) {
            request(uuid);
            return base;
        }
        return PlayerSkin.insecure(new RemoteTexture(loaded.image().texture()), base.cape(), base.elytra(),
                loaded.model().resolve(base.model()));
    }

    private static void request(UUID uuid) {
        long now = System.currentTimeMillis();
        if (IN_FLIGHT.contains(uuid) || now < NEXT_ATTEMPT.getOrDefault(uuid, 0L)) {
            return;
        }
        IN_FLIGHT.add(uuid);
        NEXT_ATTEMPT.put(uuid, now + RETRY_MS);

        // One query covers everyone online, so a single new face warms the whole server.
        List<UUID> wanted = now >= nextQuery ? SkinService.onlinePlayers() : List.of(uuid);
        nextQuery = now + 30_000;
        if (!wanted.contains(uuid)) {
            wanted = List.of(uuid);
        }

        List<UUID> queried = wanted;
        SkinService.query(queried).whenComplete((entries, error) -> Minecraft.getInstance().execute(() -> {
            IN_FLIGHT.remove(uuid);
            if (error != null) {
                StynaSTMC.LOGGER.debug("Skin query failed: {}", SkinService.message(error));
                return;
            }
            for (UUID candidate : queried) {
                SkinService.Entry entry = entries.get(candidate);
                if (entry == null) {
                    continue;
                }
                Loaded cached = SKINS.get(candidate);
                if (cached != null && cached.hash().equals(entry.hash())) {
                    continue;
                }
                fetch(entry);
            }
        }));
    }

    private static void fetch(SkinService.Entry entry) {
        UUID uuid = entry.uuid();
        if (!IN_FLIGHT.add(uuid)) {
            return;
        }
        SkinService.download(uuid).whenComplete((png, error) -> Minecraft.getInstance().execute(() -> {
            IN_FLIGHT.remove(uuid);
            if (error != null) {
                StynaSTMC.LOGGER.debug("Skin download failed for {}: {}", uuid, SkinService.message(error));
                return;
            }
            try {
                SkinImages.Loaded image = SkinImages.load(png, "remote/" + uuid);
                Loaded previous = SKINS.put(uuid, new Loaded(image, entry.model(), entry.hash()));
                if (previous != null) {
                    SkinImages.release(previous.image().texture());
                }
                NEXT_ATTEMPT.remove(uuid);
            } catch (Exception e) {
                StynaSTMC.LOGGER.warn("Bad remote skin for {}: {}", uuid, SkinService.message(e));
            }
        }));
    }

    public static void clear() {
        SKINS.values().forEach(loaded -> SkinImages.release(loaded.image().texture()));
        SKINS.clear();
        NEXT_ATTEMPT.clear();
        IN_FLIGHT.clear();
        nextQuery = 0;
    }
}
