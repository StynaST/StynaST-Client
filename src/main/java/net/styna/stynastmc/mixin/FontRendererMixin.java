package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.FontRenderer;
import net.styna.stynastmc.fonts.FontRuntime;
import net.styna.stynastmc.fonts.TtfFont;

/** Draws and measures characters with the applied Google Font; anything it lacks stays vanilla. */
@Mixin(FontRenderer.class)
public abstract class FontRendererMixin {
    @Shadow
    protected float posX;

    @Shadow
    protected float posY;

    @Shadow
    public abstract int getCharWidth(char character);

    @Inject(method = "renderChar", at = @At("HEAD"), cancellable = true)
    private void stynastmc$renderChar(char c, boolean italic, CallbackInfoReturnable<Float> cir) {
        TtfFont font = FontRuntime.active((FontRenderer) (Object) this);
        if (font != null && font.canDisplay(c)) {
            cir.setReturnValue(font.draw(c, posX, posY, italic));
        }
    }

    @Inject(method = "getCharWidth", at = @At("HEAD"), cancellable = true)
    private void stynastmc$charWidth(char c, CallbackInfoReturnable<Integer> cir) {
        TtfFont font = FontRuntime.active((FontRenderer) (Object) this);
        if (font != null && font.canDisplay(c)) {
            cir.setReturnValue(Math.round(font.advance(c)));
        }
    }

    // Same walk as vanilla, but summing fractional advances so centered and right aligned text lines up.
    @Inject(method = "getStringWidth", at = @At("HEAD"), cancellable = true)
    private void stynastmc$stringWidth(String text, CallbackInfoReturnable<Integer> cir) {
        TtfFont font = FontRuntime.active((FontRenderer) (Object) this);
        if (font == null || text == null) {
            return;
        }
        float width = 0;
        boolean bold = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i < text.length() - 1) {
                char code = Character.toLowerCase(text.charAt(++i));
                if (code == 'l') {
                    bold = true;
                } else if (code == 'r' || "0123456789abcdef".indexOf(code) >= 0) {
                    bold = false;
                }
                continue;
            }
            float advance = font.canDisplay(c) ? font.advance(c) : getCharWidth(c);
            width += advance;
            if (bold && advance > 0) {
                width++;
            }
        }
        cir.setReturnValue((int) Math.ceil(width));
    }
}
