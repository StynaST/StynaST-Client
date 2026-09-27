package net.styna.stynastmc.ui;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

public final class UiWidgets {
    private UiWidgets() {
    }

    public static void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public static class Label extends UiElement {
        public Supplier<Component> text;
        public int color;
        public float scale = 1.0F;
        public boolean alignRight;
        public boolean wrap;

        public Label(Component text, int color) {
            this(() -> text, color);
        }

        public Label(Supplier<Component> text, int color) {
            this.text = text;
            this.color = color;
        }

        public Label scale(float scale) {
            this.scale = scale;
            return this;
        }

        public int wrappedHeight(int width) {
            return Ui.font().split(text.get(), (int) (width / scale)).size() * (int) Math.ceil(Ui.font().lineHeight * scale);
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            Component c = text.get();
            if (wrap) {
                g.pose().pushMatrix();
                g.pose().translate(x, y);
                g.pose().scale(scale, scale);
                g.textWithWordWrap(Ui.font(), c, 0, 0, (int) (width / scale), color);
                g.pose().popMatrix();
                return;
            }
            int textHeight = (int) (Ui.font().lineHeight * scale);
            int ty = y + (height - textHeight) / 2 + 1;
            int tx = alignRight ? endX() - (int) (Ui.font().width(c) * scale) : x;
            Ui.text(g, c, tx, ty, color, scale);
        }
    }

    public static Label group(Component text) {
        return new Label(text, Ui.GROUP_LABEL);
    }

    public static class Button extends UiElement {
        public Supplier<Component> label;
        public Runnable onPress;
        public Consumer<Integer> onPressButton;
        public Identifier icon;
        public int background = Ui.PANEL_BACKGROUND;
        public int outline = Ui.PANEL_OUTLINE;
        public int textColor = Ui.WHITE;
        public boolean transparent;
        public boolean alignLeft;
        public boolean selected;
        public boolean enabled = true;

        public Button(Component label, Runnable onPress) {
            this(() -> label, onPress);
        }

        public Button(Supplier<Component> label, Runnable onPress) {
            this.label = label;
            this.onPress = onPress;
        }

        public Button icon(Identifier icon) {
            this.icon = icon;
            return this;
        }

        public Button transparent() {
            transparent = true;
            return this;
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hot = enabled && contains(mx, my);
            if (!transparent) {
                Ui.fill(g, x, y, width, height, background);
            }
            if (selected) {
                Ui.fill(g, x, y, width, height, Ui.HOVER_FILL);
            }
            if (!transparent || hot) {
                Ui.outline(g, x, y, width, height, hot ? Ui.FOCUS_OUTLINE : outline);
            }
            int contentX = x;
            int contentW = width;
            if (icon != null) {
                int size = Math.min(16, height - 4);
                int ix = label.get().getString().isEmpty() ? x + (width - size) / 2 : x + 3;
                Ui.sprite(g, icon, ix, y + (height - size) / 2, size, size);
                contentX = ix + size + 2;
                contentW = endX() - contentX;
            }
            Component text = label.get();
            if (alignLeft) {
                Ui.textInBox(g, text, contentX + 5, y, contentW - 10, height, enabled ? textColor : Ui.MUTED, false);
            } else if (!text.getString().isEmpty()) {
                Ui.centeredText(g, text, contentX, y, contentW, height, enabled ? textColor : Ui.MUTED);
            }
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            if (!enabled) {
                return false;
            }
            playClick();
            if (onPressButton != null) {
                onPressButton.accept(button);
            } else if (onPress != null && button == InputConstants.MOUSE_BUTTON_LEFT) {
                onPress.run();
            }
            return true;
        }
    }

    public static class Toggle extends UiElement {
        private static final Identifier OFF = Identifier.fromNamespaceAndPath("stynastmc", "ui/togglebtn");
        private static final Identifier OFF_HOT = Identifier.fromNamespaceAndPath("stynastmc", "ui/togglebtn_highlighted");
        private static final Identifier ON = Identifier.fromNamespaceAndPath("stynastmc", "ui/togglebtn_toggled");
        private static final Identifier ON_HOT = Identifier.fromNamespaceAndPath("stynastmc", "ui/togglebtn_toggled_highlighted");

        public boolean value;
        public Consumer<Boolean> onChange;

        public Toggle(boolean value, Consumer<Boolean> onChange) {
            this.value = value;
            this.onChange = onChange;
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hot = contains(mx, my);
            Ui.sprite(g, value ? (hot ? ON_HOT : ON) : (hot ? OFF_HOT : OFF), x, y, width, height);
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            value = !value;
            onChange.accept(value);
            playClick();
            return true;
        }
    }

    /** Single line text input. Calls onChange for every edit that passes the validator. */
    public static class TextField extends UiElement {
        public String text;
        public Component placeholder = Component.empty();
        public Predicate<String> validator = s -> true;
        public Consumer<String> onChange = s -> {
        };
        public Integer swatch;
        private int cursor;
        private int scrollOffset;
        private boolean focused;
        private int ticks;

        public TextField(String text) {
            this.text = text;
            this.cursor = text.length();
        }

        public boolean isValid() {
            return validator.test(text);
        }

        public void setText(String text) {
            this.text = text;
            cursor = Math.min(cursor, text.length());
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            Font font = Ui.font();
            boolean hot = contains(mx, my);
            Ui.fill(g, x, y, width, height, Ui.PANEL_BACKGROUND);
            int tx = x + 4;
            int innerWidth = width - 8;
            if (swatch != null) {
                Ui.fill(g, x + 3, y + 3, height - 6, height - 6, 0xFF000000 | swatch);
                Ui.outline(g, x + 3, y + 3, height - 6, height - 6, Ui.BLACK);
                tx += height - 4;
                innerWidth -= height - 4;
            }
            int border = !isValid() ? Ui.ERROR : focused || hot ? Ui.FOCUS_OUTLINE : Ui.PANEL_OUTLINE;
            Ui.outline(g, x, y, width, height, border);

            if (cursor < scrollOffset) {
                scrollOffset = cursor;
            }
            while (font.width(text.substring(scrollOffset, cursor)) > innerWidth && scrollOffset < cursor) {
                scrollOffset++;
            }
            String visible = font.plainSubstrByWidth(text.substring(scrollOffset), innerWidth);
            int ty = y + (height - font.lineHeight) / 2 + 1;
            g.enableScissor(x + 1, y + 1, endX() - 1, endY() - 1);
            if (text.isEmpty() && !focused) {
                g.text(font, placeholder, tx, ty, Ui.MUTED, true);
            } else {
                g.text(font, visible, tx, ty, Ui.WHITE, true);
            }
            if (focused && (ticks / 6) % 2 == 0) {
                int cx = tx + font.width(text.substring(scrollOffset, cursor));
                Ui.fill(g, cx, ty - 1, 1, font.lineHeight, Ui.WHITE);
            }
            g.disableScissor();
        }

        @Override
        public void tick() {
            ticks++;
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            UiScreen screen = screen();
            if (screen != null) {
                screen.setFocus(this);
            }
            focused = true;
            // SDL only delivers typed characters while text input is started.
            Minecraft.getInstance().textInputManager().startTextInput(this);
            int tx = x + 4 + (swatch != null ? height - 4 : 0);
            String visible = text.substring(scrollOffset);
            cursor = scrollOffset + Ui.font().plainSubstrByWidth(visible, (int) mx - tx).length();
            return true;
        }

        @Override
        public void onFocusLost() {
            focused = false;
            Minecraft.getInstance().textInputManager().stopTextInput(this);
        }

        private void edit(String newText, int newCursor) {
            text = newText;
            cursor = Math.max(0, Math.min(newCursor, text.length()));
            if (isValid()) {
                onChange.accept(text);
            }
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            switch (event.key()) {
                case InputConstants.KEY_BACKSPACE -> {
                    if (cursor > 0) {
                        edit(text.substring(0, cursor - 1) + text.substring(cursor), cursor - 1);
                    }
                }
                case InputConstants.KEY_DELETE -> {
                    if (cursor < text.length()) {
                        edit(text.substring(0, cursor) + text.substring(cursor + 1), cursor);
                    }
                }
                case InputConstants.KEY_LEFT -> cursor = Math.max(0, cursor - 1);
                case InputConstants.KEY_RIGHT -> cursor = Math.min(text.length(), cursor + 1);
                case InputConstants.KEY_HOME -> cursor = 0;
                case InputConstants.KEY_END -> cursor = text.length();
                case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER, InputConstants.KEY_ESCAPE -> {
                    UiScreen screen = screen();
                    if (screen != null) {
                        screen.setFocus(null);
                    }
                }
                default -> {
                    if (event.isPaste()) {
                        String paste = Minecraft.getInstance().keyboardHandler.getClipboard().replaceAll("[\\r\\n]", "");
                        edit(text.substring(0, cursor) + paste + text.substring(cursor), cursor + paste.length());
                    } else if (event.isSelectAll()) {
                        edit("", 0);
                    } else {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean charTyped(CharacterEvent event) {
            if (!event.isAllowedChatCharacter()) {
                return false;
            }
            String s = event.codepointAsString();
            edit(text.substring(0, cursor) + s + text.substring(cursor), cursor + s.length());
            return true;
        }
    }

    public static class Slider extends UiElement {
        public double value;
        public Consumer<Double> onChange;
        private boolean dragging;

        public Slider(double value, Consumer<Double> onChange) {
            this.value = value;
            this.onChange = onChange;
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hot = dragging || contains(mx, my);
            Ui.fill(g, x, y, width, height, Ui.PANEL_BACKGROUND);
            Ui.fill(g, x + 1, y + 1, (int) ((width - 2) * value), height - 2, Ui.QUICK_BACKGROUND);
            int knobX = x + (int) ((width - 4) * value);
            Ui.fill(g, knobX, y, 4, height, hot ? Ui.KNOB_HOVER : Ui.KNOB);
            Ui.outline(g, x, y, width, height, hot ? Ui.FOCUS_OUTLINE : Ui.PANEL_OUTLINE);
            Ui.centeredText(g, Component.literal(Math.round(value * 100) + "%"), x, y, width, height, Ui.WHITE);
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            UiScreen screen = screen();
            if (screen != null) {
                screen.startDrag(this);
            }
            dragging = true;
            dragTo(mx);
            return true;
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button) {
            dragTo(mx);
            return true;
        }

        @Override
        public void mouseReleased(double mx, double my, int button) {
            dragging = false;
        }

        private void dragTo(double mx) {
            set((mx - x - 2) / (width - 4));
        }

        private void set(double v) {
            v = Math.round(Math.clamp(v, 0.0, 1.0) * 100) / 100.0;
            if (v != value) {
                value = v;
                onChange.accept(v);
            }
        }
    }

    public static class Row extends UiElement {
        public Component label;

        public Row(Component label) {
            this.label = label;
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            Ui.fill(g, x, y, width, height, Ui.PANEL_BACKGROUND);
            Ui.textInBox(g, label, x + 5, y, width - 10, height, Ui.WHITE, false);
        }

        @Override
        protected void renderOverlay(GuiGraphicsExtractor g, int mx, int my, float delta) {
            Ui.outline(g, x, y, width, height, contains(mx, my) ? Ui.FOCUS_OUTLINE : Ui.ROW_OUTLINE);
        }
    }

    public static class InfoRow extends Row {
        private final Supplier<Component> value;

        public InfoRow(Component label, Supplier<Component> value) {
            super(label);
            this.value = value;
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            super.renderSelf(g, mx, my, delta);
            Ui.textInBox(g, value.get(), x + 5, y, width - 10, height, Ui.WHITE, true);
        }
    }
}
