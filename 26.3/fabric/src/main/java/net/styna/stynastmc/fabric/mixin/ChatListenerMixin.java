package net.styna.stynastmc.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import net.styna.stynastmc.clearspam.ClearSpam;

/** Player chat for ClearSpam; Fabric API only allows modifying system messages. */
@Mixin(ChatListener.class)
public class ChatListenerMixin {
    @ModifyArg(method = {"showMessageToPlayer", "lambda$handleDisguisedChatMessage$0"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V"),
            index = 0)
    private Component stynastmc$clearSpam(Component message) {
        return ClearSpam.onChat(message, false);
    }
}
