package net.styna.stynastmc.classicbar.util;

import net.minecraft.resources.Identifier;

public enum HealthEffect {
    NONE(""),
    POISON("poisoned_"),
    WITHER("withered_"),
    FROZEN("frozen_");

    private final String spritePrefix;

    HealthEffect(String spritePrefix) {
        this.spritePrefix = spritePrefix;
    }

    public Identifier heartSprite(boolean hardcore) {
        return Identifier.withDefaultNamespace("hud/heart/" + spritePrefix + (hardcore ? "hardcore_" : "") + "full");
    }
}
