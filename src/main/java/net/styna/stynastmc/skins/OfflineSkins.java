package net.styna.stynastmc.skins;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import net.styna.stynastmc.StynaSTMC;

/** Official Mojang skins for players on offline-mode servers, looked up by name. */
public final class OfflineSkins {
    private static final long RETRY_MS = 60_000;
    private static final int TIMEOUT = 10_000;

    public enum Status {
        PENDING, LOADED, NO_SKIN, NOT_FOUND, FAILED
    }

    private static final class Entry {
        Status status = Status.PENDING;
        SkinImages.Loaded image;
        SkinModel model = SkinModel.AUTO;
        long retryAt;
    }

    private static final Map<String, Entry> BY_NAME = new HashMap<>();
    private static final Map<UUID, Boolean> OFFLINE_IDS = new HashMap<>();
    // One request at a time keeps big servers under Mojang's rate limit.
    private static final ExecutorService HTTP = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "StynaSTMC offline skins");
        thread.setDaemon(true);
        return thread;
    });

    private static List<? extends String> cachedList;
    private static Set<String> selected = new HashSet<>();

    private OfflineSkins() {
    }

    public static boolean isOfflineProfile(GameProfile profile) {
        if (profile == null || profile.getId() == null || profile.getName() == null) {
            return false;
        }
        return OFFLINE_IDS.computeIfAbsent(profile.getId(), id -> id.equals(
                UUID.nameUUIDFromBytes(("OfflinePlayer:" + profile.getName()).getBytes(StandardCharsets.UTF_8))));
    }

    /** True when the server handed us an offline UUID, which only offline-mode servers do. */
    public static boolean serverIsOffline() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.getConnection() != null && minecraft.player != null && isOfflineProfile(minecraft.player.getGameProfile());
    }

    public static boolean isSelected(String name) {
        List<? extends String> list = SkinsConfig.OFFLINE_PLAYERS.get();
        if (list != cachedList) {
            Set<String> names = new HashSet<>();
            list.forEach(n -> names.add(n.toLowerCase(Locale.ROOT)));
            selected = names;
            cachedList = list;
        }
        return selected.contains(name.toLowerCase(Locale.ROOT));
    }

    private static boolean wanted(String name) {
        return SkinsConfig.loaded() && SkinsConfig.OFFLINE_ENABLED.get() && (SkinsConfig.OFFLINE_ALL.get() || isSelected(name));
    }

    private static Entry entry(UUID uuid) {
        NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection();
        NetworkPlayerInfo info = connection == null || uuid == null ? null : connection.getPlayerInfo(uuid);
        if (info == null || !isOfflineProfile(info.getGameProfile())) {
            return null;
        }
        String name = info.getGameProfile().getName();
        return wanted(name) ? request(name) : null;
    }

    public static ResourceLocation texture(UUID uuid) {
        Entry entry = entry(uuid);
        return entry == null || entry.image == null ? null : entry.image.texture;
    }

    public static String model(UUID uuid, String fallback) {
        Entry entry = entry(uuid);
        return entry == null || entry.image == null ? fallback : entry.model.resolve(fallback);
    }

    /** Lookup state for the tab, or null if nothing was requested for this name yet. */
    public static Status status(String name) {
        Entry entry = BY_NAME.get(name.toLowerCase(Locale.ROOT));
        return entry == null ? null : entry.status;
    }

    private static Entry request(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        Entry entry = BY_NAME.get(key);
        if (entry != null && (entry.status != Status.FAILED || System.currentTimeMillis() < entry.retryAt)) {
            return entry;
        }
        Entry fresh = entry == null ? new Entry() : entry;
        fresh.status = Status.PENDING;
        BY_NAME.put(key, fresh);
        if (!key.matches("[a-z0-9_]{1,16}")) {
            fresh.status = Status.NOT_FOUND;
            return fresh;
        }
        CompletableFuture.supplyAsync(() -> lookup(key), HTTP).whenComplete((result, error) -> Minecraft.getMinecraft().addScheduledTask(() -> {
            if (BY_NAME.get(key) != fresh) {
                return;
            }
            if (error != null) {
                fresh.status = Status.FAILED;
                fresh.retryAt = System.currentTimeMillis() + RETRY_MS;
                StynaSTMC.LOGGER.debug("Mojang skin lookup failed for {}: {}", key, SkinService.message(error));
                return;
            }
            if (result.png == null) {
                fresh.status = result.status;
                return;
            }
            try {
                fresh.image = SkinImages.load(result.png, "mojang/" + key);
                fresh.model = result.model;
                fresh.status = Status.LOADED;
            } catch (Exception e) {
                fresh.status = Status.FAILED;
                fresh.retryAt = System.currentTimeMillis() + RETRY_MS;
                StynaSTMC.LOGGER.warn("Bad Mojang skin for {}: {}", key, SkinService.message(e));
            }
        }));
        return fresh;
    }

    private static final class Result {
        final Status status;
        final byte[] png;
        final SkinModel model;

        Result(Status status, byte[] png, SkinModel model) {
            this.status = status;
            this.png = png;
            this.model = model;
        }
    }

    private static Result lookup(String name) {
        try {
            JsonObject profile = json("https://api.mojang.com/users/profiles/minecraft/" + name);
            if (profile == null) {
                return new Result(Status.NOT_FOUND, null, null);
            }
            JsonObject session = json("https://sessionserver.mojang.com/session/minecraft/profile/" + profile.get("id").getAsString());
            if (session == null || !session.has("properties")) {
                return new Result(Status.NOT_FOUND, null, null);
            }
            for (JsonElement property : session.getAsJsonArray("properties")) {
                JsonObject object = property.getAsJsonObject();
                if (!"textures".equals(object.get("name").getAsString())) {
                    continue;
                }
                String decoded = new String(Base64.getDecoder().decode(object.get("value").getAsString()), StandardCharsets.UTF_8);
                JsonObject textures = new JsonParser().parse(decoded).getAsJsonObject().getAsJsonObject("textures");
                JsonObject skin = textures == null ? null : textures.getAsJsonObject("SKIN");
                if (skin == null) {
                    break;
                }
                JsonObject metadata = skin.getAsJsonObject("metadata");
                boolean slim = metadata != null && metadata.has("model") && "slim".equals(metadata.get("model").getAsString());
                byte[] png = get(skin.get("url").getAsString().replaceFirst("^http://", "https://"));
                if (png == null) {
                    throw new IOException("Skin texture missing");
                }
                return new Result(Status.LOADED, png, slim ? SkinModel.SLIM : SkinModel.WIDE);
            }
            return new Result(Status.NO_SKIN, null, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonObject json(String url) throws IOException {
        byte[] body = get(url);
        return body == null ? null : new JsonParser().parse(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    /** Response body, or null for 204/404 (unknown name). */
    private static byte[] get(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);
        try {
            int status = connection.getResponseCode();
            if (status == 204 || status == 404) {
                return null;
            }
            if (status != 200) {
                throw new IOException("HTTP " + status + " from " + url);
            }
            try (InputStream in = connection.getInputStream()) {
                return SkinService.readAll(in);
            }
        } finally {
            connection.disconnect();
        }
    }
}
