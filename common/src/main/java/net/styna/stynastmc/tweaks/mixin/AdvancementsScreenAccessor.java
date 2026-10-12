package net.styna.stynastmc.tweaks.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AdvancementsScreen.class)
public interface AdvancementsScreenAccessor {
    @Accessor("lastScreen")
    Screen stynastmc$lastScreen();

    @Accessor("advancements")
    ClientAdvancements stynastmc$advancements();
}
