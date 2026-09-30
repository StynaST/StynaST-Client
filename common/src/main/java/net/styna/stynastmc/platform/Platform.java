package net.styna.stynastmc.platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.styna.stynastmc.StynaSTMC;

/** Loader specific services, implemented once per loader and found through {@link java.util.ServiceLoader}. */
public interface Platform {
    Platform INSTANCE = Services.load(Platform.class);

    String loaderName();

    Path configDir();

    default Path modConfigDir() {
        return configDir().resolve("StynaSTMC");
    }

    /** Resolves a file in {@link #modConfigDir()}, moving it over from its old location once. */
    default Path modConfigFile(String name, Path legacy) {
        Path file = modConfigDir().resolve(name);
        if (!Files.exists(file) && Files.exists(legacy)) {
            try {
                Files.createDirectories(file.getParent());
                Files.move(legacy, file);
                StynaSTMC.LOGGER.info("Moved {} to {}", legacy, file);
            } catch (IOException e) {
                StynaSTMC.LOGGER.warn("Could not move {} to {}", legacy, file, e);
            }
        }
        return file;
    }

    String modVersion();

    boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type);

    void send(ServerPlayer player, CustomPacketPayload payload);
}
