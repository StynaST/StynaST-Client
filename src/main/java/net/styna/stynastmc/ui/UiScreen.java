package net.styna.stynastmc.ui;

import java.io.IOException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiScreen;

/** Hosts a {@link UiElement} tree: routes input, tracks focus and drags, and draws tooltips last. */
public abstract class UiScreen extends GuiScreen {
    protected final GuiScreen parent;
    protected final String title;
    protected final UiElement root = new UiElement() {
        @Override
        public UiScreen screen() {
            return UiScreen.this;
        }
    };
    private UiElement focus;
    private UiElement drag;

    protected UiScreen(String title, GuiScreen parent) {
        this.title = title;
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        setFocus(null);
        drag = null;
        root.clearChildren();
        root.bounds(0, 0, width, height);
        build();
    }

    protected abstract void build();

    public void rebuild() {
        initGui();
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
    public void drawScreen(int mouseX, int mouseY, float delta) {
        drawDefaultBackground();
        root.render(mouseX, mouseY, delta);
        UiElement hovered = root.hovered(mouseX, mouseY);
        while (hovered != null && hovered.tooltip == null) {
            hovered = hovered.parent;
        }
        if (hovered != null && hovered.tooltip.get() != null) {
            Ui.tooltip(hovered.tooltip.get(), mouseX, mouseY, width, height);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        setFocus(null);
        if (!root.mouseClicked(mouseX, mouseY, button)) {
            super.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        if (drag != null) {
            drag.mouseReleased(mouseX, mouseY, button);
            drag = null;
            return;
        }
        super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long time) {
        if (drag != null) {
            drag.mouseDragged(mouseX, mouseY, button);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mx = Mouse.getEventX() * width / mc.displayWidth;
            int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
            root.mouseScrolled(mx, my, Integer.signum(wheel));
        }
    }

    @Override
    protected void keyTyped(char typed, int keyCode) throws IOException {
        if (focus != null && focus.keyTyped(typed, keyCode)) {
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            onClose();
        }
    }

    @Override
    public void updateScreen() {
        root.tick();
    }

    @Override
    public void onGuiClosed() {
        setFocus(null);
        Keyboard.enableRepeatEvents(false);
    }

    public void onClose() {
        mc.displayGuiScreen(parent);
    }
}
