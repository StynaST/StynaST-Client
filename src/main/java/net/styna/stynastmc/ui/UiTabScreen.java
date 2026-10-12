package net.styna.stynastmc.ui;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.styna.stynastmc.config.tabs.Tab;

/** Better Statistics Screen layout: a tab strip over one document per tab. */
public abstract class UiTabScreen extends UiScreen {
    private static final int BAR_HEIGHT = 17;

    protected List<Tab> tabs = Collections.emptyList();
    protected int current;

    protected UiTabScreen(String title, GuiScreen parent) {
        super(title, parent);
    }

    protected void setTabs(List<Tab> tabs, int initial) {
        this.tabs = tabs;
        current = MathHelper.clamp(initial, 0, tabs.size() - 1);
    }

    protected void onTabChanged(int index) {
    }

    public void openTab(int index) {
        tabs.get(current).onLeave();
        current = index;
        onTabChanged(index);
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
            entry.bounds(tx, strip.y, (int) (Ui.width(entry.title) * 0.85F) + 12, BAR_HEIGHT);
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
    public void updateScreen() {
        super.updateScreen();
        tabs.get(current).tick();
    }

    /** Sprite backed frame: background under the children, foreground (border) over them. */
    private static class Frame extends UiElement {
        private final ResourceLocation background;
        private final ResourceLocation foreground;

        Frame(ResourceLocation background, ResourceLocation foreground) {
            this.background = background;
            this.foreground = foreground;
        }

        @Override
        protected void renderSelf(int mx, int my, float delta) {
            Ui.sprite(background, x, y, width, height);
        }

        @Override
        protected void renderOverlay(int mx, int my, float delta) {
            Ui.sprite(foreground, x, y, width, height);
        }
    }

    private class TabEntry extends UiElement {
        private final String title;
        private final int index;

        TabEntry(String title, int index) {
            this.title = title;
            this.index = index;
        }

        @Override
        protected void renderSelf(int mx, int my, float delta) {
            boolean selected = index == current;
            Ui.sprite(selected ? Ui.TAB_ENTRY_SELECTED : Ui.TAB_ENTRY, x, y, width, height);
            if (!selected && contains(mx, my)) {
                Ui.outline(x, y, width, height, Ui.PANEL_OUTLINE);
            }
            Ui.text(title, x + 6, y + (height - 7) / 2, selected ? Ui.TAB_TEXT_SELECTED : Ui.TAB_TEXT, 0.85F);
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            if (index != current) {
                UiWidgets.playClick();
                openTab(index);
            }
            return true;
        }
    }
}
