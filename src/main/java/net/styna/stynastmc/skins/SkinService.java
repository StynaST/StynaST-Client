package net.styna.stynastmc.skins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.authlib.exceptions.InvalidCredentialsException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.Session;
import net.styna.stynastmc.StynaSTMC;

/**
 * Talks to the skin service on the website. Access is a bearer token traded for a Mojang joinServer
 * handshake, so the endpoints answer the mod and not a browser.
 */
public final class SkinService {
    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int TIMEOUT = 20_000;
    private static final long TOKEN_MARGIN_MS = 60_000;
    private static final long SIGN_IN_RETRY_MS = 5 * 60 * 1000;
    // Cloudflare rejects Java 8's default "Java/1.8" agent with error 1010.
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36";

    private static CompletableFuture<String> tokenRequest;
    private static String token;
    private static long tokenExpiresAt;
    private static Throwable signInFailure;
    private static long signInRetryAt;
    private static CompletableFuture<Void> skinUpdate = CompletableFuture.completedFuture(null);

    private SkinService() {
    }

    public static final class Entry {
        public final UUID uuid;
        public final String hash;
        public final SkinModel model;

        Entry(UUID uuid, String hash, SkinModel model) {
            this.uuid = uuid;
            this.hash = hash;
            this.model = model;
        }
    }

    private static final class Response {
        final int status;
        final byte[] body;

        Response(int status, byte[] body) {
            this.status = status;
            this.body = body;
        }

        String text() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    public static boolean configured() {
        return !baseUrl().isEmpty();
    }

    private static String baseUrl() {
        return SkinsConfig.loaded() ? SkinsConfig.SERVICE_URL.get().trim().replaceAll("/+$", "") : "";
    }

    public static synchronized void forgetToken() {
        token = null;
        tokenExpiresAt = 0;
        tokenRequest = null;
        signInFailure = null;
        signInRetryAt = 0;
    }

    private static <T> CompletableFuture<T> failed(Throwable error) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(error);
        return future;
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
            return failed(signInFailure);
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
        Session session = Minecraft.getMinecraft().getSession();
        JsonObject body = new JsonObject();
        body.addProperty("username", session.getUsername());

        String devToken = SkinsConfig.DEV_TOKEN.get().trim();
        if (devToken.isEmpty()) {
            String serverId = challenge();
            try {
                Minecraft.getMinecraft().getSessionService().joinServer(session.getProfile(), session.getToken(), serverId);
            } catch (InvalidCredentialsException e) {
                throw new CompletionException(new IOException("Not signed in to a Mojang account (offline or dev session)", e));
            } catch (Exception e) {
                throw new CompletionException(new IOException("Mojang session check failed: " + message(e), e));
            }
            body.addProperty("serverId", serverId);
        } else {
            body.addProperty("devToken", devToken);
            body.addProperty("uuid", session.getProfile().getId().toString());
        }

        Response response = send("POST", "/api/minecraft/skins/session", null, "application/json", body.toString().getBytes(StandardCharsets.UTF_8));
        if (response.status != 200) {
            throw new CompletionException(new IOException("Sign in failed: " + errorOf(response)));
        }
        JsonObject json = new JsonParser().parse(response.text()).getAsJsonObject();
        synchronized (SkinService.class) {
            token = json.get("token").getAsString();
            tokenExpiresAt = json.get("expiresAt").getAsLong();
            return token;
        }
    }

    /** The service hands out the serverId, so a Minecraft server we join cannot replay our handshake to it. */
    private static String challenge() {
        Response response = send("POST", "/api/minecraft/skins/challenge", null, null, new byte[0]);
        if (response.status != 200) {
            throw new CompletionException(new IOException("Sign in failed: " + errorOf(response)));
        }
        return new JsonParser().parse(response.text()).getAsJsonObject().get("serverId").getAsString();
    }

    public static CompletableFuture<Void> upload(Path file, SkinModel model) {
        return updateSkin(() -> token().thenApplyAsync(bearer -> {
            byte[] png;
            try {
                png = Files.readAllBytes(file);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
            Response response = send("PUT", "/api/minecraft/skins/self?model=" + model.name().toLowerCase(Locale.ROOT), bearer, "image/png", png);
            if (response.status == 401) {
                forgetToken();
            }
            if (response.status != 200) {
                throw new CompletionException(new IOException("Upload failed: " + errorOf(response)));
            }
            return null;
        }));
    }

    public static CompletableFuture<Void> clear() {
        return updateSkin(() -> token().thenApplyAsync(bearer -> {
            Response response = send("DELETE", "/api/minecraft/skins/self", bearer, null, null);
            if (response.status != 200) {
                throw new CompletionException(new IOException("Removal failed: " + errorOf(response)));
            }
            return null;
        }));
    }

    private static synchronized CompletableFuture<Void> updateSkin(Supplier<CompletableFuture<Void>> action) {
        // Keep slow uploads from overwriting a later selection or account-skin reset.
        skinUpdate = skinUpdate.handle((unused, error) -> null).thenCompose(unused -> action.get());
        return skinUpdate;
    }

    public static CompletableFuture<Map<UUID, Entry>> query(List<UUID> uuids) {
        return token().thenApplyAsync(bearer -> {
            JsonObject body = new JsonObject();
            JsonArray array = new JsonArray();
            uuids.forEach(uuid -> array.add(new JsonPrimitive(uuid.toString())));
            body.add("uuids", array);

            Response response = send("POST", "/api/minecraft/skins/query", bearer, "application/json", body.toString().getBytes(StandardCharsets.UTF_8));
            if (response.status == 401) {
                forgetToken();
            }
            if (response.status != 200) {
                throw new CompletionException(new IOException("Query failed: " + errorOf(response)));
            }

            Map<UUID, Entry> out = new HashMap<>();
            for (JsonElement element : new JsonParser().parse(response.text()).getAsJsonObject().getAsJsonArray("skins")) {
                JsonObject skin = element.getAsJsonObject();
                UUID uuid = UUID.fromString(skin.get("uuid").getAsString());
                out.put(uuid, new Entry(uuid, skin.get("hash").getAsString(), model(skin.get("model").getAsString())));
            }
            return out;
        });
    }

    public static CompletableFuture<byte[]> download(UUID uuid) {
        return token().thenApplyAsync(bearer -> {
            Response response = send("GET", "/api/minecraft/skins/" + uuid + ".png", bearer, null, null);
            if (response.status == 401) {
                forgetToken();
            }
            if (response.status != 200) {
                throw new CompletionException(new IOException("Download failed: HTTP " + response.status));
            }
            return response.body;
        });
    }

    /** Uuids of everyone in the player list, so one query covers the whole server. */
    public static List<UUID> onlinePlayers() {
        Minecraft minecraft = Minecraft.getMinecraft();
        List<UUID> uuids = new ArrayList<>();
        if (minecraft.getConnection() != null) {
            for (NetworkPlayerInfo info : minecraft.getConnection().getPlayerInfoMap()) {
                uuids.add(info.getGameProfile().getId());
            }
        }
        return uuids;
    }

    private static SkinModel model(String name) {
        try {
            return SkinModel.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SkinModel.AUTO;
        }
    }

    private static Response send(String method, String path, String bearer, String contentType, byte[] body) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl() + path).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            if (bearer != null) {
                connection.setRequestProperty("Authorization", "Bearer " + bearer);
            }
            if (contentType != null) {
                connection.setRequestProperty("Content-Type", contentType);
            }
            if (body != null) {
                connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body);
                }
            }
            int status = connection.getResponseCode();
            InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            return new Response(status, readAll(in));
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    static byte[] readAll(InputStream in) throws IOException {
        if (in == null) {
            return new byte[0];
        }
        try (InputStream input = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private static String errorOf(Response response) {
        try {
            JsonObject json = new JsonParser().parse(response.text()).getAsJsonObject();
            if (json.has("error")) {
                return json.get("error").getAsString();
            }
        } catch (RuntimeException ignored) {
            // Not a JSON error body, the status code is all we have.
        }
        return "HTTP " + response.status;
    }

    public static String message(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = cause.getClass().getSimpleName();
        }
        StynaSTMC.LOGGER.debug("Skin service error", error);
        return message;
    }
}
