package net.styna.stynastmc.photomode;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiScreen;
import net.styna.stynastmc.ui.UiWidgets;

/** Settings panel over the live world. */
public final class PhotoScreen extends UiScreen {
    private static final int PANEL_WIDTH = 200;
    private static final int ROW = 18;
    private static final int DAY = 24000;

    public PhotoScreen() {
        super(Component.translatable("stynastmc.photomode.title"), null);
    }

    @Override
    protected void build() {
        UiElement panel = root.add(new Panel()).bounds(width - PANEL_WIDTH - 8, 8, PANEL_WIDTH, 7 * (ROW + Ui.GAP) + 64);
        int x = panel.x + 5;
        int w = PANEL_WIDTH - 10;
        int y = panel.y + 5;

        panel.add(UiWidgets.group(title)).bounds(x, y, w, ROW);
        y += ROW + Ui.GAP;

        UiWidgets.Toggle lock = toggle(panel, "stynastmc.photomode.lock_time", x, y, w, PhotoMode.time() >= 0, on -> PhotoMode.setTime(on ? 6000 : -1));
        y += ROW + Ui.GAP;

        double initial = Math.max(PhotoMode.time(), 0) / (double) DAY;
        UiWidgets.Slider slider = panel.add(new UiWidgets.Slider(initial, v -> {
            PhotoMode.setTime(Math.min((int) (v * DAY), DAY - 1));
            lock.value = true;
        }));
        slider.step = 1.0 / 240;
        slider.format = v -> clock(Math.min((int) (v * DAY), DAY - 1));
        slider.bounds(x, y, w, ROW);
        y += ROW + Ui.GAP;

        panel.add(new UiWidgets.Button(
                () -> Component.translatable("stynastmc.photomode.weather", PhotoMode.weather().label()), PhotoMode::cycleWeather))
                .bounds(x, y, w, ROW);
        y += ROW + Ui.GAP;

        toggle(panel, "stynastmc.photomode.fog", x, y, w, PhotoMode.fog(), PhotoMode::setFog);
        y += ROW + Ui.GAP;

        panel.add(new UiWidgets.Button(Component.translatable("stynastmc.photomode.save"), this::save)).bounds(x, y, w, ROW);
        y += ROW + Ui.GAP;

        panel.add(new UiWidgets.Button(Component.translatable("stynastmc.photomode.exit"), PhotoMode::exit)).bounds(x, y, w, ROW);
        y += ROW + Ui.GAP;

        UiWidgets.Label hint = panel.add(new UiWidgets.Label(Component.translatable("stynastmc.photomode.panel_hint"), Ui.MUTED));
        hint.wrap = true;
        hint.bounds(x, y, w, 28);
        y += 30;

        UiWidgets.Label saved = panel.add(new UiWidgets.Label(() -> PhotoMode.status() != null ? PhotoMode.status() : Component.empty(), Ui.WHITE));
        saved.wrap = true;
        saved.bounds(x, y, w, 24);
    }

    private void save() {
        PhotoMode.shoot();
        if (!minecraft.hasShiftDown()) {
            onClose();
        }
    }

    private UiWidgets.Toggle toggle(UiElement panel, String key, int x, int y, int w, boolean value, java.util.function.Consumer<Boolean> onChange) {
        UiWidgets.Row row = panel.add(new UiWidgets.Row(Component.translatable(key)));
        row.bounds(x, y, w, ROW);
        UiWidgets.Toggle toggle = row.add(new UiWidgets.Toggle(value, onChange));
        toggle.bounds(row.endX() - 29, row.y + 2, 26, 14);
        return toggle;
    }

    // Tick 0 is 06:00 in game.
    private static Component clock(int ticks) {
        int minutes = (ticks * 60 / 1000 + 360) % 1440;
        return Component.literal(String.format("%02d:%02d", minutes / 60, minutes % 60));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (PhotoMode.KEY.matches(event)) {
            PhotoMode.exit();
            return true;
        }
        return super.keyPressed(event);
    }

    private static final class Panel extends UiElement {
        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            Ui.fill(g, x, y, width, height, Ui.PANEL_BACKGROUND);
            Ui.outline(g, x, y, width, height, Ui.PANEL_OUTLINE);
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            return true;
        }
    }
}
