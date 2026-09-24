package net.styna.stynastmc.neoforge;

import java.nio.file.Path;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.platform.Platform;

public final class NeoForgePlatform implements Platform {
    @Override
    public String loaderName() {
        return "NeoForge";
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String modVersion() {
        return ModList.get().getModContainerById(StynaSTMC.MODID).map(c -> c.getModInfo().getVersion().toString()).orElse("?");
    }

    @Override
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection.hasChannel(type);
    }

    @Override
    public void send(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
