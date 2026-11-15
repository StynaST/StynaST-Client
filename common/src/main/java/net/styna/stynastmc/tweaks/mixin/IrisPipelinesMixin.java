package net.styna.stynastmc.tweaks.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.styna.stynastmc.compat.MahouIrisCompat;

/** First return of getPipeline is the shadow pass, second the main pass. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.IrisPipelines", remap = false)
public abstract class IrisPipelinesMixin {
    @Inject(method = "getPipeline", at = @At(value = "RETURN", ordinal = 0), cancellable = true, require = 0)
    private static void stynastmc$mahouShadow(@Coerce Object iris, RenderPipeline pipeline, CallbackInfoReturnable<Object> cir) {
        stynastmc$assign(pipeline, true, cir);
    }

    @Inject(method = "getPipeline", at = @At(value = "RETURN", ordinal = 1), cancellable = true, require = 0)
    private static void stynastmc$mahouMain(@Coerce Object iris, RenderPipeline pipeline, CallbackInfoReturnable<Object> cir) {
        stynastmc$assign(pipeline, false, cir);
    }

    private static void stynastmc$assign(RenderPipeline pipeline, boolean shadow, CallbackInfoReturnable<Object> cir) {
        if (cir.getReturnValue() == null) {
            Object key = MahouIrisCompat.shaderKey(pipeline, shadow);
            if (key != null) {
                cir.setReturnValue(key);
            }
        }
    }
}
