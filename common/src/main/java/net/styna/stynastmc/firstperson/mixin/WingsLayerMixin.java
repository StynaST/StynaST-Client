package net.styna.stynastmc.firstperson.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.styna.stynastmc.firstperson.RenderStateAccess;

/** Elytra would cover the camera while swimming. */
@Mixin(WingsLayer.class)
public class WingsLayerMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void stynastmc$hideWhileSwimming(PoseStack poseStack, SubmitNodeCollector collector, int light, HumanoidRenderState state,
            float yRot, float xRot, CallbackInfo ci) {
        if (((RenderStateAccess) state).stynastmc$isCameraEntity() && state.isVisuallySwimming) {
            ci.cancel();
        }
    }
}
