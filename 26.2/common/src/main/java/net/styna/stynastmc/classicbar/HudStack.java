package net.styna.stynastmc.classicbar;

/** Height already used by the left and right status bar stacks above the hotbar. */
public interface HudStack {
    int left();

    int right();

    void left(int height);

    void right(int height);
}
