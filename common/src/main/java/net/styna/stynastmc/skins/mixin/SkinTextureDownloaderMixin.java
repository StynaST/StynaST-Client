package net.styna.stynastmc.skins.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.styna.stynastmc.skins.SkinImages;

/** Vanilla throws away anything that is not 64x64 or 64x32; HD multiples of that layout are fine to keep. */
@Mixin(SkinTextureDownloader.class)
public class SkinTextureDownloaderMixin {
    @Inject(method = "processLegacySkin", at = @At("HEAD"), cancellable = true)
    private static void stynastmc$allowHdSkins(NativeImage image, String url, CallbackInfoReturnable<NativeImage> cir) {
        if (image.getWidth() > 64 && SkinImages.isSupported(image.getWidth(), image.getHeight())) {
            cir.setReturnValue(SkinImages.process(image));
        }
    }
}
