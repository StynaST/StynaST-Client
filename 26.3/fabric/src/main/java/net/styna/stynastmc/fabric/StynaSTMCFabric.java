package net.styna.stynastmc.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.styna.stynastmc.classicbar.network.ExhaustionSyncPayload;
import net.styna.stynastmc.classicbar.network.SyncHandler;

public final class StynaSTMCFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(ExhaustionSyncPayload.TYPE, ExhaustionSyncPayload.STREAM_CODEC);
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(SyncHandler::onPlayerTick));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SyncHandler.onPlayerLoggedOut(handler.player.getUUID()));
    }
}
