/*
 * Ported from Chat Heads by dzwdz (https://github.com/dzwdz/chat_heads).
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.styna.stynastmc.chatheads.mixin;

import net.styna.stynastmc.chatheads.ChatHeads;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static net.styna.stynastmc.chatheads.ChatHeads.CONFIG;

// implements 3Dness. these methods are called from GuiRenderer.render()
@Mixin(targets = "net.minecraft.client.gui.font.PlayerGlyphProvider$Instance")
public abstract class PlayerGlyphProviderInstanceMixin {
    // move head to the right
    @ModifyArg(method = "renderSprite",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/PlayerGlyphProvider$Instance;renderQuad(Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IFFFFFIFFIIII)V",
                    ordinal = 0
            ),
            index = 3
    )
    public float leftHead(float original) {
        if (!ChatHeads.customHeadRendering)
            return original;

        return original + CONFIG.threeDeeNess();
    }

    // move head to the right
    @ModifyArg(method = "renderSprite",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/PlayerGlyphProvider$Instance;renderQuad(Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IFFFFFIFFIIII)V",
                    ordinal = 0
            ),
            index = 4
    )
    public float rightHead(float original) {
        if (!ChatHeads.customHeadRendering)
            return original;

        return original + CONFIG.threeDeeNess();
    }

    // scale hat wider
    @ModifyArg(method = "renderSprite",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/PlayerGlyphProvider$Instance;renderQuad(Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IFFFFFIFFIIII)V",
                    ordinal = 1
            ),
            index = 4
    )
    public float rightHat(float original) {
        if (!ChatHeads.customHeadRendering)
            return original;

        return original + 2 * CONFIG.threeDeeNess(); // 2 times because the head is moved to the right
    }

    // move hat up
    @ModifyArg(method = "renderSprite",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/PlayerGlyphProvider$Instance;renderQuad(Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IFFFFFIFFIIII)V",
                    ordinal = 1
            ),
            index = 5
    )
    public float topHat(float original) {
        if (!ChatHeads.customHeadRendering)
            return original;

        return original - CONFIG.threeDeeNess();
    }

    // scale hat taller
    @ModifyArg(method = "renderSprite",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/PlayerGlyphProvider$Instance;renderQuad(Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IFFFFFIFFIIII)V",
                    ordinal = 1
            ),
            index = 6
    )
    public float bottomHat(float original) {
        if (!ChatHeads.customHeadRendering)
            return original;

        return original + CONFIG.threeDeeNess();
    }
}
