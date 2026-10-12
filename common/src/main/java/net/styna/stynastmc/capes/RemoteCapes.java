package net.styna.stynastmc.capes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.Minecraft;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.skins.SkinService;
import net.styna.stynastmc.skins.SkinsConfig;

/** Capes shared through the skin service: pulls other players' capes and keeps our custom cape uploaded. */
public final class RemoteCapes {
    private static final long REFRESH_MS = 5_000;
    private static final long RETRY_MS = 30_000;
    private static final int QUERY_LIMIT = 200;

    private static final Map<UUID, Loaded> CAPES = new HashMap<>();
    private static final Map<UUID, Long> NEXT_ATTEMPT = new HashMap<>();
    private static final Set<UUID> IN_FLIGHT = new HashSet<>();

    private static long nextQuery;
    private static int generation;

    private static String shared;
    private static String uploading;
    private static long nextShare;
    private static String shareError;

    private RemoteCapes() {
    }

    private record Loaded(CapeTexture texture, String hash) {
    }

    public static boolean enabled() {
        return SkinsConfig.loaded() && SkinService.configured();
    }

    public static CapeTexture texture(UUID uuid) {
        if (enabled() && !IN_FLIGHT.contains(uuid) && System.currentTimeMillis() >= NEXT_ATTEMPT.getOrDefault(uuid, 0L)) {
            request(List.of(uuid));
        }
        Loaded loaded = CAPES.get(uuid);
        return loaded == null ? null : loaded.texture();
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        if (SkinService.configured() && uploading == null && now >= nextShare) {
            share();
        }
        if (!enabled() || now < nextQuery) {
            return;
        }
        nextQuery = now + REFRESH_MS;
        List<UUID> wanted = new ArrayList<>(SkinService.onlinePlayers());
        wanted.add(Minecraft.getInstance().getUser().getProfileId());
        request(wanted);
    }

    private static void request(List<UUID> wanted) {
        if (!SkinService.configured()) {
            return;
        }
        long now = System.currentTimeMillis();
        List<UUID> due = wanted.stream().distinct()
                .filter(uuid -> !IN_FLIGHT.contains(uuid) && now >= NEXT_ATTEMPT.getOrDefault(uuid, 0L)).toList();
        for (int start = 0; start < due.size(); start += QUERY_LIMIT) {
            query(due.subList(start, Math.min(start + QUERY_LIMIT, due.size())), now);
        }
    }

    private static void query(List<UUID> queried, long now) {
        int requestedGeneration = generation;
        IN_FLIGHT.addAll(queried);
        queried.forEach(uuid -> NEXT_ATTEMPT.put(uuid, now + REFRESH_MS));
        SkinService.queryCapes(queried).whenComplete((entries, failure) -> Minecraft.getInstance().execute(() -> {
            if (requestedGeneration != generation) {
                return;
            }
            IN_FLIGHT.removeAll(queried);
            if (failure != null) {
                StynaSTMC.LOGGER.debug("Cape query failed: {}", SkinService.message(failure));
                queried.forEach(uuid -> NEXT_ATTEMPT.put(uuid, System.currentTimeMillis() + RETRY_MS));
                return;
            }
            for (UUID candidate : queried) {
                SkinService.CapeEntry entry = entries.get(candidate);
                Loaded cached = CAPES.get(candidate);
                if (entry == null) {
                    remove(candidate);
                } else if (cached == null || !cached.hash().equals(entry.hash())) {
                    fetch(entry, requestedGeneration);
                }
            }
        }));
    }

    private static void fetch(SkinService.CapeEntry entry, int requestedGeneration) {
        UUID uuid = entry.uuid();
        if (!IN_FLIGHT.add(uuid)) {
            return;
        }
        SkinService.downloadCape(uuid).whenComplete((png, failure) -> Minecraft.getInstance().execute(() -> {
            if (requestedGeneration != generation) {
                return;
            }
            IN_FLIGHT.remove(uuid);
            if (failure != null) {
                NEXT_ATTEMPT.put(uuid, System.currentTimeMillis() + RETRY_MS);
                StynaSTMC.LOGGER.debug("Cape download failed for {}: {}", uuid, SkinService.message(failure));
                return;
            }
            try {
                CapeTexture texture = CapeTexture.load(png, entry.frames() > 1, "styna/" + uuid);
                Loaded previous = CAPES.put(uuid, new Loaded(texture, entry.hash()));
                if (previous != null) {
                    previous.texture().release();
                }
            } catch (Exception e) {
                NEXT_ATTEMPT.put(uuid, System.currentTimeMillis() + RETRY_MS);
                StynaSTMC.LOGGER.warn("Bad shared cape for {}: {}", uuid, SkinService.message(e));
            }
        }));
    }

    private static void remove(UUID uuid) {
        Loaded previous = CAPES.remove(uuid);
        if (previous != null) {
            previous.texture().release();
        }
    }

    /** Uploads the custom cape while one is worn, removes the shared copy otherwise. Retries failures every 30s. */
    public static CompletableFuture<Void> share() {
        Path file = Capes.ownCustomFile();
        String wanted;
        byte[] png = null;
        try {
            wanted = file == null ? "" : file + "|" + Files.getLastModifiedTime(file).toMillis() + "|" + Files.size(file);
            if (wanted.equals(shared) || wanted.equals(uploading)) {
                return CompletableFuture.completedFuture(null);
            }
            if (file != null) {
                boolean animated = file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".anim.png");
                png = CapeTexture.normalized(Files.readAllBytes(file), animated);
            }
        } catch (IOException | RuntimeException e) {
            shareError = SkinService.message(e);
            nextShare = System.currentTimeMillis() + RETRY_MS;
            return CompletableFuture.failedFuture(e);
        }
        String target = wanted;
        uploading = target;
        CompletableFuture<Void> upload = png == null ? SkinService.clearCape() : SkinService.uploadCape(png);
        upload.whenComplete((unused, failure) -> Minecraft.getInstance().execute(() -> {
            if (!target.equals(uploading)) {
                return;
            }
            uploading = null;
            if (failure == null) {
                shared = target;
                shareError = null;
                nextShare = System.currentTimeMillis() + REFRESH_MS;
                nextQuery = 0;
                NEXT_ATTEMPT.remove(Minecraft.getInstance().getUser().getProfileId());
            } else {
                shareError = SkinService.message(failure);
                nextShare = System.currentTimeMillis() + RETRY_MS;
                StynaSTMC.LOGGER.debug("Cape sharing failed: {}", shareError);
            }
        }));
        return upload;
    }

    public static String shareError() {
        return shareError;
    }

    /** Next tick re-checks the own cape against what was last shared. */
    public static void shareSoon() {
        nextShare = 0;
    }

    public static void clear() {
        generation++;
        CAPES.values().forEach(loaded -> loaded.texture().release());
        CAPES.clear();
        NEXT_ATTEMPT.clear();
        IN_FLIGHT.clear();
        nextQuery = 0;
    }

    /** Service URL or account changed: forget what was shared there. */
    public static void reset() {
        clear();
        shared = null;
        uploading = null;
        nextShare = 0;
    }
}
