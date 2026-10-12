package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundManager;
import net.styna.stynastmc.muffler.Muffler;

/** Scales every sound's volume, both when it starts and when playing sounds are updated. */
@Mixin(SoundManager.class)
public class SoundManagerMixin {
    @Inject(method = "getClampedVolume", at = @At("RETURN"), cancellable = true)
    private void stynastmc$muffle(ISound sound, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValueF() * Muffler.factor(sound));
    }
}
