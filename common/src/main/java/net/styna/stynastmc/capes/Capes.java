package net.styna.stynastmc.capes;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.Minecraft;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.styna.stynastmc.StynaSTMC;

/** Picks each player's cape from the enabled providers, plus the own cape override. */
public final class Capes {
    private static final String CUSTOM = "custom:";
    private static final long RETRY_MS = 60_000;
    private static final ClientAsset.Texture VANILLA_ELYTRA =
            new ClientAsset.ResourceTexture(Identifier.withDefaultNamespace("entity/equipment/wings/elytra"));

    private static final Map<UUID, Map<CapeProvider, Lookup>> LOOKUPS = new HashMap<>();

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

    public static List<CapeProvider> enabled() {
        List<CapeProvider> providers = new ArrayList<>();
        if (CapesConfig.loaded()) {
            for (String id : CapesConfig.PROVIDERS.get()) {
                CapeProvider provider = CapeProvider.byId(id);
                if (provider != null && !providers.contains(provider)) {
                    providers.add(provider);
                }
            }
        }
        return providers;
    }

    public static void setEnabled(List<CapeProvider> providers) {
        CapesConfig.PROVIDERS.set(providers.stream().map(CapeProvider::id).toList());
        CapesConfig.save();
    }

    /** Own cape choice: empty follows the providers, else a provider id or custom:<path>. */
    public static String own() {
        return CapesConfig.loaded() ? CapesConfig.OWN.get().trim() : "";
    }

    public static void setOwn(String value) {
        CapesConfig.OWN.set(value == null ? "" : value);
        CapesConfig.save();
        RemoteCapes.shareSoon();
    }

    public static String customValue(Path file) {
        return CUSTOM + file.toAbsolutePath().normalize();
    }

    public static Path ownCustomFile() {
        String own = own();
        if (!own.startsWith(CUSTOM)) {
            return null;
        }
        try {
            return Path.of(own.substring(CUSTOM.length()));
        } catch (InvalidPathException e) {
            return null;
        }
    }

    public static boolean isSelf(UUID uuid) {
        Minecraft minecraft = Minecraft.getInstance();
        UUID self = minecraft.player != null ? minecraft.player.getUUID() : minecraft.getUser().getProfileId();
        return uuid.equals(self);
    }

    public static PlayerSkin apply(GameProfile profile, PlayerSkin base) {
        if (!CapesConfig.loaded()) {
            return base;
        }
        boolean self = isSelf(profile.id());
        if (self) {
            ClientAsset.Texture own = ownOverride(profile, base);
            if (own != null) {
                return with(base, own);
            }
        }
        // A cape shared through the skin service is what that player picked in this mod, so it wins.
        CapeTexture shared = self ? null : RemoteCapes.texture(profile.id());
        if (shared != null) {
            return with(base, shared.asset());
        }
        for (CapeProvider provider : enabled()) {
            ClientAsset.Texture cape = provider == CapeProvider.MOJANG
                    ? self ? ownMojang(base) : base.cape()
                    : asset(texture(profile, provider));
            if (cape != null) {
                return cape == base.cape() ? base : with(base, cape);
            }
        }
        return base.cape() == null ? base : with(base, null);
    }

    private static ClientAsset.Texture ownOverride(GameProfile profile, PlayerSkin base) {
        Path custom = ownCustomFile();
        if (custom != null) {
            return asset(CustomCapes.texture(custom));
        }
        CapeProvider provider = CapeProvider.byId(own());
        if (provider == null) {
            return null;
        }
        return provider == CapeProvider.MOJANG ? ownMojang(base) : asset(texture(profile, provider));
    }

    private static ClientAsset.Texture ownMojang(PlayerSkin base) {
        if (!MojangCapes.overridden()) {
            return base.cape();
        }
        CapeTexture texture = MojangCapes.overrideTexture();
        return texture == null ? null : texture.asset();
    }

    private static ClientAsset.Texture asset(CapeTexture texture) {
        return texture == null ? null : texture.asset();
    }

    private static PlayerSkin with(PlayerSkin base, ClientAsset.Texture cape) {
        // Unset elytra reuses the cape texture like vanilla; capes with an empty elytra area get the plain wings instead.
        ClientAsset.Texture elytra = cape instanceof CapeTexture.Asset asset && !asset.elytra() ? VANILLA_ELYTRA : null;
        return new PlayerSkin(base.body(), cape, elytra, base.model(), base.secure());
    }

    /** Looks the player up on that provider once per session, retrying failures after a while. */
    public static CapeTexture texture(GameProfile profile, CapeProvider provider) {
        Lookup lookup = lookup(profile, provider);
        return lookup.state == State.LOADED ? lookup.texture : null;
    }

    public static State state(GameProfile profile, CapeProvider provider) {
        return lookup(profile, provider).state;
    }

    public static String error(GameProfile profile, CapeProvider provider) {
        return lookup(profile, provider).error;
    }

    private static Lookup lookup(GameProfile profile, CapeProvider provider) {
        Map<CapeProvider, Lookup> lookups = LOOKUPS.computeIfAbsent(profile.id(), uuid -> new EnumMap<>(CapeProvider.class));
        Lookup lookup = lookups.get(provider);
        if (lookup != null && (lookup.state != State.FAILED || System.currentTimeMillis() < lookup.retryAt)) {
            return lookup;
        }
        Lookup fresh = new Lookup();
        lookups.put(provider, fresh);
        provider.fetch(profile).whenComplete((fetched, error) -> Minecraft.getInstance().execute(() -> {
            Map<CapeProvider, Lookup> current = LOOKUPS.get(profile.id());
            if (current == null || current.get(provider) != fresh) {
                return;
            }
            try {
                if (error != null) {
                    throw error;
                }
                if (fetched == null) {
                    fresh.state = State.NONE;
                } else {
                    fresh.texture = CapeTexture.load(fetched.png(), fetched.animated(), provider.id() + "/" + profile.id());
                    fresh.state = State.LOADED;
                }
            } catch (Throwable e) {
                fresh.state = State.FAILED;
                fresh.error = MojangCapes.message(e);
                fresh.retryAt = System.currentTimeMillis() + RETRY_MS;
                StynaSTMC.LOGGER.debug("{} cape lookup failed for {}: {}", provider.title(), profile.name(), fresh.error);
            }
        }));
        return fresh;
    }

    /** Forgets one player's lookups so the next frame asks again. */
    public static void forget(UUID uuid) {
        Map<CapeProvider, Lookup> lookups = LOOKUPS.remove(uuid);
        if (lookups != null) {
            lookups.values().stream().filter(lookup -> lookup.texture != null).forEach(lookup -> lookup.texture.release());
        }
    }

    public static void clear() {
        List.copyOf(LOOKUPS.keySet()).forEach(Capes::forget);
    }

    public static void onConfigReloading() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.execute(() -> {
                CustomCapes.refresh();
                RemoteCapes.shareSoon();
            });
        }
    }

    /** Keeps the own lookups across worlds, they are what the settings screen shows. */
    public static void onLoggingOut() {
        UUID self = Minecraft.getInstance().getUser().getProfileId();
        Map<CapeProvider, Lookup> own = LOOKUPS.remove(self);
        clear();
        RemoteCapes.clear();
        if (own != null) {
            LOOKUPS.put(self, own);
        }
    }
}
