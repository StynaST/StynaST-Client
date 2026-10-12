package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.styna.stynastmc.firstperson.FirstPerson;

/** Hides the head and, when vanilla hands take over, the arms of the first person body. */
@Mixin(RenderPlayer.class)
public abstract class RenderPlayerFirstPersonMixin {
    @Inject(method = "setModelVisibilities", at = @At("TAIL"))
    private void stynastmc$firstPersonParts(AbstractClientPlayer player, CallbackInfo ci) {
        if (!FirstPerson.renderingPlayer) {
            return;
        }
        ModelPlayer model = ((RenderPlayer) (Object) this).getMainModel();
        model.bipedHead.showModel = false;
        model.bipedHeadwear.showModel = false;
        if (FirstPerson.hideArms) {
            model.bipedLeftArm.showModel = false;
            model.bipedRightArm.showModel = false;
            model.bipedLeftArmwear.showModel = false;
            model.bipedRightArmwear.showModel = false;
        }
    }
}
