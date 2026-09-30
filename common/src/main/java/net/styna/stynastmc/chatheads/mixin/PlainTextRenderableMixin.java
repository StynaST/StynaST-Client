/*
 * Ported from Chat Heads by dzwdz (https://github.com/dzwdz/chat_heads).
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.styna.stynastmc.chatheads.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.styna.stynastmc.chatheads.ChatHeads;
import net.minecraft.client.gui.font.PlainTextRenderable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PlainTextRenderable.class)
public interface PlainTextRenderableMixin {
    @Shadow
    int shadowColor();

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/PlainTextRenderable;shadowColor()I"))
    default int chatheads$disableShadow(int original) {
        return (ChatHeads.customHeadRendering && !ChatHeads.CONFIG.drawShadow() ? 0 : original);
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/PlainTextRenderable;renderSprite(Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IFFFI)V",
                    ordinal = 1
            ),
            index = 4
    )
    default float chatheads$moveDownWhenShadowDisabled(float offsetY) {
        // offsetting the head looks great in chat (where the text has shadows) but looks bad in books (where there's no shadows)
        // so we just go by whether there originally was a shadow
        return (ChatHeads.customHeadRendering && !ChatHeads.CONFIG.drawShadow() && shadowColor() != 0 ? offsetY + 1 : offsetY);
    }
}
