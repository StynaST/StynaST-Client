package net.styna.stynastmc.freecam.tripod;

public enum TripodSlot {
    NONE, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE;

    @Override
    public String toString() {
        return this == NONE ? "None" : "#" + ordinal();
    }

    /** Hotbar key index 0-8 to its slot. */
    public static TripodSlot ofHotbar(int index) {
        return index >= 0 && index < 9 ? values()[index + 1] : NONE;
    }
}
