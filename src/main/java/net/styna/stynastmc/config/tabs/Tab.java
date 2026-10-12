package net.styna.stynastmc.config.tabs;

import net.styna.stynastmc.ui.UiElement;

/** One document in the settings screen. Tabs outlive rebuilds, so keep UI state in fields. */
public interface Tab {
    String title();

    void build(UiElement area);

    default void onLeave() {
    }

    default void tick() {
    }
}
