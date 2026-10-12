package net.styna.stynastmc.firstperson.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.StuckInBodyLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.styna.stynastmc.firstperson.FirstPersonConfig;
import net.styna.stynastmc.firstperson.RenderStateAccess;

@Mixin(StuckInBodyLayer.class)
public class StuckInBodyLayerMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void stynastmc$hideStuck(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state,
            float yRot, float xRot, CallbackInfo ci) {
        if (((RenderStateAccess) state).stynastmc$isCameraEntity() && !FirstPersonConfig.RENDER_STUCK_FEATURES.get()) {
            ci.cancel();
        }
    }
}
