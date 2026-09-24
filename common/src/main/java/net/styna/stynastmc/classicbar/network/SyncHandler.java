package net.styna.stynastmc.classicbar.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.styna.stynastmc.platform.Platform;

public final class SyncHandler {
    private static final Map<UUID, Float> lastExhaustionLevels = new HashMap<>();

    private SyncHandler() {
    }

    public static void onPlayerTick(ServerPlayer player) {
        if (!Platform.INSTANCE.canSend(player, ExhaustionSyncPayload.TYPE)) {
            return;
        }

        UUID uuid = player.getUUID();
        Float lastExhaustionLevel = lastExhaustionLevels.get(uuid);
        float exhaustionLevel = player.getFoodData().exhaustionLevel;
        if (lastExhaustionLevel == null || Math.abs(lastExhaustionLevel - exhaustionLevel) >= 0.01F) {
            Platform.INSTANCE.send(player, new ExhaustionSyncPayload(exhaustionLevel));
            lastExhaustionLevels.put(uuid, exhaustionLevel);
        }
    }

    public static void onPlayerLoggedOut(UUID player) {
        lastExhaustionLevels.remove(player);
    }
}
