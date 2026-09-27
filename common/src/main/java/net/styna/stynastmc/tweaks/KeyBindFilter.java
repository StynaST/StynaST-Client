package net.styna.stynastmc.tweaks;

import java.util.Locale;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Search text plus a mode, applied to the key binds list. */
public record KeyBindFilter(String query, Mode mode) {
    public static final KeyBindFilter NONE = new KeyBindFilter("", Mode.ALL);

    public enum Mode {
        ALL, CONFLICTS, UNBOUND, CHANGED;

        public Component label() {
            return Component.translatable("stynastmc.keybinds.filter." + name().toLowerCase(Locale.ROOT));
        }
    }

    public boolean isEmpty() {
        return query.isBlank() && mode == Mode.ALL;
    }

    public boolean matches(KeyMapping key) {
        boolean modeOk = switch (mode) {
            case ALL -> true;
            case CONFLICTS -> conflicts(key);
            case UNBOUND -> key.isUnbound();
            case CHANGED -> !key.isDefault();
        };
        if (!modeOk) {
            return false;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        return q.isEmpty()
                || Component.translatable(key.getName()).getString().toLowerCase(Locale.ROOT).contains(q)
                || key.getTranslatedKeyMessage().getString().toLowerCase(Locale.ROOT).contains(q)
                || key.getCategory().label().getString().toLowerCase(Locale.ROOT).contains(q);
    }

    private static boolean conflicts(KeyMapping key) {
        if (key.isUnbound()) {
            return false;
        }
        for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
            if (other != key && key.same(other)) {
                return true;
            }
        }
        return false;
    }

    /** Implemented on KeyBindsList by mixin. */
    public interface Target {
        void stynastmc$filter(KeyBindFilter filter);
    }
}
