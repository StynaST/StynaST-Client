package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.styna.stynastmc.freecam.Freecam;

@Mixin(Entity.class)
public class EntityMixin {
    // Makes mouse input rotate the camera instead of the player.
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void stynastmc$turnCamera(float yaw, float pitch, CallbackInfo ci) {
        if (Freecam.controlsCamera() && (Object) this == Minecraft.getMinecraft().player) {
            Freecam.getFreeCamera().turn(yaw, pitch);
            ci.cancel();
        }
    }

    // Freezes the player in place when freeze_player is on.
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void stynastmc$freeze(MoverType type, double x, double y, double z, CallbackInfo ci) {
        if (Freecam.freezesPlayer() && (Object) this == Minecraft.getMinecraft().player) {
            ci.cancel();
        }
    }

    // Outlines the player with the glowing effect while viewed from the camera.
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void stynastmc$outline(CallbackInfoReturnable<Boolean> cir) {
        if (Freecam.isOutlineEnabled() && (Object) this == Minecraft.getMinecraft().player) {
            cir.setReturnValue(true);
        }
    }
}
