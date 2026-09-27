package net.styna.stynastmc.freecam.mixins;

import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.styna.stynastmc.freecam.Freecam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static net.styna.stynastmc.freecam.Freecam.MC;

// Arm shading follows the FreeCamera position rather than the player position.
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class FirstPersonHandsAndItemsRendererMixin {

    @ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true)
    private int useFreeCameraLight(int lightCoords) {
        if (Freecam.isEnabled()) {
            return MC.getEntityRenderDispatcher().getPackedLightCoords(Freecam.getFreeCamera(), MC.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        }
        return lightCoords;
    }
}
