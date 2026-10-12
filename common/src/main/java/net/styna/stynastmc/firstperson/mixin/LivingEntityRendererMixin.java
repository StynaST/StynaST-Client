package net.styna.stynastmc.firstperson.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.styna.stynastmc.firstperson.FirstPerson;
import net.styna.stynastmc.firstperson.RenderStateAccess;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    // Only states we extract ourselves are marked, so GUIs and other captures keep the head.
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("HEAD"))
    private void stynastmc$markCameraEntity(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
        RenderStateAccess access = (RenderStateAccess) state;
        access.stynastmc$setCameraEntity(FirstPerson.renderingPlayer);
        access.stynastmc$setHideArms(false);
        access.stynastmc$setHideBody(false);
        access.stynastmc$setArmOffset(0);
    }
}
