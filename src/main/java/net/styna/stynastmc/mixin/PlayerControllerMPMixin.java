package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.styna.stynastmc.freecam.Freecam;

/** From the camera the player is just another entity; never hit or use yourself. */
@Mixin(PlayerControllerMP.class)
public class PlayerControllerMPMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void stynastmc$noSelfAttack(EntityPlayer player, Entity target, CallbackInfo ci) {
        if (Freecam.isEnabled() && target == player) {
            ci.cancel();
        }
    }

    @Inject(method = "interactWithEntity(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/EnumHand;)Lnet/minecraft/util/EnumActionResult;",
            at = @At("HEAD"), cancellable = true)
    private void stynastmc$noSelfUse(EntityPlayer player, Entity target, net.minecraft.util.EnumHand hand, CallbackInfoReturnable<EnumActionResult> cir) {
        if (Freecam.isEnabled() && target == player) {
            cir.setReturnValue(EnumActionResult.PASS);
        }
    }

    @Inject(method = "interactWithEntity(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/RayTraceResult;Lnet/minecraft/util/EnumHand;)Lnet/minecraft/util/EnumActionResult;",
            at = @At("HEAD"), cancellable = true)
    private void stynastmc$noSelfUseAt(EntityPlayer player, Entity target, net.minecraft.util.math.RayTraceResult ray, net.minecraft.util.EnumHand hand,
            CallbackInfoReturnable<EnumActionResult> cir) {
        if (Freecam.isEnabled() && target == player) {
            cir.setReturnValue(EnumActionResult.PASS);
        }
    }
}
