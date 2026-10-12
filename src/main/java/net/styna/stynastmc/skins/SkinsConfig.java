package net.styna.stynastmc.skins;

import java.util.ArrayList;
import java.util.List;

import net.styna.stynastmc.config.StynaConfigSpec;

public final class SkinsConfig {
    public static final StynaConfigSpec SPEC;
    public static final StynaConfigSpec.BooleanValue ENABLED;
    public static final StynaConfigSpec.ConfigValue<String> DIRECTORY;
    public static final StynaConfigSpec.ConfigValue<String> SKIN;
    public static final StynaConfigSpec.EnumValue<SkinModel> MODEL;
    public static final StynaConfigSpec.ConfigValue<String> SERVICE_URL;
    public static final StynaConfigSpec.ConfigValue<String> DEV_TOKEN;
    public static final StynaConfigSpec.BooleanValue OFFLINE_ENABLED;
    public static final StynaConfigSpec.BooleanValue OFFLINE_ALL;
    public static final StynaConfigSpec.ConfigValue<List<? extends String>> OFFLINE_PLAYERS;

    private SkinsConfig() {
    }

    public static boolean loaded() {
        return SPEC.isLoaded();
    }

    public static void save() {
        SPEC.save();
    }

    static {
        StynaConfigSpec.Builder builder = new StynaConfigSpec.Builder();
        builder.push("skins");
        ENABLED = builder.comment("Render your own player with the local skin, or your skin uploaded to the skin service. False uses the account skin.")
                .define("enabled", true);
        DIRECTORY = builder.comment("Folder holding skin PNGs. 64x64 and HD multiples of it (128, 256, 512) are accepted. Empty means config/StynaSTMC/skins.")
                .define("directory", "");
        SKIN = builder.comment("Absolute path of the skin PNG. Empty or missing falls back to your uploaded skin.").define("skin", "");
        MODEL = builder.comment("Arm width. AUTO keeps the model of your account skin.").defineEnum("model", SkinModel.AUTO);
        builder.push("sharing");
        SERVICE_URL = builder.comment("Skin service origin. Empty disables sharing entirely.").define("service_url", "https://styna.net");
        DEV_TOKEN = builder.comment("Dev only: skips the Mojang session check against a test server.").define("dev_token", "");
        builder.pop();
        builder.push("offline");
        OFFLINE_ENABLED = builder.comment("On offline-mode servers, give chosen players the official Mojang skin of their name.")
                .define("enabled", true);
        OFFLINE_ALL = builder.comment("Apply to every offline player instead of only the chosen ones.").define("all_players", false);
        OFFLINE_PLAYERS = builder.comment("Player names that get their Mojang skin.")
                .defineList("players", ArrayList::new, String.class::isInstance);
        builder.pop();
        builder.pop();
        SPEC = builder.build();
    }
}
