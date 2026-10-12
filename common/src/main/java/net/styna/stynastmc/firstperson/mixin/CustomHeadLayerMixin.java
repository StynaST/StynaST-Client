package net.styna.stynastmc.firstperson.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.styna.stynastmc.firstperson.RenderStateAccess;

@Mixin(value = CustomHeadLayer.class, priority = 100)
public class CustomHeadLayerMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void stynastmc$hideHead(PoseStack poseStack, SubmitNodeCollector collector, int light, LivingEntityRenderState state,
            float yRot, float xRot, CallbackInfo ci) {
        if (((RenderStateAccess) state).stynastmc$isCameraEntity()) {
            ci.cancel();
        }
    }
}
