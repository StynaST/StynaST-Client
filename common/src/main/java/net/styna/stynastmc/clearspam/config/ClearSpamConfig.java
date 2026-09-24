package net.styna.stynastmc.clearspam.config;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClearSpamConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue SPAM_WINDOW_SECONDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED;

    private ClearSpamConfig() {
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");
        ENABLED = builder.comment("Collapse repeated chat messages into one line with a counter.")
                .define("enabled", true);
        SPAM_WINDOW_SECONDS = builder.comment("How long a message keeps counting repeats.")
                .defineInRange("spam_window_seconds", 30, 1, 3600);
        BLOCKED = builder.comment("Messages containing any of these strings are hidden. Same as '.clearspam block <string>'.")
                .defineList("blocked_messages", () -> List.<String>of(), () -> "", String.class::isInstance);
        builder.pop();
        SPEC = builder.build();
    }
}
