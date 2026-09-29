package net.styna.stynastmc.tweaks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Iris hands shader packs the Darkness strength straight from the effect, skipping the vanilla lightmap. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.CommonUniforms", remap = false)
public abstract class IrisCommonUniformsMixin {
    @Inject(method = "getDarknessFactor", at = @At("HEAD"), cancellable = true, require = 0)
    private static void stynastmc$noDarkness(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(0.0F);
    }
}
