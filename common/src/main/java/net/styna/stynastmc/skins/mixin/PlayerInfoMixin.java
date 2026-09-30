package net.styna.stynastmc.skins.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import net.styna.stynastmc.skins.SkinsBridge;

/** Tab list and line chat heads read skins from here. */
@Mixin(PlayerInfo.class)
public class PlayerInfoMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void stynastmc$customSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerInfo info = (PlayerInfo) (Object) this;
        cir.setReturnValue(SkinsBridge.forPlayer(info.getProfile().id(), cir.getReturnValue()));
    }
}
