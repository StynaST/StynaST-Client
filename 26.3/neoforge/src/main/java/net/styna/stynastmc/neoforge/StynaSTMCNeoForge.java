package net.styna.stynastmc.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.classicbar.network.ClassicBarNetwork;
import net.styna.stynastmc.classicbar.network.ExhaustionSyncPayload;
import net.styna.stynastmc.classicbar.network.SyncHandler;

@Mod(StynaSTMC.MODID)
public final class StynaSTMCNeoForge {
    public StynaSTMCNeoForge(IEventBus modEventBus) {
        modEventBus.addListener(StynaSTMCNeoForge::registerPayloads);
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SyncHandler.onPlayerTick(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> SyncHandler.onPlayerLoggedOut(event.getEntity().getUUID()));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .optional()
                .playToClient(ExhaustionSyncPayload.TYPE, ExhaustionSyncPayload.STREAM_CODEC, (payload, context) -> {
                    payload.handle(context.player());
                    ClassicBarNetwork.presentOnServer = true;
                });
    }
}
