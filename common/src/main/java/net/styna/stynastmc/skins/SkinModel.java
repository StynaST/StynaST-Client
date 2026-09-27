package net.styna.stynastmc.skins;

import net.minecraft.world.entity.player.PlayerModelType;

/** Arm width to render the custom skin with. AUTO keeps whatever the account skin uses. */
public enum SkinModel {
    AUTO,
    WIDE,
    SLIM;

    public PlayerModelType resolve(PlayerModelType fallback) {
        return switch (this) {
            case AUTO -> fallback;
            case WIDE -> PlayerModelType.WIDE;
            case SLIM -> PlayerModelType.SLIM;
        };
    }
}
