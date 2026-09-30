/*
 * Ported from Chat Heads by dzwdz (https://github.com/dzwdz/chat_heads).
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.styna.stynastmc.chatheads.mixin;

import net.styna.stynastmc.chatheads.ChatHeads;
import net.styna.stynastmc.chatheads.ComponentProcessor;
import net.styna.stynastmc.chatheads.HeadData;
import net.styna.stynastmc.chatheads.config.RenderPosition;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// needs to apply after Compact Chat's refreshTrimmedMessage() and recursive addMessage() call
// also after Chat Timestamps so the head position is correct
@Mixin(value = ChatComponent.class, priority = 10100)
public abstract class ChatComponentMixin2 {
    @ModifyArg(
            method = "addMessage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/chat/GuiMessage;<init>(ILnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V"
            ),
            index = 1
    )
    private Component chatheads$prependChatHead(Component contents) {
        if (ChatHeads.CONFIG.renderPosition() == RenderPosition.BEFORE_LINE && ChatHeads.lastSenderData != HeadData.EMPTY) {
            return ComponentProcessor.prependChatHead(contents, ChatHeads.lastSenderData.playerInfo());
        }

        return contents;
    }

    @Inject(
            method = "addMessageToDisplayQueue",
            at = @At("HEAD")
    )
    private void chatheads$transferMessageOwner(GuiMessage guiMessage, CallbackInfo ci) {
        ChatHeads.lineData = ChatHeads.lastSenderData; // only really need to set when not refreshing
    }

    @ModifyArg(
            method = "addMessage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessageToDisplayQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V"
            )
    )
    private GuiMessage chatheads$setOwner(GuiMessage message) {
        ChatHeads.setHeadData(message, ChatHeads.lastSenderData);
        return message;
    }

    // just in case the GuiMessage isn't the same
    @ModifyArg(
            method = "addMessage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessageToQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V"
            )
    )
    private GuiMessage chatheads$setOwner2(GuiMessage message) {
        ChatHeads.setHeadData(message, ChatHeads.lastSenderData);
        return message;
    }

    @Inject(
            method = "addMessage",
            at = @At("RETURN")
    )
    private void chatheads$forgetSender(CallbackInfo ci) {
        ChatHeads.lastSenderData = HeadData.EMPTY;
    }
}
