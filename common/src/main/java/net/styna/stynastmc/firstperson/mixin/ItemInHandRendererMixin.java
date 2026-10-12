package net.styna.stynastmc.firstperson.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.styna.stynastmc.firstperson.FirstPerson;
import net.styna.stynastmc.firstperson.FirstPersonConfig;
import net.styna.stynastmc.firstperson.FirstPersonConfig.VanillaHands;

/** Hides or reshapes the vanilla first person hands. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow private float mainHandHeight;
    @Shadow private float offHandHeight;
    @Shadow private ItemStack mainHandItem;
    @Shadow private ItemStack offHandItem;

    @Shadow
    protected abstract void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector collector, int light, float inverseArmHeight,
            float attack, HumanoidArm arm);

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void stynastmc$vanillaHands(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack,
            ItemStack item, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (!FirstPerson.shouldApply()) {
            return;
        }
        if (!FirstPerson.showVanillaHands()
                || FirstPersonConfig.SKIP_SWIMMING.get() && player.isSwimming()
                || FirstPerson.dynamicHandsEnabled() && xRot > 35) {
            ci.cancel();
            return;
        }
        if (FirstPersonConfig.VANILLA_HANDS.get() != VanillaHands.ALL_DOUBLE || player.getMainHandItem().is(Items.FILLED_MAP)) {
            return;
        }
        // Vanilla only draws the empty main arm, ALL_DOUBLE adds the empty off arm too.
        if (item.isEmpty() && hand == InteractionHand.OFF_HAND && !player.isInvisible()) {
            poseStack.pushPose();
            renderPlayerArm(poseStack, collector, light, inverseArmHeight, attack, player.getMainArm().getOpposite());
            poseStack.popPose();
        }
    }

    // Dynamic mode lowers the vanilla hands as you look down, until the body's arms take over.
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"), cancellable = true)
    private void stynastmc$lowerHands(CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!FirstPerson.shouldApply() || !FirstPerson.showVanillaHands() || !FirstPerson.dynamicHandsEnabled()) {
            return;
        }
        float xRot = player.getXRot();
        if (xRot <= 15) {
            return;
        }
        if (xRot < 30) {
            mainHandHeight = 15 / xRot;
            offHandHeight = 15 / xRot;
        } else {
            mainHandHeight -= mainHandHeight > -0.1f ? 0.15f : 0;
            offHandHeight -= offHandHeight > -0.1f ? 0.15f : 0;
        }
        mainHandItem = player.getMainHandItem();
        offHandItem = player.getOffhandItem();
        ci.cancel();
    }
}
