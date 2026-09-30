package net.styna.stynastmc.tweaks.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.styna.stynastmc.tweaks.TweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void stynastmc$noBobbing(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (TweaksConfig.on(TweaksConfig.NO_BOBBING)) {
            ci.cancel();
        }
    }
}
