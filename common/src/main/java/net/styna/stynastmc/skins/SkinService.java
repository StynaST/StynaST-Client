package net.styna.stynastmc.skins;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.InvalidCredentialsException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.styna.stynastmc.StynaSTMC;

/**
 * Talks to the skin service on the website. Access is a bearer token traded for a Mojang joinServer
 * handshake, so the endpoints answer the mod and not a browser.
 */
public final class SkinService {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final long TOKEN_MARGIN_MS = 60_000;
    private static final long SIGN_IN_RETRY_MS = 5 * 60 * 1000;

    private static CompletableFuture<String> tokenRequest;
    private static String token;
    private static long tokenExpiresAt;
    private static Throwable signInFailure;
    private static long signInRetryAt;

    private SkinService() {
    }

    /** What the service knows about one player's shared skin. */
    public record Entry(UUID uuid, String hash, SkinModel model) {
    }

    public static boolean configured() {
        return !baseUrl().isEmpty();
    }

    private static String baseUrl() {
        return SkinsConfig.loaded() ? SkinsConfig.SERVICE_URL.get().trim().replaceAll("/+$", "") : "";
    }

    private static URI uri(String path) {
        return URI.create(baseUrl() + path);
    }

    public static synchronized void forgetToken() {
        token = null;
        tokenExpiresAt = 0;
        tokenRequest = null;
        signInFailure = null;
        signInRetryAt = 0;
    }

    private static synchronized CompletableFuture<String> token() {
        if (token != null && System.currentTimeMillis() < tokenExpiresAt - TOKEN_MARGIN_MS) {
            return CompletableFuture.completedFuture(token);
        }
        if (tokenRequest != null) {
            return tokenRequest;
        }
        // A failed sign-in stays failed for a while, so every player on screen does not re-ask Mojang.
        if (signInFailure != null && System.currentTimeMillis() < signInRetryAt) {
            return CompletableFuture.failedFuture(signInFailure);
        }
        tokenRequest = CompletableFuture.supplyAsync(SkinService::authenticate).whenComplete((result, error) -> {
            synchronized (SkinService.class) {
                tokenRequest = null;
                signInFailure = error;
                signInRetryAt = error == null ? 0 : System.currentTimeMillis() + SIGN_IN_RETRY_MS;
            }
        });
        return tokenRequest;
    }

    private static String authenticate() {
        if (!configured()) {
            throw new CompletionException(new IOException("No skin service configured"));
        }
        User user = Minecraft.getInstance().getUser();
        JsonObject body = new JsonObject();
        body.addProperty("username", user.getName());

        String devToken = SkinsConfig.DEV_TOKEN.get().trim();
        if (devToken.isEmpty()) {
            byte[] random = new byte[16];
            RANDOM.nextBytes(random);
            String serverId = HexFormat.of().formatHex(random);
            try {
                Minecraft.getInstance().services().sessionService().joinServer(user.getProfileId(), user.getAccessToken(), serverId);
            } catch (InvalidCredentialsException e) {
                throw new CompletionException(new IOException("Not signed in to a Mojang account (offline or dev session)", e));
            } catch (Exception e) {
                throw new CompletionException(new IOException("Mojang session check failed: " + message(e), e));
            }
            body.addProperty("serverId", serverId);
        } else {
            body.addProperty("devToken", devToken);
            body.addProperty("uuid", user.getProfileId().toString());
        }

        HttpRequest request = HttpRequest.newBuilder(uri("/api/minecraft/skins/session"))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new CompletionException(new IOException("Sign in failed: " + errorOf(response)));
        }
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        synchronized (SkinService.class) {
            token = json.get("token").getAsString();
            tokenExpiresAt = json.get("expiresAt").getAsLong();
            return token;
        }
    }

    public static CompletableFuture<Void> upload(Path file, SkinModel model) {
        return token().thenApplyAsync(bearer -> {
            byte[] png;
            try {
                png = Files.readAllBytes(file);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
            HttpRequest request = HttpRequest.newBuilder(uri("/api/minecraft/skins/self?model=" + model.name().toLowerCase(Locale.ROOT)))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + bearer)
                    .header("Content-Type", "image/png")
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(png))
                    .build();
            HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                forgetToken();
            }
            if (response.statusCode() != 200) {
                throw new CompletionException(new IOException("Upload failed: " + errorOf(response)));
            }
            return null;
        });
    }

    public static CompletableFuture<Void> clear() {
        return token().thenApplyAsync(bearer -> {
            HttpRequest request = HttpRequest.newBuilder(uri("/api/minecraft/skins/self"))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + bearer)
                    .DELETE()
                    .build();
            HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new CompletionException(new IOException("Removal failed: " + errorOf(response)));
            }
            return null;
        });
    }

    public static CompletableFuture<Map<UUID, Entry>> query(List<UUID> uuids) {
        return token().thenApplyAsync(bearer -> {
            JsonObject body = new JsonObject();
            com.google.gson.JsonArray array = new com.google.gson.JsonArray();
            uuids.forEach(uuid -> array.add(uuid.toString()));
            body.add("uuids", array);

            HttpRequest request = HttpRequest.newBuilder(uri("/api/minecraft/skins/query"))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + bearer)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                forgetToken();
            }
            if (response.statusCode() != 200) {
                throw new CompletionException(new IOException("Query failed: " + errorOf(response)));
            }

            Map<UUID, Entry> out = new HashMap<>();
            for (var element : JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("skins")) {
                JsonObject skin = element.getAsJsonObject();
                UUID uuid = UUID.fromString(skin.get("uuid").getAsString());
                out.put(uuid, new Entry(uuid, skin.get("hash").getAsString(), model(skin.get("model").getAsString())));
            }
            return out;
        });
    }

    public static CompletableFuture<byte[]> download(UUID uuid) {
        return token().thenApplyAsync(bearer -> {
            HttpRequest request = HttpRequest.newBuilder(uri("/api/minecraft/skins/" + uuid + ".png"))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + bearer)
                    .GET()
                    .build();
            HttpResponse<byte[]> response = send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 401) {
                forgetToken();
            }
            if (response.statusCode() != 200) {
                throw new CompletionException(new IOException("Download failed: HTTP " + response.statusCode()));
            }
            return response.body();
        });
    }

    /** Uuids of everyone in the player list, so one query covers the whole server. */
    public static List<UUID> onlinePlayers() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return List.of();
        }
        List<UUID> uuids = new ArrayList<>();
        minecraft.getConnection().getOnlinePlayers().forEach(info -> uuids.add(info.getProfile().id()));
        return uuids;
    }

    private static SkinModel model(String name) {
        try {
            return SkinModel.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SkinModel.AUTO;
        }
    }

    private static <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        try {
            return HTTP.send(request, handler);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new CompletionException(e);
        }
    }

    private static String errorOf(HttpResponse<String> response) {
        try {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("error")) {
                return json.get("error").getAsString();
            }
        } catch (RuntimeException ignored) {
            // Not a JSON error body, the status code is all we have.
        }
        return "HTTP " + response.statusCode();
    }

    public static String message(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            message = cause.getClass().getSimpleName();
        }
        StynaSTMC.LOGGER.debug("Skin service error", error);
        return message;
    }
}
