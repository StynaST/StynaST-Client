package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.styna.stynastmc.firstperson.FirstPerson;

@Mixin(LayerBipedArmor.class)
public class LayerBipedArmorMixin {
    @Inject(method = "setModelSlotVisible", at = @At("TAIL"))
    private void stynastmc$hideArmorParts(ModelBiped model, EntityEquipmentSlot slot, CallbackInfo ci) {
        if (!FirstPerson.renderingPlayer) {
            return;
        }
        model.bipedHead.showModel = false;
        model.bipedHeadwear.showModel = false;
        if (FirstPerson.hideArms) {
            model.bipedRightArm.showModel = false;
            model.bipedLeftArm.showModel = false;
        }
    }
}
