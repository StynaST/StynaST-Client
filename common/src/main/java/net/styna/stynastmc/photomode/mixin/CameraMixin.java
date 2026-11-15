package net.styna.stynastmc.photomode.mixin;

import net.minecraft.client.Camera;
import net.styna.stynastmc.photomode.PhotoMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void stynastmc$photoCamera(float partialTicks, CallbackInfo ci) {
        if (PhotoMode.isActive()) {
            PhotoMode.stepFrame();
            setRotation(PhotoMode.yaw(), PhotoMode.pitch());
            setPosition(PhotoMode.x(), PhotoMode.y(), PhotoMode.z());
        }
    }
}
