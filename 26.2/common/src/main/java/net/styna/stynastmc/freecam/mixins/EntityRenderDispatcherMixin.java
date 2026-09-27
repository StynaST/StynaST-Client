package net.styna.stynastmc.freecam.mixins;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.styna.stynastmc.freecam.Freecam;
import net.styna.stynastmc.freecam.config.ModConfig;
import net.styna.stynastmc.freecam.util.FreeCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.styna.stynastmc.freecam.Freecam.MC;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    // Prevents shadow being cast when Iris is enabled.
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void onShouldRender(
        Entity entity,
        Frustum culler,
        double camX,
        double camY,
        double camZ,
        CallbackInfoReturnable<Boolean> cir)
    {
        if (entity instanceof FreeCamera) {
            cir.setReturnValue(false);
        } else if (entity == MC.player && Freecam.isEnabled() && ModConfig.get().shouldHidePlayer()) {
            cir.setReturnValue(false);
        }
    }
}
