package net.styna.stynastmc.skins.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import net.styna.stynastmc.skins.CustomSkins;
import net.styna.stynastmc.skins.RemoteSkins;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void stynastmc$customSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (player == Minecraft.getInstance().player) {
            cir.setReturnValue(CustomSkins.apply(player.getUUID(), cir.getReturnValue()));
        } else {
            cir.setReturnValue(RemoteSkins.apply(player.getUUID(), cir.getReturnValue()));
        }
    }
}
