package net.styna.stynastmc.tweaks.mixin;

import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DarknessFogEnvironment.class)
public abstract class DarknessFogEnvironmentMixin {
    @Inject(method = "getModifiedDarkness", at = @At("HEAD"), cancellable = true)
    private void stynastmc$keepDarkness(LivingEntity entity, float darkness, float partialTick, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(darkness);
    }
}
