package net.styna.stynastmc.freecam.tripod;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.styna.stynastmc.freecam.util.FreecamPosition;

public class TripodRegistry {
    private final Map<Integer, Map<TripodSlot, FreecamPosition>> tripods = new HashMap<>();

    public FreecamPosition get(TripodSlot tripod) {
        Map<TripodSlot, FreecamPosition> positions = tripods.get(dimension());
        return positions == null ? null : positions.get(tripod);
    }

    public void put(TripodSlot tripod, FreecamPosition position) {
        tripods.computeIfAbsent(dimension(), dim -> new EnumMap<>(TripodSlot.class)).put(tripod, position);
    }

    public void clear() {
        tripods.clear();
    }

    private static int dimension() {
        return Minecraft.getMinecraft().world.provider.getDimension();
    }
}
