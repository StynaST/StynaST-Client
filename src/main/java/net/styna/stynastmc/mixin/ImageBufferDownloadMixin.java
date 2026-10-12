package net.styna.stynastmc.mixin;

import java.awt.image.BufferedImage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.ImageBufferDownload;
import net.styna.stynastmc.skins.SkinImages;

/** Vanilla squashes every downloaded skin to 64x64; HD skins keep their resolution. */
@Mixin(ImageBufferDownload.class)
public class ImageBufferDownloadMixin {
    @Inject(method = "parseUserSkin", at = @At("HEAD"), cancellable = true)
    private void stynastmc$allowHdSkins(BufferedImage image, CallbackInfoReturnable<BufferedImage> cir) {
        if (image != null && image.getWidth() > 64 && SkinImages.isSupported(image.getWidth(), image.getHeight())) {
            cir.setReturnValue(SkinImages.process(image));
        }
    }
}
