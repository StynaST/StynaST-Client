package net.styna.stynastmc.zoom.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;
import net.styna.stynastmc.zoom.Zoom;

/** Hooks the world FOV only; the hand uses calculateHudFov and stays unzoomed. */
@Mixin(Camera.class)
public class CameraMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void stynastmc$zoom(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Zoom.apply(cir.getReturnValueF()));
    }
}
