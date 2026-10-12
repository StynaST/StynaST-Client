package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.styna.stynastmc.freecam.Freecam;

/** Blocks attacking, using, picking and mining while freecam has the controls, unless allow_interact is on. */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    private void stynastmc$noAttack(CallbackInfo ci) {
        if (Freecam.blocksInteraction()) {
            ci.cancel();
        }
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"), cancellable = true)
    private void stynastmc$noUse(CallbackInfo ci) {
        if (Freecam.blocksInteraction()) {
            ci.cancel();
        }
    }

    @Inject(method = "middleClickMouse", at = @At("HEAD"), cancellable = true)
    private void stynastmc$noPick(CallbackInfo ci) {
        if (Freecam.blocksInteraction()) {
            ci.cancel();
        }
    }

    @Inject(method = "sendClickBlockToController", at = @At("HEAD"), cancellable = true)
    private void stynastmc$noMining(boolean leftClick, CallbackInfo ci) {
        if (Freecam.blocksInteraction()) {
            ci.cancel();
        }
    }
}
