package net.styna.stynastmc.photomode.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.styna.stynastmc.photomode.PhotoMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void stynastmc$photoLook(LocalPlayer player, double dx, double dy, Operation<Void> original) {
        if (PhotoMode.isActive()) {
            PhotoMode.turn(dx, dy);
        } else {
            original.call(player, dx, dy);
        }
    }

    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ScrollWheelHandler;onMouseScroll(DD)Lorg/joml/Vector2i;"), cancellable = true)
    private void stynastmc$photoScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        if (PhotoMode.isActive()) {
            if (yoffset != 0) {
                PhotoMode.scroll(yoffset);
            }
            ci.cancel();
        }
    }
}
