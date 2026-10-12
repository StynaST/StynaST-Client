package net.styna.stynastmc.firstperson.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.styna.stynastmc.firstperson.RenderStateAccess;

/** Hides the head and, as configured, the arms and body of the first person model. Also applies to armor models. */
@Mixin(HumanoidModel.class)
public class HumanoidModelMixin {
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart body;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightArm;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void stynastmc$hideParts(HumanoidRenderState state, CallbackInfo ci) {
        RenderStateAccess access = (RenderStateAccess) state;
        // Models are shared between entities, so always write the flags back.
        head.visible = !access.stynastmc$isCameraEntity();
        if (state instanceof AvatarRenderState avatar && avatar.isSpectator) {
            return;
        }
        leftArm.visible = !access.stynastmc$hideLeftArm();
        rightArm.visible = !access.stynastmc$hideRightArm();
        leftArm.xRot += access.stynastmc$armOffset();
        rightArm.xRot += access.stynastmc$armOffset();
        body.visible = !access.stynastmc$hideBody();
    }
}
