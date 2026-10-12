package net.styna.stynastmc.clearspam.config;

import java.util.ArrayList;
import java.util.List;

import net.styna.stynastmc.config.StynaConfigSpec;

public final class ClearSpamConfig {
    public static final StynaConfigSpec SPEC;
    public static final StynaConfigSpec.BooleanValue ENABLED;
    public static final StynaConfigSpec.IntValue SPAM_WINDOW_SECONDS;
    public static final StynaConfigSpec.ConfigValue<List<? extends String>> BLOCKED;

    private ClearSpamConfig() {
    }

    static {
        StynaConfigSpec.Builder builder = new StynaConfigSpec.Builder();
        builder.push("general");
        ENABLED = builder.comment("Collapse repeated chat messages into one line with a counter.")
                .define("enabled", true);
        SPAM_WINDOW_SECONDS = builder.comment("How long a message keeps counting repeats.")
                .defineInRange("spam_window_seconds", 30, 1, 3600);
        BLOCKED = builder.comment("Messages containing any of these strings are hidden. Same as '.clearspam block <string>'.")
                .defineList("blocked_messages", ArrayList::new, String.class::isInstance);
        builder.pop();
        SPEC = builder.build();
    }
}
