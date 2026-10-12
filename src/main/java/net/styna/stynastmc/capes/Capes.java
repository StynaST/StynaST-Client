package net.styna.stynastmc.capes;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.styna.stynastmc.StynaSTMC;

/** Your own cape override. Other players keep whatever the server sends. */
public final class Capes {
    public static final String NONE = "none";
    private static final String CUSTOM = "custom:";
    private static final String MOJANG = "mojang:";
    private static final long RETRY_MS = 60_000;
    private static final ResourceLocation VANILLA_ELYTRA = new ResourceLocation("textures/entity/elytra.png");

    static final ExecutorService HTTP = Executors.newFixedThreadPool(2, r -> {
        Thread thread = new Thread(r, "StynaSTMC capes");
        thread.setDaemon(true);
        return thread;
    });

    private static final Map<CapeProvider, Lookup> LOOKUPS = new EnumMap<>(CapeProvider.class);

    private Capes() {
    }

    public enum State {
        LOADING,
        NONE,
        LOADED,
        FAILED
    }

    private static final class Lookup {
        State state = State.LOADING;
        CapeTexture texture;
        String error;
        long retryAt;
    }

    /** Empty keeps the server cape, otherwise none, a provider id, mojang:<id> or custom:<path>. */
    public static String own() {
        return CapesConfig.loaded() ? CapesConfig.OWN.get().trim() : "";
    }

    public static void setOwn(String value) {
        CapesConfig.OWN.set(value == null ? "" : value);
        CapesConfig.save();
    }

    public static String customValue(Path file) {
        return CUSTOM + file.toAbsolutePath().normalize();
    }

    public static String mojangValue(MojangCapes.AccountCape cape) {
        return MOJANG + cape.id;
    }

    public static Path ownCustomFile() {
        String own = own();
        if (!own.startsWith(CUSTOM)) {
            return null;
        }
        try {
            return Paths.get(own.substring(CUSTOM.length()));
        } catch (InvalidPathException e) {
            return null;
        }
    }

    public static String ownMojangId() {
        String own = own();
        return own.startsWith(MOJANG) ? own.substring(MOJANG.length()) : null;
    }

    private static boolean isSelf(UUID uuid) {
        Minecraft minecraft = Minecraft.getMinecraft();
        UUID self = minecraft.player != null ? minecraft.player.getUniqueID() : minecraft.getSession().getProfile().getId();
        return uuid != null && uuid.equals(self);
    }

    /** The cape you picked, or null for none (also while it loads). */
    public static CapeTexture ownTexture() {
        String own = own();
        if (own.isEmpty() || own.equals(NONE)) {
            return null;
        }
        Path custom = ownCustomFile();
        if (custom != null) {
            return CustomCapes.texture(custom);
        }
        String mojangId = ownMojangId();
        CapeProvider provider = CapeProvider.byId(own);
        if (mojangId != null || provider == CapeProvider.MOJANG) {
            MojangCapes.AccountCape cape = mojangId == null ? active() : MojangCapes.byId(mojangId);
            return cape == null ? null : MojangCapes.texture(cape);
        }
        return provider == null ? null : texture(provider);
    }

    private static MojangCapes.AccountCape active() {
        if (MojangCapes.capes() != null) {
            for (MojangCapes.AccountCape cape : MojangCapes.capes()) {
                if (cape.active) {
                    return cape;
                }
            }
        }
        return null;
    }

    public static ResourceLocation cape(UUID uuid, ResourceLocation vanilla) {
        if (own().isEmpty() || !isSelf(uuid)) {
            return vanilla;
        }
        CapeTexture texture = ownTexture();
        return texture == null ? null : texture.current();
    }

    /** Capes without an elytra area get the plain wings, like vanilla would for a missing texture. */
    public static ResourceLocation elytra(UUID uuid, ResourceLocation vanilla) {
        if (own().isEmpty() || !isSelf(uuid)) {
            return vanilla;
        }
        CapeTexture texture = ownTexture();
        return texture == null ? null : texture.elytra ? texture.current() : VANILLA_ELYTRA;
    }

    /** Your cape on a third party site, looked up once per session with your account's UUID and name. */
    public static CapeTexture texture(CapeProvider provider) {
        Lookup lookup = lookup(provider);
        return lookup.state == State.LOADED ? lookup.texture : null;
    }

    public static State state(CapeProvider provider) {
        return lookup(provider).state;
    }

    public static String error(CapeProvider provider) {
        return lookup(provider).error;
    }

    private static Lookup lookup(CapeProvider provider) {
        Lookup lookup = LOOKUPS.get(provider);
        if (lookup != null && (lookup.state != State.FAILED || System.currentTimeMillis() < lookup.retryAt)) {
            return lookup;
        }
        Lookup fresh = new Lookup();
        LOOKUPS.put(provider, fresh);
        GameProfile profile = Minecraft.getMinecraft().getSession().getProfile();
        CompletableFuture.supplyAsync(() -> {
            try {
                return provider.fetch(profile.getId(), profile.getName());
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, HTTP).whenComplete((fetched, failure) -> Minecraft.getMinecraft().addScheduledTask(() -> {
            if (LOOKUPS.get(provider) != fresh) {
                return;
            }
            try {
                if (failure != null) {
                    throw failure;
                }
                if (fetched == null) {
                    fresh.state = State.NONE;
                } else {
                    fresh.texture = CapeTexture.load(fetched.png, fetched.animated, provider.id() + "/" + profile.getId());
                    fresh.state = State.LOADED;
                }
            } catch (Throwable e) {
                fresh.state = State.FAILED;
                fresh.error = message(e);
                fresh.retryAt = System.currentTimeMillis() + RETRY_MS;
                StynaSTMC.LOGGER.debug("{} cape lookup failed: {}", provider.title(), fresh.error);
            }
        }));
        return fresh;
    }

    public static void onConfigReloading() {
        Minecraft.getMinecraft().addScheduledTask(CustomCapes::refresh);
    }

    static String message(Throwable error) {
        while (error.getCause() != null && (error instanceof CompletionException || error.getClass() == RuntimeException.class)) {
            error = error.getCause();
        }
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
