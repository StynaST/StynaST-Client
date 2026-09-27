package net.styna.stynastmc.platform;

import java.nio.file.Path;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Loader specific services, implemented once per loader and found through {@link java.util.ServiceLoader}. */
public interface Platform {
    Platform INSTANCE = Services.load(Platform.class);

    String loaderName();

    Path configDir();

    String modVersion();

    boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type);

    void send(ServerPlayer player, CustomPacketPayload payload);
}
