/*
 * Ported from Chat Heads by dzwdz (https://github.com/dzwdz/chat_heads).
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.styna.stynastmc.chatheads.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.styna.stynastmc.chatheads.ChatHeads;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ChatListener.class)
public abstract class ChatListenerMixin {
    // called after message filtering, either from handlePlayerChatMessage directly, or after some potential chat delay
    @ModifyArg(
            method = "showMessageToPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
                    ordinal = 0
            )
    )
    public Component chatheads$handleAddedPlayerMessage(Component message, @Local(argsOnly = true) PlayerChatMessage playerChatMessage) {
        // it looks like gameProfile.getId() *could* be different from the sender UUID (or null), so we use the latter instead
        return ChatHeads.handleAddedMessage(message, ChatHeads.getOwner(playerChatMessage));
    }

    // either called from handleDisguisedChatMessage directly, or after some potential chat delay
    @ModifyArg(
            method = "lambda$handleDisguisedChatMessage$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
                    ordinal = 0
            )
    )
    public Component chatheads$handleAddedDisguisedMessage(Component message) {
        return ChatHeads.handleAddedMessage(message, null);
    }

    // called for system messages
    @ModifyArg(
            method = "handleSystemMessage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;addServerSystemMessage(Lnet/minecraft/network/chat/Component;)V",
                    ordinal = 0
            )
    )
    public Component chatheads$handleAddedSystemMessage(Component message) {
        if (ChatHeads.CONFIG.handleSystemMessages())
            return ChatHeads.handleAddedMessage(message, null);

        return message;
    }
}
