package net.styna.stynastmc.capes;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.styna.stynastmc.Tags;

/** Where your cape can come from. Mojang means the capes owned by your account; the rest are third party cape sites. */
public enum CapeProvider {
    MOJANG("mojang", "Mojang", "https://www.minecraft.net/msaprofile/mygames/editprofile"),
    OPTIFINE("optifine", "OptiFine", "https://optifine.net/home"),
    MINECRAFTCAPES("minecraftcapes", "MinecraftCapes", "https://minecraftcapes.net"),
    COSMETICA("cosmetica", "Cosmetica", "https://cosmetica.cc"),
    LABYMOD("labymod", "LabyMod", "https://labymod.net");

    private static final int TIMEOUT = 15_000;
    private static final int MAX_BYTES = 10_000_000;

    private final String id;
    private final String title;
    private final String homepage;

    CapeProvider(String id, String title, String homepage) {
        this.id = id;
        this.title = title;
        this.homepage = homepage;
    }

    /** Raw cape PNG; animated means frames stacked vertically. */
    public static final class Fetched {
        public final byte[] png;
        public final boolean animated;

        Fetched(byte[] png, boolean animated) {
            this.png = png;
            this.animated = animated;
        }
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String homepage() {
        return homepage;
    }

    public static CapeProvider byId(String id) {
        String wanted = id.trim().toLowerCase(Locale.ROOT);
        for (CapeProvider provider : values()) {
            if (provider.id.equals(wanted)) {
                return provider;
            }
        }
        return null;
    }

    public static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (CapeProvider provider : values()) {
            ids.add(provider.id);
        }
        return ids;
    }

    /** Blocking lookup, null when there is no cape. Mojang capes go through {@link MojangCapes} instead. */
    Fetched fetch(UUID uuid, String name) throws IOException {
        String plain = uuid.toString().replace("-", "");
        switch (this) {
            case OPTIFINE:
                return name == null || name.trim().isEmpty() ? null : png("http://s.optifine.net/capes/" + name + ".png", false);
            case MINECRAFTCAPES: {
                JsonObject json = json("https://api.minecraftcapes.net/profile/" + plain);
                String url = string(json, "cape_url");
                boolean animated = json != null && json.has("animatedCape") && json.get("animatedCape").getAsBoolean();
                return url == null ? null : png(url, animated);
            }
            case COSMETICA: {
                JsonObject json = json("https://api.cloaks.gg/users/" + uuid + "/cape");
                JsonObject cloak = json != null && json.get("cloak") instanceof JsonObject object ? object : null;
                String url = string(cloak, "texture");
                boolean animated = cloak != null && cloak.has("frames") && cloak.get("frames").getAsInt() > 1;
                return url == null ? null : png(url, animated);
            }
            case LABYMOD:
                return png("https://dl.labymod.net/capes/" + uuid, false);
            default:
                return null;
        }
    }

    static byte[] get(String url, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);
        connection.setRequestProperty("User-Agent", "StynaST-MC/" + Tags.VERSION);
        if (headers != null) {
            headers.forEach(connection::setRequestProperty);
        }
        try {
            int status = connection.getResponseCode();
            if (status == 404 || status == 204) {
                return null;
            }
            if (status == 401) {
                throw new IOException("Not signed in to a Microsoft account");
            }
            if (status / 100 != 2) {
                throw new IOException("HTTP " + status);
            }
            try (InputStream in = connection.getInputStream()) {
                byte[] buffer = new byte[8192];
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    if (out.size() > MAX_BYTES) {
                        throw new IOException("Response larger than " + MAX_BYTES + " bytes");
                    }
                }
                return out.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    static JsonObject json(String url) throws IOException {
        byte[] body = get(url, null);
        if (body == null) {
            return null;
        }
        JsonElement parsed = new JsonParser().parse(new String(body, StandardCharsets.UTF_8));
        return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
    }

    private static Fetched png(String url, boolean animated) throws IOException {
        byte[] body = get(url, null);
        return body == null ? null : new Fetched(body, animated);
    }

    static String string(JsonObject json, String key) {
        JsonElement value = json == null ? null : json.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }
}
