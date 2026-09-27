package net.styna.stynastmc.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Hosts a {@link UiElement} tree: routes input, tracks focus and drags, and draws tooltips last. */
public abstract class UiScreen extends Screen {
    protected final Screen parent;
    protected final UiElement root = new UiElement() {
        @Override
        public UiScreen screen() {
            return UiScreen.this;
        }
    };
    private UiElement focus;
    private UiElement drag;

    protected UiScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        focus = null;
        drag = null;
        root.clearChildren();
        root.bounds(0, 0, width, height);
        build();
    }

    protected abstract void build();

    /** Relayouts everything, e.g. after the game font changed. */
    public void rebuild() {
        init(width, height);
    }

    public void setFocus(UiElement element) {
        if (focus != null && focus != element) {
            focus.onFocusLost();
        }
        focus = element;
    }

    public void startDrag(UiElement element) {
        drag = element;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        root.render(g, mouseX, mouseY, delta);
        UiElement hovered = root.hovered(mouseX, mouseY);
        while (hovered != null && hovered.tooltip == null) {
            hovered = hovered.parent;
        }
        if (hovered != null && hovered.tooltip.get() != null) {
            Ui.tooltip(g, hovered.tooltip.get(), mouseX, mouseY, width, height);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        setFocus(null);
        return root.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (drag != null) {
            drag.mouseReleased(event.x(), event.y(), event.button());
            drag = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return drag != null && drag.mouseDragged(event.x(), event.y(), event.button()) || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        return root.mouseScrolled(mx, my, scrollY) || super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (focus != null && focus.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return focus != null && focus.charTyped(event) || super.charTyped(event);
    }

    @Override
    public void tick() {
        root.tick();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
