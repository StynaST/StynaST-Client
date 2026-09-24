package net.styna.stynastmc.fonts.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.font.FontManager;
import net.styna.stynastmc.fonts.FontRuntime;

@Mixin(FontManager.class)
public class FontManagerMixin {
    @Inject(method = "apply", at = @At("TAIL"))
    private void stynastmc$afterApply(CallbackInfo ci) {
        FontRuntime.onFontsReloaded((FontManager) (Object) this);
    }
}
