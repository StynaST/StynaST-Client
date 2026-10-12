package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.achievement.GuiStats;

@Mixin(GuiStats.class)
public interface GuiStatsAccessor {
    @Accessor("parentScreen")
    GuiScreen stynastmc$parent();
}
