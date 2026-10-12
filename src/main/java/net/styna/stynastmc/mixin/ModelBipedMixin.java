package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.styna.stynastmc.firstperson.FirstPerson;

/** Raises the body's arms into view while looking down in dynamic mode. Armor models get the same pose. */
@Mixin(ModelBiped.class)
public class ModelBipedMixin {
    @Shadow public ModelRenderer bipedRightArm;
    @Shadow public ModelRenderer bipedLeftArm;

    @Inject(method = "setRotationAngles", at = @At("TAIL"))
    private void stynastmc$armOffset(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
            float scaleFactor, Entity entity, CallbackInfo ci) {
        if (FirstPerson.renderingPlayer && FirstPerson.armOffset != 0 && entity == Minecraft.getMinecraft().player) {
            bipedRightArm.rotateAngleX += FirstPerson.armOffset;
            bipedLeftArm.rotateAngleX += FirstPerson.armOffset;
        }
    }
}
