package net.styna.stynastmc.firstperson.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.styna.stynastmc.firstperson.FirstPerson;

/** Starts the fishing line at the body's hand instead of the vanilla first person hand. */
@Mixin(FishingHookRenderer.class)
public class FishingHookRendererMixin {
    private static boolean stynastmc$fromBody(Player owner) {
        return owner == Minecraft.getInstance().player && FirstPerson.shouldApply() && !FirstPerson.hideArmsAndItems(owner);
    }

    @Redirect(method = "getPlayerHandPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;"))
    private CameraType stynastmc$cameraType(Options options, Player owner, float swing, float partialTicks) {
        return stynastmc$fromBody(owner) ? CameraType.THIRD_PERSON_BACK : options.getCameraType();
    }

    @Inject(method = "getPlayerHandPos", at = @At("RETURN"), cancellable = true)
    private void stynastmc$offsetHand(Player owner, float swing, float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        if (stynastmc$fromBody(owner)) {
            cir.setReturnValue(cir.getReturnValue().add(FirstPerson.offset()));
        }
    }
}
