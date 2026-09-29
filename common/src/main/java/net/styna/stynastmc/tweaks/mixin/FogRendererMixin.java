package net.styna.stynastmc.tweaks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.styna.stynastmc.tweaks.TweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Unique
    private static final float STYNASTMC_FAR = 1.0E7F;
    @Unique
    private boolean stynastmc$clear;

    @WrapOperation(method = "setupFog", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/fog/environment/FogEnvironment;setupFog(Lnet/minecraft/client/renderer/fog/FogData;Lnet/minecraft/client/Camera;Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/DeltaTracker;)V"))
    private void stynastmc$noFog(FogEnvironment environment, FogData fog, Camera camera, ClientLevel level, float renderDistance,
            DeltaTracker deltaTracker, Operation<Void> original) {
        original.call(environment, fog, camera, level, renderDistance, deltaTracker);
        stynastmc$clear = environment instanceof AtmosphericFogEnvironment && TweaksConfig.on(TweaksConfig.NO_FOG);
        if (stynastmc$clear) {
            fog.environmentalStart = STYNASTMC_FAR;
            fog.environmentalEnd = STYNASTMC_FAR;
        }
    }

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void stynastmc$noDistanceFog(CallbackInfoReturnable<FogData> cir) {
        if (stynastmc$clear) {
            stynastmc$clear = false;
            FogData fog = cir.getReturnValue();
            fog.renderDistanceStart = STYNASTMC_FAR;
            fog.renderDistanceEnd = STYNASTMC_FAR;
        }
    }
}
