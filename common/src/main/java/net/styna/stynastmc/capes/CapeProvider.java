package net.styna.stynastmc.capes;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;

import net.minecraft.util.Util;
import net.styna.stynastmc.platform.Platform;

/** Where capes come from. Mojang is the vanilla cape; the rest are third party cape sites, looked up per player. */
public enum CapeProvider {
    MOJANG("mojang", "Mojang", "https://www.minecraft.net/msaprofile/mygames/editprofile"),
    OPTIFINE("optifine", "OptiFine", "https://optifine.net/home"),
    MINECRAFTCAPES("minecraftcapes", "MinecraftCapes", "https://minecraftcapes.net"),
    COSMETICA("cosmetica", "Cosmetica", "https://cosmetica.cc"),
    LABYMOD("labymod", "LabyMod", "https://labymod.net");

    static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_BYTES = 10_000_000;

    private final String id;
    private final String title;
    private final URI homepage;
    // Keeps a server full of players from flooding one site.
    private final Semaphore slots = new Semaphore(4);

    CapeProvider(String id, String title, String homepage) {
        this.id = id;
        this.title = title;
        this.homepage = URI.create(homepage);
    }

    /** Raw cape PNG; animated means frames stacked vertically. */
    public record Fetched(byte[] png, boolean animated) {
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public URI homepage() {
        return homepage;
    }

    public static CapeProvider byId(String id) {
        for (CapeProvider provider : values()) {
            if (provider.id.equals(id.trim().toLowerCase(Locale.ROOT))) {
                return provider;
            }
        }
        return null;
    }

    public static List<String> ids() {
        return Arrays.stream(values()).map(CapeProvider::id).toList();
    }

    /** Null result when the player has no cape there. Mojang capes come from the profile and are never fetched here. */
    public CompletableFuture<Fetched> fetch(GameProfile profile) {
        return switch (this) {
            case MOJANG -> CompletableFuture.completedFuture(null);
            case OPTIFINE -> profile.name().isBlank()
                    ? CompletableFuture.completedFuture(null)
                    : limited(() -> png("http://s.optifine.net/capes/" + profile.name() + ".png", false));
            case MINECRAFTCAPES -> limited(() -> json("https://api.minecraftcapes.net/profile/" + profile.id().toString().replace("-", ""))
                    .thenCompose(json -> {
                        String url = string(json, "cape_url");
                        boolean animated = json != null && json.has("animatedCape") && json.get("animatedCape").getAsBoolean();
                        return url == null ? CompletableFuture.completedFuture(null) : png(url, animated);
                    }));
            case COSMETICA -> limited(() -> json("https://api.cloaks.gg/users/" + profile.id() + "/cape").thenCompose(json -> {
                JsonObject cloak = json != null && json.get("cloak") instanceof JsonObject object ? object : null;
                String url = string(cloak, "texture");
                boolean animated = cloak != null && cloak.has("frames") && cloak.get("frames").getAsInt() > 1;
                return url == null ? CompletableFuture.completedFuture(null) : png(url, animated);
            }));
            case LABYMOD -> limited(() -> png("https://dl.labymod.net/capes/" + profile.id(), false));
        };
    }

    private <T> CompletableFuture<T> limited(Supplier<CompletableFuture<T>> request) {
        return CompletableFuture.runAsync(slots::acquireUninterruptibly, Util.ioPool())
                .thenCompose(unused -> request.get())
                .whenComplete((result, error) -> slots.release());
    }

    private static HttpRequest.Builder request(String url) {
        return HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT)
                .header("User-Agent", "StynaST-MC/" + Platform.INSTANCE.modVersion());
    }

    private static CompletableFuture<JsonObject> json(String url) {
        return HTTP.sendAsync(request(url).GET().build(), HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() == 404) {
                return null;
            }
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("HTTP " + response.statusCode() + " from " + url);
            }
            return JsonParser.parseString(response.body()) instanceof JsonObject object ? object : null;
        });
    }

    private static CompletableFuture<Fetched> png(String url, boolean animated) {
        return HTTP.sendAsync(request(url).GET().build(), HttpResponse.BodyHandlers.ofByteArray()).thenApply(response -> {
            if (response.statusCode() == 404) {
                return null;
            }
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("HTTP " + response.statusCode() + " from " + url);
            }
            if (response.body().length > MAX_BYTES) {
                throw new IllegalStateException(new IOException("Cape larger than " + MAX_BYTES + " bytes"));
            }
            return new Fetched(response.body(), animated);
        });
    }

    private static String string(JsonObject json, String key) {
        JsonElement value = json == null ? null : json.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }
}
