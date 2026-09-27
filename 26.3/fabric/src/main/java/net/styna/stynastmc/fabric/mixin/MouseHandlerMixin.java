package net.styna.stynastmc.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.MouseHandler;
import net.styna.stynastmc.zoom.Zoom;

/** Zoom hooks that NeoForge exposes as events. */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ScrollWheelHandler;onMouseScroll(DD)Lorg/joml/Vector2i;"),
            cancellable = true)
    private void stynastmc$zoomScroll(long window, double scrollX, double scrollY, CallbackInfo ci) {
        if (Zoom.onScroll(scrollY)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "turnPlayer", at = @At(value = "INVOKE", target = "Ljava/lang/Double;doubleValue()D", ordinal = 0))
    private double stynastmc$zoomSensitivity(double sensitivity) {
        return Zoom.sensitivity(sensitivity);
    }

    @ModifyExpressionValue(method = "turnPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;smoothCamera:Z"))
    private boolean stynastmc$zoomCinematic(boolean smoothCamera) {
        return smoothCamera || Zoom.cinematicCamera();
    }
}
