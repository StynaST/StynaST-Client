package net.styna.stynastmc.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import net.minecraft.client.gui.GuiScreen;
import net.styna.stynastmc.config.tabs.CapeTab;
import net.styna.stynastmc.config.tabs.ConfigTab;
import net.styna.stynastmc.firstperson.FirstPersonConfig;
import net.styna.stynastmc.config.tabs.FontsTab;
import net.styna.stynastmc.config.tabs.HomeTab;
import net.styna.stynastmc.config.tabs.MufflerTab;
import net.styna.stynastmc.config.tabs.OfflinePlayersTab;
import net.styna.stynastmc.config.tabs.SkinTab;
import net.styna.stynastmc.config.tabs.Tab;
import net.styna.stynastmc.freecam.config.FreecamConfig;
import net.styna.stynastmc.tweaks.TweaksConfig;
import net.styna.stynastmc.ui.UiTabScreen;
import net.styna.stynastmc.util.Text;
import net.styna.stynastmc.zoom.config.ZoomConfig;

public class StynaConfigScreen extends UiTabScreen {
    public static final int FREECAM_TAB = 1;
    public static final int FONTS_TAB = 2;
    public static final int SKIN_TAB = 3;
    public static final int CAPE_TAB = 4;
    public static final int OFFLINE_TAB = 5;
    public static final int ZOOM_TAB = 6;
    public static final int MUFFLER_TAB = 7;
    public static final int TWEAKS_TAB = 8;
    private static final List<Function<StynaConfigScreen, Tab>> ADDON_TABS = new ArrayList<>();
    private static int lastTab;

    /** Lets addon mods append their own tabs after the built-in ones. */
    public static void addTab(Function<StynaConfigScreen, Tab> tab) {
        ADDON_TABS.add(tab);
    }

    public StynaConfigScreen(GuiScreen parent) {
        this(parent, lastTab);
    }

    public StynaConfigScreen(GuiScreen parent, int tab) {
        super(Text.tr("stynastmc.configuration.title"), parent);
        Runnable none = () -> {
        };
        List<Tab> tabs = new ArrayList<>(Arrays.asList(
                new HomeTab(this),
                new ConfigTab(Text.tr("stynastmc.configuration.freecam"), FreecamConfig.SPEC, none),
                new FontsTab(this),
                new SkinTab(this),
                new CapeTab(this),
                new OfflinePlayersTab(),
                new ConfigTab(Text.tr("stynastmc.configuration.zoom"), ZoomConfig.SPEC, none),
                new MufflerTab(),
                new ConfigTab(Text.tr("stynastmc.configuration.tweaks"), TweaksConfig.SPEC, none),
                new ConfigTab(Text.tr("stynastmc.configuration.firstperson"), FirstPersonConfig.SPEC, none)));
        ADDON_TABS.forEach(factory -> tabs.add(factory.apply(this)));
        setTabs(tabs, tab);
        lastTab = current;
    }

    @Override
    protected void onTabChanged(int index) {
        lastTab = index;
    }
}
