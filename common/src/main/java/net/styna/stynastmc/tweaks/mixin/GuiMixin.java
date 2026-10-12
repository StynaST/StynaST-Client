package net.styna.stynastmc.tweaks.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.styna.stynastmc.advancements.AdvancementsViewScreen;
import net.styna.stynastmc.stats.StatsViewScreen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow
    public abstract void setScreen(@Nullable Screen screen);

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void stynastmc$replaceScreen(@Nullable Screen screen, CallbackInfo ci) {
        if (screen == null) {
            return;
        }
        if (screen.getClass() == StatsScreen.class) {
            ci.cancel();
            setScreen(new StatsViewScreen(((StatsScreenAccessor) screen).stynastmc$lastScreen()));
        } else if (screen.getClass() == AdvancementsScreen.class) {
            ci.cancel();
            AdvancementsScreenAccessor vanilla = (AdvancementsScreenAccessor) screen;
            setScreen(new AdvancementsViewScreen(vanilla.stynastmc$advancements(), vanilla.stynastmc$lastScreen()));
        }
    }
}
