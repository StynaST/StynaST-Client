package net.styna.stynastmc.tweaks.mixin;

import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectFogEnvironment.class)
public abstract class MobEffectFogEnvironmentMixin {
    @Inject(method = "isApplicable", at = @At("HEAD"), cancellable = true)
    private void stynastmc$noDarknessFog(FogType type, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof DarknessFogEnvironment) {
            cir.setReturnValue(false);
        }
    }
}
