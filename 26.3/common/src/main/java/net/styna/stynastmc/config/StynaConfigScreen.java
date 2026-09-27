package net.styna.stynastmc.config;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.styna.stynastmc.classicbar.BarHud;
import net.styna.stynastmc.classicbar.config.ClassicBarConfig;
import net.styna.stynastmc.config.tabs.ConfigTab;
import net.styna.stynastmc.config.tabs.FontsTab;
import net.styna.stynastmc.config.tabs.HomeTab;
import net.styna.stynastmc.config.tabs.MufflerTab;
import net.styna.stynastmc.config.tabs.SkinTab;
import net.styna.stynastmc.config.tabs.Tab;
import net.styna.stynastmc.freecam.FreecamConfigImpl;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiScreen;
import net.styna.stynastmc.ui.UiWidgets;
import net.styna.stynastmc.zoom.config.ZoomConfig;

/** Mod settings in the layout of Better Statistics Screen: a tab strip and one document per tab. */
public class StynaConfigScreen extends UiScreen {
    public static final int MUFFLER_TAB = 6;
    private static final int BAR_HEIGHT = 17;
    private static int lastTab;

    private final List<Tab> tabs;
    private int current;

    public StynaConfigScreen(Screen parent) {
        this(parent, lastTab);
    }

    public StynaConfigScreen(Screen parent, int tab) {
        super(Component.translatable("stynastmc.configuration.title"), parent);
        tabs = List.of(
                new HomeTab(this),
                new ConfigTab(Component.translatable("stynastmc.configuration.classicbar"), ClassicBarConfig.SPEC, BarHud::reload),
                new ConfigTab(Component.translatable("stynastmc.configuration.freecam"), FreecamConfigImpl.SPEC, () -> {
                }),
                new FontsTab(this),
                new SkinTab(this),
                new ConfigTab(Component.translatable("stynastmc.configuration.zoom"), ZoomConfig.SPEC, () -> {
                }),
                new MufflerTab());
        current = Math.min(tab, tabs.size() - 1);
        lastTab = current;
    }

    public void openTab(int index) {
        switchTo(index);
    }

    private void switchTo(int index) {
        tabs.get(current).onLeave();
        current = index;
        lastTab = index;
        rebuild();
    }

    @Override
    protected void build() {
        int frameX = (int) (width * 0.05);
        int frameWidth = (int) (width * 0.9);

        UiElement document = root.add(new Frame(Ui.TAB_BACKGROUND, Ui.TAB_FOREGROUND));
        document.bounds(frameX, 5, frameWidth, height - 10);

        UiElement strip = document.add(new Frame(Ui.TAB_STRIP_BACKGROUND, Ui.TAB_STRIP_FOREGROUND));
        strip.bounds(document.x + 1, document.y + 1, document.width - 2, BAR_HEIGHT);
        int tx = strip.x;
        for (int i = 0; i < tabs.size(); i++) {
            TabEntry entry = strip.add(new TabEntry(tabs.get(i).title(), i));
            entry.bounds(tx, strip.y, (int) (Ui.font().width(entry.title) * 0.85F) + 12, BAR_HEIGHT);
            tx = entry.endX();
        }

        UiElement content = document.add(new UiElement());
        content.bounds(document.x + 1, strip.endY() + 1, document.width - 2, document.endY() - strip.endY() - 2);
        tabs.get(current).build(content);
    }

    @Override
    public void onClose() {
        tabs.get(current).onLeave();
        super.onClose();
    }

    @Override
    public void tick() {
        super.tick();
        tabs.get(current).tick();
    }

    /** Sprite backed frame: background under the children, foreground (border) over them. */
    private static class Frame extends UiElement {
        private final net.minecraft.resources.Identifier background;
        private final net.minecraft.resources.Identifier foreground;

        Frame(net.minecraft.resources.Identifier background, net.minecraft.resources.Identifier foreground) {
            this.background = background;
            this.foreground = foreground;
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            Ui.sprite(g, background, x, y, width, height);
        }

        @Override
        protected void renderOverlay(GuiGraphicsExtractor g, int mx, int my, float delta) {
            Ui.sprite(g, foreground, x, y, width, height);
        }
    }

    private class TabEntry extends UiElement {
        private final Component title;
        private final int index;

        TabEntry(Component title, int index) {
            this.title = title;
            this.index = index;
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean selected = index == current;
            Ui.sprite(g, selected ? Ui.TAB_ENTRY_SELECTED : Ui.TAB_ENTRY, x, y, width, height);
            if (!selected && contains(mx, my)) {
                Ui.outline(g, x, y, width, height, Ui.PANEL_OUTLINE);
            }
            Ui.text(g, title, x + 6, y + (height - 7) / 2, selected ? Ui.TAB_TEXT_SELECTED : Ui.TAB_TEXT, 0.85F);
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            if (index != current) {
                UiWidgets.playClick();
                switchTo(index);
            }
            return true;
        }
    }
}
