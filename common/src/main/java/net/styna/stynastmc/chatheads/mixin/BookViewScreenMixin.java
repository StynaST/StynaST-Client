/*
 * Ported from Chat Heads by dzwdz (https://github.com/dzwdz/chat_heads).
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.styna.stynastmc.chatheads.mixin;

import net.styna.stynastmc.chatheads.ChatHeads;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// custom rendering inside books wasn't planned but there is no easy way to *not* do 3Dness there, so this will also enable the offsetting
@Mixin(BookViewScreen.class)
public abstract class BookViewScreenMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    public void chatheads$isInsideBook(CallbackInfo ci) {
        ChatHeads.customHeadRendering = true;
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    public void chatheads$isOutsideBook(CallbackInfo ci) {
        ChatHeads.customHeadRendering = false;
    }
}
