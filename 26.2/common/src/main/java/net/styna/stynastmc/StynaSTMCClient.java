package net.styna.stynastmc;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.styna.stynastmc.config.StynaConfigSpec;
import net.styna.stynastmc.classicbar.BarHud;
import net.styna.stynastmc.classicbar.config.ClassicBarConfig;
import net.styna.stynastmc.clearspam.ClearSpam;
import net.styna.stynastmc.clearspam.config.ClearSpamConfig;
import net.styna.stynastmc.config.ConfigKey;
import net.styna.stynastmc.fonts.FontsBridge;
import net.styna.stynastmc.fonts.FontsConfig;
import net.styna.stynastmc.freecam.FreecamConfigImpl;
import net.styna.stynastmc.muffler.Muffler;
import net.styna.stynastmc.skins.SkinsBridge;
import net.styna.stynastmc.skins.SkinsConfig;
import net.styna.stynastmc.zoom.Zoom;
import net.styna.stynastmc.zoom.config.ZoomConfig;
import net.styna.stynastmc.freecam.Freecam;
import net.styna.stynastmc.freecam.config.ModBindings;

/** Loader independent client entry point. Loaders forward their events here. */
public final class StynaSTMCClient {
    /** A client config file, with callbacks for when it is first loaded and when it changes on disk. */
    public record Config(StynaConfigSpec spec, String fileName, Runnable onLoad, Runnable onReload) {
        private static Config of(StynaConfigSpec spec, String name, Runnable onLoad, Runnable onReload) {
            return new Config(spec, StynaSTMC.MODID + "-" + name + "-client.toml", onLoad, onReload);
        }
    }

    private static final Runnable NONE = () -> {
    };
    private static long lastConfigCheck;

    public static final List<Config> CONFIGS = List.of(
            Config.of(ClassicBarConfig.SPEC, "classicbar", BarHud::reload, BarHud::reload),
            Config.of(ClearSpamConfig.SPEC, "clearspam", ClearSpam::syncConfig, ClearSpam::syncConfig),
            Config.of(FreecamConfigImpl.SPEC, "freecam", NONE, NONE),
            Config.of(FontsConfig.SPEC, "fonts", FontsBridge::onConfigLoading, NONE),
            Config.of(SkinsConfig.SPEC, "skins", NONE, SkinsBridge::onConfigReloading),
            Config.of(ZoomConfig.SPEC, "zoom", NONE, NONE));

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
        Muffler.tick(minecraft);
    }

    public static void onLoggingOut() {
        ClearSpam.onDisconnect();
        net.styna.stynastmc.classicbar.network.ClassicBarNetwork.presentOnServer = false;
    }
}
