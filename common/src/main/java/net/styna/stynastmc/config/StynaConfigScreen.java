package net.styna.stynastmc.config;

import java.util.List;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.styna.stynastmc.classicbar.BarHud;
import net.styna.stynastmc.classicbar.config.ClassicBarConfig;
import net.styna.stynastmc.config.tabs.ConfigTab;
import net.styna.stynastmc.config.tabs.FontsTab;
import net.styna.stynastmc.config.tabs.HomeTab;
import net.styna.stynastmc.config.tabs.MufflerTab;
import net.styna.stynastmc.config.tabs.SkinTab;
import net.styna.stynastmc.freecam.FreecamConfigImpl;
import net.styna.stynastmc.tweaks.TweaksConfig;
import net.styna.stynastmc.ui.UiTabScreen;
import net.styna.stynastmc.zoom.config.ZoomConfig;

public class StynaConfigScreen extends UiTabScreen {
    public static final int MUFFLER_TAB = 6;
    public static final int TWEAKS_TAB = 7;
    private static int lastTab;

    public StynaConfigScreen(Screen parent) {
        this(parent, lastTab);
    }

    public StynaConfigScreen(Screen parent, int tab) {
        super(Component.translatable("stynastmc.configuration.title"), parent);
        Runnable none = () -> {
        };
        setTabs(List.of(
                new HomeTab(this),
                new ConfigTab(Component.translatable("stynastmc.configuration.classicbar"), ClassicBarConfig.SPEC, BarHud::reload),
                new ConfigTab(Component.translatable("stynastmc.configuration.freecam"), FreecamConfigImpl.SPEC, none),
                new FontsTab(this),
                new SkinTab(this),
                new ConfigTab(Component.translatable("stynastmc.configuration.zoom"), ZoomConfig.SPEC, none),
                new MufflerTab(),
                new ConfigTab(Component.translatable("stynastmc.configuration.tweaks"), TweaksConfig.SPEC, none)), tab);
        lastTab = current;
    }

    @Override
    protected void onTabChanged(int index) {
        lastTab = index;
    }
}
