package net.styna.stynastmc.capes;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.styna.stynastmc.StynaSTMC;

/** Capes owned by the signed in account, read and equipped through the Minecraft services API like the launcher does. */
public final class MojangCapes {
    private static final String PROFILE = "https://api.minecraftservices.com/minecraft/profile";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private static List<AccountCape> capes;
    private static String error;
    private static boolean loading;
    private static final Map<String, CapeTexture> TEXTURES = new HashMap<>();
    private static final Map<String, String> TEXTURE_ERRORS = new HashMap<>();
    // Set once a cape is equipped here, since the session's own profile keeps the old cape until rejoining.
    private static AccountCape equipped;
    private static boolean equipChanged;

    private MojangCapes() {
    }

    public record AccountCape(String id, String alias, String url, boolean active) {
    }

    public static List<AccountCape> capes() {
        if (capes == null && !loading && error == null) {
            refresh();
        }
        return capes;
    }

    public static boolean loading() {
        return loading;
    }

    public static String error() {
        return error;
    }

    public static void refresh() {
        loading = true;
        error = null;
        send(HttpRequest.newBuilder(URI.create(PROFILE)).GET()).thenApply(MojangCapes::parse)
                .whenComplete((list, failure) -> Minecraft.getInstance().execute(() -> {
                    loading = false;
                    if (failure != null) {
                        error = message(failure);
                    } else {
                        capes = list;
                    }
                }));
    }

    /** Equips the cape on the account for everyone, or hides it when null. */
    public static CompletableFuture<Void> equip(AccountCape cape) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(PROFILE + "/capes/active"));
        if (cape == null) {
            request.DELETE();
        } else {
            JsonObject body = new JsonObject();
            body.addProperty("capeId", cape.id());
            request.header("Content-Type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(body.toString()));
        }
        CompletableFuture<Void> result = new CompletableFuture<>();
        send(request).thenApply(MojangCapes::parse).whenComplete((list, failure) -> Minecraft.getInstance().execute(() -> {
            if (failure != null) {
                result.completeExceptionally(failure);
                return;
            }
            capes = list;
            equipped = cape;
            equipChanged = true;
            result.complete(null);
        }));
        return result;
    }

    public static AccountCape active() {
        if (capes == null) {
            return null;
        }
        return capes.stream().filter(AccountCape::active).findFirst().orElse(null);
    }

    /** True once the own Mojang cape was changed this session. */
    static boolean overridden() {
        return equipChanged;
    }

    static CapeTexture overrideTexture() {
        return equipped == null ? null : texture(equipped);
    }

    public static CapeTexture texture(AccountCape cape) {
        CapeTexture loaded = TEXTURES.get(cape.id());
        if (TEXTURES.containsKey(cape.id()) || TEXTURE_ERRORS.containsKey(cape.id())) {
            return loaded;
        }
        TEXTURES.put(cape.id(), null);
        CapeProvider.HTTP.sendAsync(HttpRequest.newBuilder(URI.create(cape.url())).timeout(TIMEOUT).GET().build(),
                        HttpResponse.BodyHandlers.ofByteArray())
                .whenComplete((response, failure) -> Minecraft.getInstance().execute(() -> {
                    try {
                        if (failure != null) {
                            throw failure;
                        }
                        if (response.statusCode() / 100 != 2) {
                            throw new IllegalStateException("HTTP " + response.statusCode());
                        }
                        TEXTURES.put(cape.id(), CapeTexture.load(response.body(), false, "mojang/" + cape.alias()));
                    } catch (Throwable e) {
                        TEXTURES.remove(cape.id());
                        TEXTURE_ERRORS.put(cape.id(), message(e));
                        StynaSTMC.LOGGER.warn("Could not load Mojang cape {}: {}", cape.alias(), message(e));
                    }
                }));
        return null;
    }

    public static void clear() {
        TEXTURES.values().stream().filter(Objects::nonNull).forEach(CapeTexture::release);
        TEXTURES.clear();
        TEXTURE_ERRORS.clear();
        capes = null;
        error = null;
        equipped = null;
        equipChanged = false;
    }

    private static CompletableFuture<String> send(HttpRequest.Builder request) {
        String token = Minecraft.getInstance().getUser().getAccessToken();
        return CapeProvider.HTTP.sendAsync(request.timeout(TIMEOUT).header("Authorization", "Bearer " + token).build(),
                HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
                    if (response.statusCode() == 401) {
                        throw new IllegalStateException("Not signed in to a Microsoft account");
                    }
                    if (response.statusCode() / 100 != 2) {
                        throw new IllegalStateException("HTTP " + response.statusCode());
                    }
                    return response.body();
                });
    }

    private static List<AccountCape> parse(String body) {
        List<AccountCape> list = new ArrayList<>();
        JsonObject profile = JsonParser.parseString(body).getAsJsonObject();
        if (profile.get("capes") != null && profile.get("capes").isJsonArray()) {
            for (JsonElement element : profile.getAsJsonArray("capes")) {
                JsonObject cape = element.getAsJsonObject();
                list.add(new AccountCape(cape.get("id").getAsString(), cape.get("alias").getAsString(), cape.get("url").getAsString(),
                        "ACTIVE".equals(cape.get("state").getAsString())));
            }
        }
        return List.copyOf(list);
    }

    static String message(Throwable error) {
        while (error.getCause() != null && (error instanceof CompletionException || error.getMessage() == null)) {
            error = error.getCause();
        }
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
