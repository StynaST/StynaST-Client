package net.styna.stynastmc.freecam.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.styna.stynastmc.freecam.Freecam;
import net.styna.stynastmc.freecam.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.styna.stynastmc.freecam.Freecam.MC;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {

    public LocalPlayerMixin(ClientLevel level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    // Needed for Baritone compatibility.
    @Inject(method = "isControlledCamera", at = @At("HEAD"), cancellable = true)
    private void onIsCamera(CallbackInfoReturnable<Boolean> cir) {
        if (Freecam.isEnabled() && freecam$this() == MC.player) {
            cir.setReturnValue(true);
        }
    }

    // Makes rotation depend upon FreeCamera rather than the player.
    @Override
    public float getViewXRot(float partialTick) {
        if (freecam$useFreecamRotation()) {
            return Freecam.getFreeCamera().getViewXRot(partialTick);
        }
        return super.getViewXRot(partialTick);
    }

    @Inject(method = "getViewYRot", at = @At("HEAD"), cancellable = true)
    private void onGetViewYRot(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (freecam$useFreecamRotation()) {
            cir.setReturnValue(Freecam.getFreeCamera().getViewYRot(partialTick));
        }
    }

    @Unique
    private boolean freecam$useFreecamRotation() {
        return Freecam.isEnabled() && !Freecam.isPlayerControlEnabled() && !ModConfig.get().allowInteractionsFromPlayer();
    }

    @Unique
    private LocalPlayer freecam$this() {
        return (LocalPlayer) (Object) this;
    }
}
