package net.styna.stynastmc.tweaks.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.styna.stynastmc.stats.StatsViewScreen;
import net.styna.stynastmc.tweaks.TweaksConfig;
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
    private void stynastmc$statsScreen(@Nullable Screen screen, CallbackInfo ci) {
        if (screen != null && screen.getClass() == StatsScreen.class && TweaksConfig.on(TweaksConfig.STATS_SCREEN)) {
            ci.cancel();
            setScreen(new StatsViewScreen(((StatsScreenAccessor) screen).stynastmc$lastScreen()));
        }
    }
}
