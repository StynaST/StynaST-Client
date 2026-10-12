package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import net.styna.stynastmc.capes.Capes;
import net.styna.stynastmc.skins.SkinsBridge;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
    @Inject(method = "getLocationSkin()Lnet/minecraft/util/ResourceLocation;", at = @At("RETURN"), cancellable = true)
    private void stynastmc$customSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation custom = SkinsBridge.texture(((AbstractClientPlayer) (Object) this).getUniqueID());
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }

    @Inject(method = "getSkinType", at = @At("RETURN"), cancellable = true)
    private void stynastmc$customModel(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(SkinsBridge.model(((AbstractClientPlayer) (Object) this).getUniqueID(), cir.getReturnValue()));
    }

    @Inject(method = "getLocationCape", at = @At("RETURN"), cancellable = true)
    private void stynastmc$ownCape(CallbackInfoReturnable<ResourceLocation> cir) {
        cir.setReturnValue(Capes.cape(((AbstractClientPlayer) (Object) this).getUniqueID(), cir.getReturnValue()));
    }

    @Inject(method = "getLocationElytra", at = @At("RETURN"), cancellable = true)
    private void stynastmc$ownElytra(CallbackInfoReturnable<ResourceLocation> cir) {
        cir.setReturnValue(Capes.elytra(((AbstractClientPlayer) (Object) this).getUniqueID(), cir.getReturnValue()));
    }
}
