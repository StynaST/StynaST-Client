package net.styna.stynastmc.ui;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public final class UiWidgets {
    private UiWidgets() {
    }

    public static void playClick() {
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public static class Label extends UiElement {
        public Supplier<String> text;
        public int color;
        public float scale = 1.0F;
        public boolean alignRight;
        public boolean wrap;

        public Label(String text, int color) {
            this(() -> text, color);
        }

        public Label(Supplier<String> text, int color) {
            this.text = text;
            this.color = color;
        }

        public Label scale(float scale) {
            this.scale = scale;
            return this;
        }

        public int wrappedHeight(int width) {
            return lines((int) (width / scale)).size() * (int) Math.ceil(Ui.lineHeight() * scale);
        }

        private List<String> lines(int width) {
            return Ui.font().listFormattedStringToWidth(text.get(), Math.max(1, width));
        }

        @Override
        protected void renderSelf(int mx, int my, float delta) {
            String s = text.get();
            if (wrap) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(x, y, 0);
                GlStateManager.scale(scale, scale, 1);
                List<String> lines = lines((int) (width / scale));
                for (int i = 0; i < lines.size(); i++) {
                    Ui.font().drawString(lines.get(i), 0, i * Ui.lineHeight(), color);
                }
                GlStateManager.popMatrix();
                return;
            }
            int textHeight = (int) (Ui.lineHeight() * scale);
            int ty = y + (height - textHeight) / 2 + 1;
            int tx = alignRight ? endX() - (int) (Ui.width(s) * scale) : x;
            Ui.text(s, tx, ty, color, scale);
        }
    }

    public static Label group(String text) {
        return new Label(text, Ui.GROUP_LABEL);
    }

    public static class Button extends UiElement {
        public Supplier<String> label;
        public Runnable onPress;
        public IntConsumer onPressButton;
        public ResourceLocation icon;
        public ItemStack item = ItemStack.EMPTY;
        public int background = Ui.PANEL_BACKGROUND;
        public int outline = Ui.PANEL_OUTLINE;
        public int textColor = Ui.WHITE;
        public boolean transparent;
        public boolean alignLeft;
        public boolean selected;
        public boolean enabled = true;

        public Button(String label, Runnable onPress) {
            this(() -> label, onPress);
        }

        public Button(Supplier<String> label, Runnable onPress) {
            this.label = label;
            this.onPress = onPress;
        }

        public Button icon(ResourceLocation icon) {
            this.icon = icon;
            return this;
        }

        public Button item(ItemStack item) {
            this.item = item;
            return this;
        }

        public Button transparent() {
            transparent = true;
            return this;
        }

        @Override
        protected void renderSelf(int mx, int my, float delta) {
            boolean hot = enabled && contains(mx, my);
            if (!transparent) {
                Ui.fill(x, y, width, height, background);
            }
            if (selected) {
                Ui.fill(x, y, width, height, Ui.HOVER_FILL);
            }
            if (!transparent || hot) {
                Ui.outline(x, y, width, height, hot ? Ui.FOCUS_OUTLINE : outline);
            }
            String text = label.get();
            int contentX = x;
            int contentW = width;
            if (icon != null || !item.isEmpty()) {
                int size = Math.min(16, height - 4);
                int ix = text.isEmpty() ? x + (width - size) / 2 : x + 3;
                int iy = y + (height - size) / 2;
                if (!item.isEmpty()) {
                    Ui.item(item, ix, iy);
                } else {
                    Ui.sprite(icon, ix, iy, size, size);
                }
                contentX = ix + size + 2;
                contentW = endX() - contentX;
            }
            if (alignLeft) {
                Ui.textInBox(text, contentX + 5, y, contentW - 10, height, enabled ? textColor : Ui.MUTED, false);
            } else if (!text.isEmpty()) {
                Ui.centeredText(text, contentX, y, contentW, height, enabled ? textColor : Ui.MUTED);
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
            } else if (onPress != null && button == 0) {
                onPress.run();
            }
            return true;
        }
    }

    public static class Toggle extends UiElement {
        private static final ResourceLocation OFF = Ui.sprite("ui/togglebtn");
        private static final ResourceLocation OFF_HOT = Ui.sprite("ui/togglebtn_highlighted");
        private static final ResourceLocation ON = Ui.sprite("ui/togglebtn_toggled");
        private static final ResourceLocation ON_HOT = Ui.sprite("ui/togglebtn_toggled_highlighted");

        public boolean value;
        public Consumer<Boolean> onChange;

        public Toggle(boolean value, Consumer<Boolean> onChange) {
            this.value = value;
            this.onChange = onChange;
        }

        @Override
        protected void renderSelf(int mx, int my, float delta) {
            boolean hot = contains(mx, my);
            Ui.sprite(value ? (hot ? ON_HOT : ON) : (hot ? OFF_HOT : OFF), x, y, width, height);
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
        public String placeholder = "";
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
        protected void renderSelf(int mx, int my, float delta) {
            FontRenderer font = Ui.font();
            boolean hot = contains(mx, my);
            Ui.fill(x, y, width, height, Ui.PANEL_BACKGROUND);
            int tx = x + 4;
            int innerWidth = width - 8;
            if (swatch != null) {
                Ui.fill(x + 3, y + 3, height - 6, height - 6, 0xFF000000 | swatch);
                Ui.outline(x + 3, y + 3, height - 6, height - 6, Ui.BLACK);
                tx += height - 4;
                innerWidth -= height - 4;
            }
            int border = !isValid() ? Ui.ERROR : focused || hot ? Ui.FOCUS_OUTLINE : Ui.PANEL_OUTLINE;
            Ui.outline(x, y, width, height, border);

            scrollOffset = Math.min(scrollOffset, text.length());
            if (cursor < scrollOffset) {
                scrollOffset = cursor;
            }
            while (font.getStringWidth(text.substring(scrollOffset, cursor)) > innerWidth && scrollOffset < cursor) {
                scrollOffset++;
            }
            String visible = font.trimStringToWidth(text.substring(scrollOffset), innerWidth);
            int ty = y + (height - font.FONT_HEIGHT) / 2 + 1;
            Ui.enableScissor(x + 1, y + 1, endX() - 1, endY() - 1);
            if (text.isEmpty() && !focused) {
                font.drawStringWithShadow(placeholder, tx, ty, Ui.MUTED);
            } else {
                font.drawStringWithShadow(visible, tx, ty, Ui.WHITE);
            }
            if (focused && (ticks / 6) % 2 == 0) {
                int cx = tx + font.getStringWidth(text.substring(scrollOffset, cursor));
                Ui.fill(cx, ty - 1, 1, font.FONT_HEIGHT, Ui.WHITE);
            }
            Ui.disableScissor();
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
            int tx = x + 4 + (swatch != null ? height - 4 : 0);
            String visible = text.substring(Math.min(scrollOffset, text.length()));
            cursor = Math.min(text.length(), scrollOffset + Ui.font().trimStringToWidth(visible, Math.max(0, (int) mx - tx)).length());
            return true;
        }

        @Override
        public void onFocusLost() {
            focused = false;
        }

        private void edit(String newText, int newCursor) {
            text = newText;
            cursor = Math.max(0, Math.min(newCursor, text.length()));
            if (isValid()) {
                onChange.accept(text);
            }
        }

        @Override
        public boolean keyTyped(char typed, int keyCode) {
            if (GuiScreen.isKeyComboCtrlV(keyCode)) {
                String paste = GuiScreen.getClipboardString().replaceAll("[\\r\\n]", "");
                edit(text.substring(0, cursor) + paste + text.substring(cursor), cursor + paste.length());
                return true;
            }
            if (GuiScreen.isKeyComboCtrlA(keyCode)) {
                edit("", 0);
                return true;
            }
            switch (keyCode) {
                case Keyboard.KEY_BACK -> {
                    if (cursor > 0) {
                        edit(text.substring(0, cursor - 1) + text.substring(cursor), cursor - 1);
                    }
                }
                case Keyboard.KEY_DELETE -> {
                    if (cursor < text.length()) {
                        edit(text.substring(0, cursor) + text.substring(cursor + 1), cursor);
                    }
                }
                case Keyboard.KEY_LEFT -> cursor = Math.max(0, cursor - 1);
                case Keyboard.KEY_RIGHT -> cursor = Math.min(text.length(), cursor + 1);
                case Keyboard.KEY_HOME -> cursor = 0;
                case Keyboard.KEY_END -> cursor = text.length();
                case Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER, Keyboard.KEY_ESCAPE -> {
                    UiScreen screen = screen();
                    if (screen != null) {
                        screen.setFocus(null);
                    }
                }
                default -> {
                    if (!ChatAllowedCharacters.isAllowedCharacter(typed)) {
                        return false;
                    }
                    String s = String.valueOf(typed);
                    edit(text.substring(0, cursor) + s + text.substring(cursor), cursor + 1);
                }
            }
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
        protected void renderSelf(int mx, int my, float delta) {
            boolean hot = dragging || contains(mx, my);
            Ui.fill(x, y, width, height, Ui.PANEL_BACKGROUND);
            Ui.fill(x + 1, y + 1, (int) ((width - 2) * value), height - 2, Ui.QUICK_BACKGROUND);
            int knobX = x + (int) ((width - 4) * value);
            Ui.fill(knobX, y, 4, height, hot ? Ui.KNOB_HOVER : Ui.KNOB);
            Ui.outline(x, y, width, height, hot ? Ui.FOCUS_OUTLINE : Ui.PANEL_OUTLINE);
            Ui.centeredText(Math.round(value * 100) + "%", x, y, width, height, Ui.WHITE);
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
            v = Math.round(MathHelper.clamp(v, 0.0, 1.0) * 100) / 100.0;
            if (v != value) {
                value = v;
                onChange.accept(v);
            }
        }
    }

    public static class Row extends UiElement {
        public String label;

        public Row(String label) {
            this.label = label;
        }

        @Override
        protected void renderSelf(int mx, int my, float delta) {
            Ui.fill(x, y, width, height, Ui.PANEL_BACKGROUND);
            Ui.textInBox(label, x + 5, y, width - 10, height, Ui.WHITE, false);
        }

        @Override
        protected void renderOverlay(int mx, int my, float delta) {
            Ui.outline(x, y, width, height, contains(mx, my) ? Ui.FOCUS_OUTLINE : Ui.ROW_OUTLINE);
        }
    }

    public static class InfoRow extends Row {
        private final Supplier<String> value;

        public InfoRow(String label, Supplier<String> value) {
            super(label);
            this.value = value;
        }

        @Override
        protected void renderSelf(int mx, int my, float delta) {
            super.renderSelf(mx, my, delta);
            Ui.textInBox(value.get(), x + 5, y, width - 10, height, Ui.WHITE, true);
        }
    }
}
