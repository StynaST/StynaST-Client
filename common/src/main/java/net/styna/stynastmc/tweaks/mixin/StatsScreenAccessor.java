package net.styna.stynastmc.tweaks.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StatsScreen.class)
public interface StatsScreenAccessor {
    @Accessor("lastScreen")
    Screen stynastmc$lastScreen();
}
