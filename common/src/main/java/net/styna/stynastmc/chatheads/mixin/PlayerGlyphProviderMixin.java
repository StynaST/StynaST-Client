/*
 * Ported from Chat Heads by dzwdz (https://github.com/dzwdz/chat_heads).
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.styna.stynastmc.chatheads.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.font.GlyphInfo;
import net.styna.stynastmc.chatheads.PaddedChatGlyph;
import net.minecraft.client.gui.font.PlayerGlyphProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PlayerGlyphProvider.class, priority = 500)
public abstract class PlayerGlyphProviderMixin {
    @ModifyExpressionValue(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/font/GlyphInfo;simple(F)Lcom/mojang/blaze3d/font/GlyphInfo;"
            )
    )
    private static GlyphInfo chatheads$addPaddingInsideChat(GlyphInfo original) {
        return new PaddedChatGlyph(original);
    }
}
