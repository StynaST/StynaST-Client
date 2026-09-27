package net.styna.stynastmc.freecam.config.keys;

@FunctionalInterface
public interface HoldAction {
    /**
     * @return whether the key was "used".
     */
    boolean run();
}
