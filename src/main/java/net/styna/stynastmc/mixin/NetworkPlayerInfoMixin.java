package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import net.styna.stynastmc.skins.SkinsBridge;

/** Tab list heads and anything else reading skins through the player list. */
@Mixin(NetworkPlayerInfo.class)
public class NetworkPlayerInfoMixin {
    @Inject(method = "getLocationSkin", at = @At("RETURN"), cancellable = true)
    private void stynastmc$customSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation custom = SkinsBridge.texture(((NetworkPlayerInfo) (Object) this).getGameProfile().getId());
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }

    @Inject(method = "getSkinType", at = @At("RETURN"), cancellable = true)
    private void stynastmc$customModel(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(SkinsBridge.model(((NetworkPlayerInfo) (Object) this).getGameProfile().getId(), cir.getReturnValue()));
    }
}
