package net.styna.stynastmc.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * Minimal retained element tree. Children use absolute screen coordinates; scroll panels offset
 * the pose and the mouse for everything inside them.
 */
public class UiElement {
    public int x;
    public int y;
    public int width;
    public int height;
    public boolean visible = true;
    public Supplier<Component> tooltip;
    protected UiElement parent;
    protected final List<UiElement> children = new ArrayList<>();

    public UiElement bounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public int endX() {
        return x + width;
    }

    public int endY() {
        return y + height;
    }

    public <T extends UiElement> T add(T child) {
        child.parent = this;
        children.add(child);
        return child;
    }

    public void clearChildren() {
        children.clear();
    }

    public UiElement tooltip(Component text) {
        tooltip = () -> text;
        return this;
    }

    public boolean contains(double mx, double my) {
        return mx >= x && my >= y && mx < x + width && my < y + height;
    }

    public UiScreen screen() {
        return parent == null ? null : parent.screen();
    }

    public void render(GuiGraphicsExtractor g, int mx, int my, float delta) {
        renderSelf(g, mx, my, delta);
        for (UiElement child : children) {
            if (child.visible) {
                child.render(g, mx, my, delta);
            }
        }
        renderOverlay(g, mx, my, delta);
    }

    protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
    }

    protected void renderOverlay(GuiGraphicsExtractor g, int mx, int my, float delta) {
    }

    /** Transforms mouse coordinates from this element's space into its children's space. */
    protected double childMouseY(double my) {
        return my;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        double cy = childMouseY(my);
        for (int i = children.size() - 1; i >= 0; i--) {
            UiElement child = children.get(i);
            if (child.visible && child.contains(mx, cy) && child.mouseClicked(mx, cy, button)) {
                return true;
            }
        }
        return onClick(mx, my, button);
    }

    protected boolean onClick(double mx, double my, int button) {
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        double cy = childMouseY(my);
        for (int i = children.size() - 1; i >= 0; i--) {
            UiElement child = children.get(i);
            if (child.visible && child.contains(mx, cy) && child.mouseScrolled(mx, cy, amount)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mx, double my, int button) {
        return false;
    }

    public void mouseReleased(double mx, double my, int button) {
    }

    public boolean keyPressed(KeyEvent event) {
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        return false;
    }

    public void onFocusLost() {
    }

    /** Deepest visible element under the mouse, for tooltips and hover. */
    public UiElement hovered(double mx, double my) {
        double cy = childMouseY(my);
        for (int i = children.size() - 1; i >= 0; i--) {
            UiElement child = children.get(i);
            if (child.visible && child.contains(mx, cy)) {
                UiElement hit = child.hovered(mx, cy);
                return hit != null ? hit : child;
            }
        }
        return null;
    }

    public void tick() {
        children.forEach(UiElement::tick);
    }
}
