package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.entity.layers.LayerArrow;
import net.minecraft.entity.EntityLivingBase;
import net.styna.stynastmc.firstperson.FirstPerson;
import net.styna.stynastmc.firstperson.FirstPersonConfig;

@Mixin(LayerArrow.class)
public class LayerArrowMixin {
    @Inject(method = "doRenderLayer", at = @At("HEAD"), cancellable = true)
    private void stynastmc$hideStuckArrows(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        if (FirstPerson.renderingPlayer && !FirstPersonConfig.RENDER_STUCK_FEATURES.get()) {
            ci.cancel();
        }
    }
}
