package net.styna.stynastmc.firstperson.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.styna.stynastmc.firstperson.FirstPerson;
import net.styna.stynastmc.firstperson.RenderStateAccess;

/** No held items on the body while the vanilla hands show them. */
@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {
    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void stynastmc$hideItems(ArmedEntityRenderState state, ItemStackRenderState item, ItemStack stack, HumanoidArm arm,
            PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        RenderStateAccess access = (RenderStateAccess) state;
        if (access.stynastmc$hideLeftArm() && access.stynastmc$hideRightArm() && !FirstPerson.lookingDown(state.xRot)) {
            ci.cancel();
        }
    }
}
