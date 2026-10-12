package net.styna.stynastmc.firstperson.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.styna.stynastmc.firstperson.FirstPerson;
import net.styna.stynastmc.firstperson.FirstPersonConfig;
import net.styna.stynastmc.firstperson.FirstPersonConfig.VanillaHands;

/** Hides or reshapes the vanilla first person hands. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {
    @Shadow
    protected abstract void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector collector, int light, float inverseArmHeight,
            float attack, HumanoidArm arm, PlayerRenderState playerState);

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void stynastmc$vanillaHands(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks,
            float xRot, InteractionHand hand, float attack, ItemStack item, float inverseArmHeight, PoseStack poseStack,
            SubmitNodeCollector collector, int light, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !FirstPerson.shouldApply()) {
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
            renderPlayerArm(poseStack, collector, light, inverseArmHeight, attack, player.getMainArm().getOpposite(), playerState);
            poseStack.popPose();
        }
    }
}
