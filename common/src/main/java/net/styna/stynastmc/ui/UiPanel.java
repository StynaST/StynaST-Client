package net.styna.stynastmc.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/** Clipped, vertically scrolling panel with a TCDCommons style flat scrollbar. */
public class UiPanel extends UiElement {
    private static final int SCROLLBAR_WIDTH = 6;

    public int background = Ui.PANEL_BACKGROUND;
    public int outline = Ui.PANEL_OUTLINE;
    public int padding = 10;
    private int cursor;
    private double scroll;
    private boolean draggingBar;

    @Override
    public UiPanel bounds(int x, int y, int width, int height) {
        super.bounds(x, y, width, height);
        cursor = y + padding;
        return this;
    }

    public UiPanel style(int background, int outline, int padding) {
        this.background = background;
        this.outline = outline;
        this.padding = padding;
        cursor = y + padding;
        return this;
    }

    public int innerX() {
        return x + padding;
    }

    public int innerWidth() {
        return width - padding * 2 - (canScroll() ? SCROLLBAR_WIDTH : 0);
    }

    public <T extends UiElement> T place(T element, int height, int gap) {
        element.bounds(innerX(), cursor, width - padding * 2 - SCROLLBAR_WIDTH, height);
        cursor += height + gap;
        return add(element);
    }

    public int reserve(int height, int gap) {
        int top = cursor;
        cursor += height + gap;
        return top;
    }

    @Override
    public void clearChildren() {
        super.clearChildren();
        cursor = y + padding;
        scroll = 0;
    }

    private int contentHeight() {
        return cursor - y - Ui.GAP + padding;
    }

    private boolean canScroll() {
        return contentHeight() > height;
    }

    private double maxScroll() {
        return Math.max(0, contentHeight() - height);
    }

    @Override
    protected double childMouseY(double my) {
        return my + scroll;
    }

    @Override
    public void render(GuiGraphicsExtractor g, int mx, int my, float delta) {
        scroll = Mth.clamp(scroll, 0, maxScroll());
        Ui.fill(g, x, y, width, height, background);
        g.enableScissor(x + 1, y + 1, endX() - 1, endY() - 1);
        g.pose().pushMatrix();
        g.pose().translate(0, (float) -scroll);
        int cy = contains(mx, my) ? (int) (my + scroll) : Integer.MIN_VALUE;
        for (UiElement child : children) {
            if (child.visible && child.endY() >= y + scroll && child.y <= endY() + scroll) {
                child.render(g, mx, cy, delta);
            }
        }
        g.pose().popMatrix();
        g.disableScissor();
        Ui.outline(g, x, y, width, height, outline);
        if (canScroll()) {
            int barX = endX() - SCROLLBAR_WIDTH - 1;
            Ui.fill(g, barX, y + 1, SCROLLBAR_WIDTH, height - 2, Ui.PANEL_BACKGROUND);
            int knobHeight = Math.max(10, (int) ((height - 2) * (height / (double) contentHeight())));
            int knobY = y + 1 + (int) ((height - 2 - knobHeight) * (scroll / maxScroll()));
            boolean hot = draggingBar || (mx >= barX && mx < barX + SCROLLBAR_WIDTH && my >= y && my < endY());
            Ui.fill(g, barX, knobY, SCROLLBAR_WIDTH, knobHeight, hot ? Ui.KNOB_HOVER : Ui.KNOB);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (canScroll() && mx >= endX() - SCROLLBAR_WIDTH - 1) {
            draggingBar = true;
            dragTo(my);
            UiScreen screen = screen();
            if (screen != null) {
                screen.startDrag(this);
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button) {
        if (draggingBar) {
            dragTo(my);
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mx, double my, int button) {
        draggingBar = false;
    }

    private void dragTo(double my) {
        scroll = Mth.clamp((my - y) / height * contentHeight() - height / 2.0, 0, maxScroll());
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (super.mouseScrolled(mx, my, amount)) {
            return true;
        }
        if (!canScroll()) {
            return false;
        }
        scroll = Mth.clamp(scroll - amount * 20, 0, maxScroll());
        return true;
    }

    public double scroll() {
        return scroll;
    }

    public void setScroll(double scroll) {
        this.scroll = scroll;
    }

    public void scrollTo(UiElement element) {
        scroll = Mth.clamp(element.y - y - padding, 0, maxScroll());
    }
}
