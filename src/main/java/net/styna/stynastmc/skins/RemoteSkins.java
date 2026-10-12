package net.styna.stynastmc.skins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.styna.stynastmc.StynaSTMC;

/** HD skins of other players, pulled from the skin service and cached per profile. */
public final class RemoteSkins {
    private static final long REFRESH_MS = 5_000;
    private static final long RETRY_MS = 30_000;
    private static final int QUERY_LIMIT = 200;

    private static final Map<UUID, Loaded> SKINS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_ATTEMPT = new HashMap<>();
    private static final Set<UUID> IN_FLIGHT = new HashSet<>();

    private static long nextQuery;
    private static int generation;

    private RemoteSkins() {
    }

    public static final class Loaded {
        public final SkinImages.Loaded image;
        public final SkinModel model;
        final String hash;

        Loaded(SkinImages.Loaded image, SkinModel model, String hash) {
            this.image = image;
            this.model = model;
            this.hash = hash;
        }
    }

    public static boolean enabled() {
        return SkinsConfig.loaded() && SkinService.configured();
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        if (!enabled() || now < nextQuery) {
            return;
        }
        nextQuery = now + REFRESH_MS;
        request(SkinService.onlinePlayers());
    }

    /** The service skin for this player, or null to keep the vanilla one. */
    public static Loaded get(UUID uuid) {
        if (!enabled()) {
            return null;
        }
        Long next = NEXT_ATTEMPT.get(uuid);
        if (!IN_FLIGHT.contains(uuid) && System.currentTimeMillis() >= (next == null ? 0L : next)) {
            request(Collections.singletonList(uuid));
        }
        return SKINS.get(uuid);
    }

    private static void request(List<UUID> wanted) {
        long now = System.currentTimeMillis();
        List<UUID> due = new ArrayList<>();
        for (UUID uuid : new LinkedHashSet<>(wanted)) {
            Long next = NEXT_ATTEMPT.get(uuid);
            if (!IN_FLIGHT.contains(uuid) && now >= (next == null ? 0L : next)) {
                due.add(uuid);
            }
        }
        for (int start = 0; start < due.size(); start += QUERY_LIMIT) {
            query(new ArrayList<>(due.subList(start, Math.min(start + QUERY_LIMIT, due.size()))), now);
        }
    }

    private static void query(List<UUID> queried, long now) {
        int requestedGeneration = generation;
        IN_FLIGHT.addAll(queried);
        queried.forEach(uuid -> NEXT_ATTEMPT.put(uuid, now + REFRESH_MS));
        SkinService.query(queried).whenComplete((entries, error) -> Minecraft.getMinecraft().addScheduledTask(() -> {
            if (requestedGeneration != generation) {
                return;
            }
            IN_FLIGHT.removeAll(queried);
            if (error != null) {
                queried.forEach(uuid -> NEXT_ATTEMPT.put(uuid, System.currentTimeMillis() + RETRY_MS));
                StynaSTMC.LOGGER.debug("Skin query failed: {}", SkinService.message(error));
                return;
            }
            for (UUID candidate : queried) {
                SkinService.Entry entry = entries.get(candidate);
                Loaded cached = SKINS.get(candidate);
                if (entry == null) {
                    remove(candidate);
                } else if (cached != null && cached.hash.equals(entry.hash)) {
                    SKINS.put(candidate, new Loaded(cached.image, entry.model, entry.hash));
                } else {
                    fetch(entry, requestedGeneration);
                }
            }
        }));
    }

    private static void fetch(SkinService.Entry entry, int requestedGeneration) {
        UUID uuid = entry.uuid;
        if (!IN_FLIGHT.add(uuid)) {
            return;
        }
        SkinService.download(uuid).whenComplete((png, error) -> Minecraft.getMinecraft().addScheduledTask(() -> {
            if (requestedGeneration != generation) {
                return;
            }
            IN_FLIGHT.remove(uuid);
            if (error != null) {
                NEXT_ATTEMPT.put(uuid, System.currentTimeMillis() + RETRY_MS);
                StynaSTMC.LOGGER.debug("Skin download failed for {}: {}", uuid, SkinService.message(error));
                return;
            }
            try {
                SkinImages.Loaded image = SkinImages.load(png, "remote/" + uuid);
                Loaded previous = SKINS.put(uuid, new Loaded(image, entry.model, entry.hash));
                if (previous != null) {
                    SkinImages.release(previous.image.texture);
                }
                NEXT_ATTEMPT.put(uuid, System.currentTimeMillis() + REFRESH_MS);
            } catch (Exception e) {
                NEXT_ATTEMPT.put(uuid, System.currentTimeMillis() + RETRY_MS);
                StynaSTMC.LOGGER.warn("Bad remote skin for {}: {}", uuid, SkinService.message(e));
            }
        }));
    }

    private static void remove(UUID uuid) {
        Loaded previous = SKINS.remove(uuid);
        if (previous != null) {
            SkinImages.release(previous.image.texture);
        }
    }

    public static void clear() {
        generation++;
        SKINS.values().forEach(loaded -> SkinImages.release(loaded.image.texture));
        SKINS.clear();
        NEXT_ATTEMPT.clear();
        IN_FLIGHT.clear();
        nextQuery = 0;
    }
}
