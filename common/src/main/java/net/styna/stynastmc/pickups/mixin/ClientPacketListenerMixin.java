package net.styna.stynastmc.pickups.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.styna.stynastmc.pickups.PickupNotifications;
import net.styna.stynastmc.pickups.PickupTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    // HEAD also runs on the network thread before vanilla reschedules, so only act on the main thread.
    @Inject(method = "handleTakeItemEntity", at = @At("HEAD"))
    private void stynastmc$pickup(ClientboundTakeItemEntityPacket packet, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            return;
        }
        PickupTracker.taken(packet.getItemId());
        if (minecraft.player != null && minecraft.level != null && packet.getPlayerId() == minecraft.player.getId()) {
            PickupNotifications.onPickup(minecraft.level.getEntity(packet.getItemId()), packet.getAmount());
        }
    }
}
