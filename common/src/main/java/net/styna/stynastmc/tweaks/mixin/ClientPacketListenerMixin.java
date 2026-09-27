package net.styna.stynastmc.tweaks.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.styna.stynastmc.stats.StatsViewScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleAwardStats", at = @At("TAIL"))
    private void stynastmc$statsUpdated(ClientboundAwardStatsPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().gui.screen() instanceof StatsViewScreen screen) {
            screen.onStatsUpdated();
        }
    }
}
