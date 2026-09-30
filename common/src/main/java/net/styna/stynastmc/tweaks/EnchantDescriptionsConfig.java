package net.styna.stynastmc.tweaks;

import net.styna.stynastmc.config.StynaConfigSpec;

/** File only, no settings tab. Mirrors options Enchantment Descriptions ships. */
public final class EnchantDescriptionsConfig {
    public static final StynaConfigSpec SPEC;
    public static final StynaConfigSpec.BooleanValue ENABLED;
    public static final StynaConfigSpec.BooleanValue REQUIRE_SHIFT;

    private EnchantDescriptionsConfig() {
    }

    static {
        StynaConfigSpec.Builder builder = new StynaConfigSpec.Builder();
        builder.push("enchantment_descriptions");
        ENABLED = builder.comment("Explain what each enchantment does in item tooltips.")
                .define("enabled", true);
        REQUIRE_SHIFT = builder.comment("Only show descriptions while Shift is held.")
                .define("require_shift", false);
        builder.pop();
        SPEC = builder.build();
    }

    public static boolean on(StynaConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
