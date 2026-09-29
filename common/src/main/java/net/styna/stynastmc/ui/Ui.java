package net.styna.stynastmc.ui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.styna.stynastmc.StynaSTMC;

/** Palette, sprites and draw helpers, matching TCDCommons as used by Better Statistics Screen (LGPL-3.0). */
public final class Ui {
    public static final int PANEL_BACKGROUND = 0x50000000;
    public static final int PANEL_OUTLINE = 0x50FFFFFF;
    public static final int FOCUS_OUTLINE = 0xFFAAFFFF;
    public static final int BLACK = 0xFF000000;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int ROW_OUTLINE = 0x35000000;
    public static final int GROUP_LABEL = 0xFFFFFF00;
    public static final int MUTED = 0xFFAAAAAA;
    public static final int ERROR = 0xFFFF5555;
    public static final int CARD_BACKGROUND = 0x3F000000;
    public static final int CARD_OUTLINE = 0x10000000;
    public static final int QUICK_BACKGROUND = 0x22000000;
    public static final int HOVER_FILL = 0x33FFFFFF;
    public static final int KNOB = 0x42FFFFFF;
    public static final int KNOB_HOVER = 0x6EFFFFFF;
    public static final int TOOLTIP_OUTLINE = 0xFF290F6B;
    public static final int TAB_TEXT = 0xEEFFFFFF;
    public static final int TAB_TEXT_SELECTED = 0xFFC5E862;
    public static final int GAP = 3;

    public static final Identifier TAB_BACKGROUND = sprite("tab_background");
    public static final Identifier TAB_FOREGROUND = sprite("tab_foreground");
    public static final Identifier TAB_STRIP_BACKGROUND = sprite("tab_strip_background");
    public static final Identifier TAB_STRIP_FOREGROUND = sprite("tab_strip_foreground");
    public static final Identifier TAB_ENTRY = sprite("tab_strip_entry");
    public static final Identifier TAB_ENTRY_SELECTED = sprite("tab_strip_entry_selected");
    public static final Identifier FOLDER = sprite("folder");
    public static final Identifier SETTINGS = sprite("settings");
    public static final Identifier CLOSE = sprite("close");

    private Ui() {
    }

    private static Identifier sprite(String name) {
        return StynaSTMC.id("ui/" + name);
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static void fill(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (color != 0) {
            g.fill(x, y, x + w, y + h, color);
        }
    }

    public static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (color != 0) {
            g.outline(x, y, w, h, color);
        }
    }

    public static void sprite(GuiGraphicsExtractor g, Identifier sprite, int x, int y, int w, int h) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h);
    }

    public static void text(GuiGraphicsExtractor g, Component text, int x, int y, int color, float scale) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.text(font(), text, 0, 0, color, true);
        g.pose().popMatrix();
    }

    public static void textInBox(GuiGraphicsExtractor g, Component text, int x, int y, int w, int h, int color, boolean alignRight) {
        Font font = font();
        int width = font.width(text);
        int tx = alignRight ? x + w - width : x;
        g.text(font, text, tx, y + (h - font.lineHeight) / 2 + 1, color, true);
    }

    public static void centeredText(GuiGraphicsExtractor g, Component text, int x, int y, int w, int h, int color) {
        Font font = font();
        g.text(font, text, x + (w - font.width(text)) / 2, y + (h - font.lineHeight) / 2 + 1, color, true);
    }

    public static void tooltip(GuiGraphicsExtractor g, Component text, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        Font font = font();
        List<FormattedCharSequence> lines = font.split(text, 200);
        int w = lines.stream().mapToInt(font::width).max().orElse(0) + 6;
        int h = lines.size() * font.lineHeight + 4;
        int x = Math.min(mouseX + 10, screenWidth - w - 2);
        int y = Math.min(mouseY + 10, screenHeight - h - 2);
        g.nextStratum();
        fill(g, x, y, w, h, BLACK);
        outline(g, x, y, w, h, TOOLTIP_OUTLINE);
        for (int i = 0; i < lines.size(); i++) {
            g.text(font, lines.get(i), x + 3, y + 3 + i * font.lineHeight, WHITE, false);
        }
    }
}
