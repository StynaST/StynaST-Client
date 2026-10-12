package net.styna.stynastmc.skins;

/** Arm width to render the custom skin with. AUTO keeps whatever the account skin uses. */
public enum SkinModel {
    AUTO,
    WIDE,
    SLIM;

    /** Resolves to the 1.12 skin type names, "default" or "slim". */
    public String resolve(String fallback) {
        return switch (this) {
            case AUTO -> fallback;
            case WIDE -> "default";
            case SLIM -> "slim";
        };
    }
}
