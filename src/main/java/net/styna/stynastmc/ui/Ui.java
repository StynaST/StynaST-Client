package net.styna.stynastmc.ui;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
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
    public static final int SUCCESS = 0xFF55FF55;
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

    public static final ResourceLocation TAB_BACKGROUND = sprite("ui/tab_background");
    public static final ResourceLocation TAB_FOREGROUND = sprite("ui/tab_foreground");
    public static final ResourceLocation TAB_STRIP_BACKGROUND = sprite("ui/tab_strip_background");
    public static final ResourceLocation TAB_STRIP_FOREGROUND = sprite("ui/tab_strip_foreground");
    public static final ResourceLocation TAB_ENTRY = sprite("ui/tab_strip_entry");
    public static final ResourceLocation TAB_ENTRY_SELECTED = sprite("ui/tab_strip_entry_selected");
    public static final ResourceLocation FOLDER = sprite("ui/folder");
    public static final ResourceLocation SETTINGS = sprite("ui/settings");
    public static final ResourceLocation CLOSE = sprite("ui/close");

    private static final Map<ResourceLocation, int[]> SLICES = new HashMap<>();
    private static final Deque<int[]> SCISSORS = new ArrayDeque<>();
    private static int offsetY;

    private Ui() {
    }

    public static ResourceLocation sprite(String name) {
        return StynaSTMC.id("textures/gui/sprites/" + name + ".png");
    }

    public static FontRenderer font() {
        return Minecraft.getMinecraft().fontRenderer;
    }

    public static int lineHeight() {
        return font().FONT_HEIGHT;
    }

    public static int width(String text) {
        return font().getStringWidth(text);
    }

    public static void fill(int x, int y, int w, int h, int color) {
        if (color != 0 && w > 0 && h > 0) {
            Gui.drawRect(x, y, x + w, y + h, color);
        }
    }

    public static void outline(int x, int y, int w, int h, int color) {
        if (color == 0) {
            return;
        }
        fill(x, y, w, 1, color);
        fill(x, y + h - 1, w, 1, color);
        fill(x, y + 1, 1, h - 2, color);
        fill(x + w - 1, y + 1, 1, h - 2, color);
    }

    public static void sprite(ResourceLocation texture, int x, int y, int w, int h) {
        sprite(texture, x, y, w, h, 0xFFFFFFFF);
    }

    /** Draws a GUI sprite, honoring the nine slice scaling in its .mcmeta like modern versions do. */
    public static void sprite(ResourceLocation texture, int x, int y, int w, int h, int tint) {
        int[] slice = slice(texture);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color((tint >> 16 & 0xFF) / 255.0F, (tint >> 8 & 0xFF) / 255.0F, (tint & 0xFF) / 255.0F, (tint >>> 24) / 255.0F);
        int tw = slice[0];
        int th = slice[1];
        int b = slice[2];
        if (b <= 0 || w < b * 2 || h < b * 2) {
            Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, tw, th, w, h, tw, th);
        } else {
            int iw = tw - b * 2;
            int ih = th - b * 2;
            int cw = w - b * 2;
            int ch = h - b * 2;
            part(x, y, 0, 0, b, b, b, b, tw, th);
            part(x + w - b, y, tw - b, 0, b, b, b, b, tw, th);
            part(x, y + h - b, 0, th - b, b, b, b, b, tw, th);
            part(x + w - b, y + h - b, tw - b, th - b, b, b, b, b, tw, th);
            part(x + b, y, b, 0, iw, b, cw, b, tw, th);
            part(x + b, y + h - b, b, th - b, iw, b, cw, b, tw, th);
            part(x, y + b, 0, b, b, ih, b, ch, tw, th);
            part(x + w - b, y + b, tw - b, b, b, ih, b, ch, tw, th);
            part(x + b, y + b, b, b, iw, ih, cw, ch, tw, th);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void part(int x, int y, int u, int v, int uw, int vh, int w, int h, int tw, int th) {
        if (w > 0 && h > 0) {
            Gui.drawScaledCustomSizeModalRect(x, y, u, v, uw, vh, w, h, tw, th);
        }
    }

    /** {width, height, border} from the sprite's .mcmeta, or the image size with no border. */
    private static int[] slice(ResourceLocation texture) {
        return SLICES.computeIfAbsent(texture, Ui::readSlice);
    }

    private static int[] readSlice(ResourceLocation texture) {
        int[] out = {16, 16, 0};
        try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(texture)) {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(resource.getInputStream());
            out[0] = image.getWidth();
            out[1] = image.getHeight();
        } catch (Exception e) {
            return out;
        }
        ResourceLocation meta = new ResourceLocation(texture.getNamespace(), texture.getPath() + ".mcmeta");
        try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(meta);
             Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject scaling = new JsonParser().parse(reader).getAsJsonObject().getAsJsonObject("gui").getAsJsonObject("scaling");
            if ("nine_slice".equals(scaling.get("type").getAsString())) {
                JsonElement border = scaling.get("border");
                out[2] = border.isJsonPrimitive() ? border.getAsInt() : border.getAsJsonObject().get("left").getAsInt();
            }
        } catch (Exception ignored) {
            // No metadata means the sprite stretches.
        }
        return out;
    }

    public static void clearSpriteCache() {
        SLICES.clear();
    }

    public static void text(String text, int x, int y, int color, float scale) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, 1);
        font().drawStringWithShadow(text, 0, 0, color);
        GlStateManager.popMatrix();
    }

    public static void textInBox(String text, int x, int y, int w, int h, int color, boolean alignRight) {
        int tx = alignRight ? x + w - width(text) : x;
        font().drawStringWithShadow(text, tx, y + (h - lineHeight()) / 2 + 1, color);
    }

    public static void centeredText(String text, int x, int y, int w, int h, int color) {
        font().drawStringWithShadow(text, x + (w - width(text)) / 2, y + (h - lineHeight()) / 2 + 1, color);
    }

    public static void item(ItemStack stack, int x, int y) {
        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GlStateManager.popMatrix();
    }

    public static void itemWithCount(ItemStack stack, int x, int y, String count) {
        item(stack, x, y);
        GlStateManager.enableDepth();
        Minecraft.getMinecraft().getRenderItem().renderItemOverlayIntoGUI(font(), stack, x, y, count);
        GlStateManager.disableDepth();
    }

    public static void tooltip(String text, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        FontRenderer font = font();
        List<String> lines = new java.util.ArrayList<>();
        for (String part : text.split("\n")) {
            lines.addAll(font.listFormattedStringToWidth(part, 200));
        }
        int w = 0;
        for (String line : lines) {
            w = Math.max(w, font.getStringWidth(line));
        }
        w += 6;
        int h = lines.size() * font.FONT_HEIGHT + 4;
        int x = Math.min(mouseX + 10, screenWidth - w - 2);
        int y = Math.min(mouseY + 10, screenHeight - h - 2);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 400);
        fill(x, y, w, h, BLACK);
        outline(x, y, w, h, TOOLTIP_OUTLINE);
        for (int i = 0; i < lines.size(); i++) {
            font.drawString(lines.get(i), x + 3, y + 3 + i * font.FONT_HEIGHT, WHITE);
        }
        GlStateManager.popMatrix();
    }

    /** Vertical translation applied by scrolling panels, so scissor boxes land in screen space. */
    public static void translateY(int dy) {
        offsetY += dy;
        GlStateManager.translate(0, dy, 0);
    }

    public static void enableScissor(int x1, int y1, int x2, int y2) {
        int[] box = {x1, y1 + offsetY, x2, y2 + offsetY};
        int[] top = SCISSORS.peek();
        if (top != null) {
            box = new int[] {Math.max(box[0], top[0]), Math.max(box[1], top[1]), Math.min(box[2], top[2]), Math.min(box[3], top[3])};
        }
        SCISSORS.push(box);
        apply(box);
    }

    public static void disableScissor() {
        SCISSORS.pop();
        int[] top = SCISSORS.peek();
        if (top == null) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            apply(top);
        }
    }

    private static void apply(int[] box) {
        Minecraft mc = Minecraft.getMinecraft();
        int scale = new ScaledResolution(mc).getScaleFactor();
        int w = Math.max(0, box[2] - box[0]);
        int h = Math.max(0, box[3] - box[1]);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(box[0] * scale, mc.displayHeight - box[3] * scale, w * scale, h * scale);
    }
}
