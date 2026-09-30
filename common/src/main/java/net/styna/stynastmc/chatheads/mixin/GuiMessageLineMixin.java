/*
 * Ported from Chat Heads by dzwdz (https://github.com/dzwdz/chat_heads).
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.styna.stynastmc.chatheads.mixin;

import net.styna.stynastmc.chatheads.ChatHeads;
import net.styna.stynastmc.chatheads.HeadData;
import net.styna.stynastmc.chatheads.mixininterface.HeadRenderable;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMessage.Line.class)
public abstract class GuiMessageLineMixin implements HeadRenderable {
    @Unique @NotNull
    public HeadData chatheads$headData = HeadData.EMPTY;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void chatheads$setOwnerForFirstLine(CallbackInfo callbackInfo) {
        chatheads$headData = ChatHeads.getLineData();
        ChatHeads.setLineData(HeadData.EMPTY); // reset early so multi-line chats don't each receive a chat head
    }

    @Override @NotNull
    public HeadData chatheads$getHeadData() {
        return chatheads$headData;
    }
}
