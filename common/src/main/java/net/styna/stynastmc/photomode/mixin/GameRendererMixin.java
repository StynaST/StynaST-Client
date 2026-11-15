package net.styna.stynastmc.photomode.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.styna.stynastmc.photomode.PhotoMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = {"bobHurt", "bobView"}, at = @At("HEAD"), cancellable = true)
    private void stynastmc$steadyCamera(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (PhotoMode.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "tryTakeScreenshotIfNeeded", at = @At("HEAD"))
    private void stynastmc$photoShot(CallbackInfo ci) {
        PhotoMode.afterLevel();
    }
}
