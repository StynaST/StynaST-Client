package net.styna.stynastmc.capes;

import java.util.List;

import net.styna.stynastmc.config.StynaConfigSpec;

public final class CapesConfig {
    public static final StynaConfigSpec SPEC;
    public static final StynaConfigSpec.ConfigValue<List<? extends String>> PROVIDERS;
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
        PROVIDERS = builder.comment("Cape providers checked for every player after capes shared through the skin service, first match wins. Known: "
                        + String.join(", ", CapeProvider.ids()) + ". Every provider but mojang sends player UUIDs or names to that site.")
                .defineList("providers", () -> List.of(CapeProvider.MOJANG.id()), () -> "", item -> item instanceof String);
        OWN = builder.comment("Your own cape. Empty follows the providers above, a provider id prefers that provider, custom:<path> uses a local PNG, shared with other StynaST-MC players through the skin service.")
                .define("own", "");
        DIRECTORY = builder.comment("Folder holding custom cape PNGs. Empty means config/StynaSTMC/capes.").define("directory", "");
        builder.pop();
        SPEC = builder.build();
    }
}
