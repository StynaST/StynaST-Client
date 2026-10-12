package net.styna.stynastmc.skins.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import net.styna.stynastmc.capes.Capes;
import net.styna.stynastmc.skins.CustomSkins;
import net.styna.stynastmc.skins.RemoteSkins;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void stynastmc$customSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        PlayerSkin skin = player == Minecraft.getInstance().player
                ? CustomSkins.apply(player.getUUID(), cir.getReturnValue())
                : RemoteSkins.apply(player.getUUID(), cir.getReturnValue());
        cir.setReturnValue(Capes.apply(player.getGameProfile(), skin));
    }
}
