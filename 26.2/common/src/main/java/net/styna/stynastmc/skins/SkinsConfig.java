package net.styna.stynastmc.skins;

import net.styna.stynastmc.config.StynaConfigSpec;

public final class SkinsConfig {
    public static final StynaConfigSpec SPEC;
    public static final StynaConfigSpec.BooleanValue ENABLED;
    public static final StynaConfigSpec.ConfigValue<String> DIRECTORY;
    public static final StynaConfigSpec.ConfigValue<String> SKIN;
    public static final StynaConfigSpec.EnumValue<SkinModel> MODEL;
    public static final StynaConfigSpec.ConfigValue<String> SERVICE_URL;
    public static final StynaConfigSpec.ConfigValue<String> DEV_TOKEN;

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
        ENABLED = builder.comment("Render your own player with the skin picked in the Skin tab.").define("enabled", true);
        DIRECTORY = builder.comment("Folder holding skin PNGs. 64x64 and HD multiples of it (128, 256, 512) are accepted.")
                .define("directory", "");
        SKIN = builder.comment("Skin file inside the folder above. Empty means the account skin.").define("skin", "");
        MODEL = builder.comment("Arm width. AUTO keeps the model of your account skin.").defineEnum("model", SkinModel.AUTO);
        builder.push("sharing");
        SERVICE_URL = builder.comment("Skin service origin. Empty disables sharing entirely.").define("service_url", "https://styna.net");
        DEV_TOKEN = builder.comment("Dev only: skips the Mojang session check against a test server.").define("dev_token", "");
        builder.pop();
        builder.pop();
        SPEC = builder.build();
    }
}
