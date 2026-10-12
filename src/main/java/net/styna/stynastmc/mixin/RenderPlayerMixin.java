package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.styna.stynastmc.freecam.Freecam;

/** Vanilla only draws the local player when it is the camera; freecam needs it drawn from outside. */
@Mixin(RenderPlayer.class)
public class RenderPlayerMixin {
    @Redirect(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;isUser()Z"))
    private boolean stynastmc$drawSelf(AbstractClientPlayer player) {
        return player.isUser() && !Freecam.isEnabled();
    }
}
