package net.styna.stynastmc.freecam.mixins;

import net.minecraft.client.renderer.Lightmap;
import net.styna.stynastmc.freecam.Freecam;
import net.styna.stynastmc.freecam.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.state.LightmapRenderState;

@Mixin(Lightmap.class)
public class FullBrightMixin {

    @WrapOperation(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/LightmapRenderState;brightness:F"))
    private float getBrightness(LightmapRenderState instance, Operation<Float> original) {
        if (Freecam.isEnabled() && ModConfig.get().isFullBrightEnabled()) {
            return 16.0f;
        }
        return original.call(instance);
    }
}
