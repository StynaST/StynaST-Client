package net.styna.stynastmc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.styna.stynastmc.chatheads.config.ChatHeadsConfig;
import net.styna.stynastmc.compat.ModCompat;
import net.styna.stynastmc.config.StynaConfigSpec;
import net.styna.stynastmc.classicbar.BarHud;
import net.styna.stynastmc.classicbar.config.ClassicBarConfig;
import net.styna.stynastmc.clearspam.ClearSpam;
import net.styna.stynastmc.clearspam.config.ClearSpamConfig;
import net.styna.stynastmc.config.ConfigKey;
import net.styna.stynastmc.fonts.FontsBridge;
import net.styna.stynastmc.fonts.FontsConfig;
import net.styna.stynastmc.hotreload.HotReload;
import net.styna.stynastmc.hotreload.SkinTotem;
import net.styna.stynastmc.freecam.FreecamConfigImpl;
import net.styna.stynastmc.muffler.Muffler;
import net.styna.stynastmc.platform.Platform;
import net.styna.stynastmc.skins.SkinsBridge;
import net.styna.stynastmc.skins.CustomSkins;
import net.styna.stynastmc.skins.SkinsConfig;
import net.styna.stynastmc.tweaks.ContainerPreviewConfig;
import net.styna.stynastmc.tweaks.DebugPauseKey;
import net.styna.stynastmc.tweaks.EnchantDescriptionsConfig;
import net.styna.stynastmc.tweaks.TradeCycler;
import net.styna.stynastmc.tweaks.TweaksConfig;
import net.styna.stynastmc.zoom.Zoom;
import net.styna.stynastmc.zoom.config.ZoomConfig;
import net.styna.stynastmc.freecam.Freecam;
import net.styna.stynastmc.freecam.config.ModBindings;

/** Loader independent client entry point. Loaders forward their events here. */
public final class StynaSTMCClient {
    public record Config(StynaConfigSpec spec, String name, String legacyName, Runnable onLoad, Runnable onReload) {
        public Path path() {
            Path legacy = Platform.INSTANCE.configDir().resolve(StynaSTMC.MODID + "-" + legacyName + "-client.toml");
            return Platform.INSTANCE.modConfigFile(name + ".toml", legacy);
        }
    }

    private static final Runnable NONE = () -> {
    };
    private static long lastConfigCheck;

    public static final List<Config> CONFIGS = Stream.of(
            new Config(ClassicBarConfig.SPEC, "bars", "classicbar", BarHud::reload, BarHud::reload),
            new Config(ClearSpamConfig.SPEC, "clearspam", "clearspam", ClearSpam::syncConfig, ClearSpam::syncConfig),
            new Config(FreecamConfigImpl.SPEC, "freecam", "freecam", NONE, NONE),
            new Config(FontsConfig.SPEC, "fonts", "fonts", FontsBridge::onConfigLoading, NONE),
            new Config(SkinsConfig.SPEC, "skins", "skins", NONE, SkinsBridge::onConfigReloading),
            new Config(ZoomConfig.SPEC, "zoom", "zoom", NONE, NONE),
            new Config(TweaksConfig.SPEC, "tweaks", "tweaks", NONE, NONE),
            unless(ModCompat.ENCHANT_DESCRIPTIONS, new Config(EnchantDescriptionsConfig.SPEC, "enchantdescriptions", "enchantdescriptions", NONE, NONE)),
            unless(ModCompat.SHULKER_BOX_TOOLTIP, new Config(ContainerPreviewConfig.SPEC, "containerpreview", "containerpreview", NONE, NONE)),
            unless(ModCompat.CHAT_HEADS, new Config(ChatHeadsConfig.SPEC, "chatheads", "chatheads", NONE, NONE)))
            .filter(Objects::nonNull).toList();

    // Ported features step aside for the original mod, config file included.
    private static Config unless(String modId, Config config) {
        return ModCompat.isLoaded(modId) ? null : config;
    }

    private StynaSTMCClient() {
    }

    public static void init() {
        Muffler.load();
    }

    public static List<KeyMapping> keyMappings() {
        List<KeyMapping> keys = new ArrayList<>();
        ModBindings.forEach(keys::add);
        keys.add(Zoom.KEY);
        keys.add(Muffler.KEY);
        keys.add(ConfigKey.OPEN);
        keys.add(DebugPauseKey.KEY);
        keys.add(TradeCycler.KEY);
        return keys;
    }

    public static void preTick(Minecraft minecraft) {
        Freecam.preTick(minecraft);
    }

    public static void postTick(Minecraft minecraft) {
        long now = System.currentTimeMillis();
        if (now - lastConfigCheck >= 1000) {
            lastConfigCheck = now;
            for (Config config : CONFIGS) {
                if (config.spec().reloadIfModified()) {
                    config.onReload().run();
                }
            }
        }
        Freecam.postTick(minecraft);
        ConfigKey.tick(minecraft);
        DebugPauseKey.tick(minecraft);
        TradeCycler.tick(minecraft);
        Muffler.tick(minecraft);
        HotReload.tick(minecraft);
        CustomSkins.tick();
        SkinTotem.tick(minecraft);
    }

    public static void onLoggingOut() {
        ClearSpam.onDisconnect();
        net.styna.stynastmc.classicbar.network.ClassicBarNetwork.presentOnServer = false;
    }
}
