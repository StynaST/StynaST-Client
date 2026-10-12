package net.styna.stynastmc.capes;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.styna.stynastmc.StynaSTMC;

/** Capes owned by the signed in account, read through the Minecraft services API. Wearing one here is local only. */
public final class MojangCapes {
    private static final String PROFILE = "https://api.minecraftservices.com/minecraft/profile";

    private static List<AccountCape> capes;
    private static String error;
    private static boolean loading;
    private static final Map<String, CapeTexture> TEXTURES = new HashMap<>();
    private static final Map<String, String> TEXTURE_ERRORS = new HashMap<>();

    private MojangCapes() {
    }

    public static final class AccountCape {
        public final String id;
        public final String alias;
        public final String url;
        public final boolean active;

        AccountCape(String id, String alias, String url, boolean active) {
            this.id = id;
            this.alias = alias;
            this.url = url;
            this.active = active;
        }
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

    public static AccountCape byId(String id) {
        List<AccountCape> list = capes();
        if (list != null) {
            for (AccountCape cape : list) {
                if (cape.id.equals(id)) {
                    return cape;
                }
            }
        }
        return null;
    }

    public static void refresh() {
        loading = true;
        error = null;
        String token = Minecraft.getMinecraft().getSession().getToken();
        CompletableFuture.supplyAsync(() -> {
            try {
                byte[] body = CapeProvider.get(PROFILE, Collections.singletonMap("Authorization", "Bearer " + token));
                return parse(body == null ? "{}" : new String(body, StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, Capes.HTTP).whenComplete((list, failure) -> Minecraft.getMinecraft().addScheduledTask(() -> {
            loading = false;
            if (failure != null) {
                error = Capes.message(failure);
            } else {
                capes = list;
            }
        }));
    }

    public static CapeTexture texture(AccountCape cape) {
        CapeTexture loaded = TEXTURES.get(cape.id);
        if (TEXTURES.containsKey(cape.id) || TEXTURE_ERRORS.containsKey(cape.id)) {
            return loaded;
        }
        TEXTURES.put(cape.id, null);
        CompletableFuture.supplyAsync(() -> {
            try {
                return CapeProvider.get(cape.url.replaceFirst("^http://", "https://"), null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, Capes.HTTP).whenComplete((png, failure) -> Minecraft.getMinecraft().addScheduledTask(() -> {
            try {
                if (failure != null) {
                    throw failure;
                }
                if (png == null) {
                    throw new IllegalStateException("Cape texture missing");
                }
                TEXTURES.put(cape.id, CapeTexture.load(png, false, "mojang/" + cape.alias));
            } catch (Throwable e) {
                TEXTURES.remove(cape.id);
                TEXTURE_ERRORS.put(cape.id, Capes.message(e));
                StynaSTMC.LOGGER.warn("Could not load Mojang cape {}: {}", cape.alias, Capes.message(e));
            }
        }));
        return null;
    }

    public static String textureError(AccountCape cape) {
        return TEXTURE_ERRORS.get(cape.id);
    }

    private static List<AccountCape> parse(String body) {
        List<AccountCape> list = new ArrayList<>();
        JsonObject profile = new JsonParser().parse(body).getAsJsonObject();
        if (profile.get("capes") != null && profile.get("capes").isJsonArray()) {
            for (JsonElement element : profile.getAsJsonArray("capes")) {
                JsonObject cape = element.getAsJsonObject();
                list.add(new AccountCape(cape.get("id").getAsString(), cape.get("alias").getAsString(), cape.get("url").getAsString(),
                        "ACTIVE".equals(cape.get("state").getAsString())));
            }
        }
        return Collections.unmodifiableList(list);
    }
}
