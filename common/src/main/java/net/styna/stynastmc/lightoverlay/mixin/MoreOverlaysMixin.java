package net.styna.stynastmc.lightoverlay.mixin;

import net.styna.stynastmc.lightoverlay.LightOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** More Overlays' light overlay key toggles ours instead, so theirs never turns on. */
@Pseudo
@Mixin(targets = "at.ridgo8.moreoverlays.lightoverlay.LightOverlayHandler", remap = false)
public abstract class MoreOverlaysMixin {
    // Its key always asks for true since it never enables; false only comes from level unload.
    @Inject(method = "setEnabled", at = @At("HEAD"), cancellable = true, require = 0)
    private static void stynastmc$useOurs(boolean enabled, CallbackInfo ci) {
        ci.cancel();
        if (enabled) {
            LightOverlay.toggle();
        }
    }
}
