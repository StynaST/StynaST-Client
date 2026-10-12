package net.styna.stynastmc.capes;

import net.styna.stynastmc.config.StynaConfigSpec;

public final class CapesConfig {
    public static final StynaConfigSpec SPEC;
    public static final StynaConfigSpec.ConfigValue<String> OWN;
    public static final StynaConfigSpec.ConfigValue<String> DIRECTORY;

    private CapesConfig() {
    }

    public static boolean loaded() {
        return SPEC.isLoaded();
    }

    public static void save() {
        SPEC.save();
    }

    static {
        StynaConfigSpec.Builder builder = new StynaConfigSpec.Builder();
        builder.push("capes");
        OWN = builder.comment("Your own cape, only you see it. Empty keeps the server's cape, none hides it, a provider id ("
                        + String.join(", ", CapeProvider.ids()) + ") uses your cape there, mojang:<id> one of your account capes, custom:<path> a local PNG.")
                .define("own", "");
        DIRECTORY = builder.comment("Folder holding custom cape PNGs. Empty means config/StynaSTMC/capes.").define("directory", "");
        builder.pop();
        SPEC = builder.build();
    }
}
