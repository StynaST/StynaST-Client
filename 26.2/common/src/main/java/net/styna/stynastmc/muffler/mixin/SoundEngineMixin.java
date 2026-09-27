package net.styna.stynastmc.muffler.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.styna.stynastmc.muffler.Muffler;

/** Scales every sound's volume, both when it starts and when playing sounds are updated. */
@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    @Inject(method = "play", at = @At("HEAD"))
    private void stynastmc$record(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        Muffler.onPlaySound(instance);
    }

    @WrapOperation(method = {"play", "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getVolume()F"))
    private float stynastmc$muffle(SoundInstance instance, Operation<Float> original) {
        return original.call(instance) * Muffler.factor(instance);
    }
}
