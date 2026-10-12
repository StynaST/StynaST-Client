package net.styna.stynastmc.fonts;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;

/**
 * A TrueType font rasterized with AWT into one texture per 256 character block.
 * Glyphs are drawn oversampled and scaled down, with the vanilla baseline 7 GUI pixels below the line top.
 */
public final class TtfFont {
    private static final float BASELINE = 7.0F;
    private static final int PAD = 2;
    private static final int PAGE_WIDTH = 1024;

    private final Font font;
    private final FontRenderContext context = new FontRenderContext(null, true, true);
    private final float oversample;
    private final float shiftX;
    private final float shiftY;
    private final Map<Integer, Page> pages = new HashMap<>();

    private static final class Glyph {
        final int texture;
        final float u0;
        final float v0;
        final float u1;
        final float v1;
        final float x;
        final float y;
        final float width;
        final float height;
        final float advance;

        Glyph(int texture, float u0, float v0, float u1, float v1, float x, float y, float width, float height, float advance) {
            this.texture = texture;
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.advance = advance;
        }
    }

    private static final class Page {
        final int texture;
        final Glyph[] glyphs = new Glyph[256];

        Page(int texture) {
            this.texture = texture;
        }
    }

    public TtfFont(Path file, FontSettings settings) throws IOException {
        try {
            oversample = settings.oversample;
            shiftX = settings.shiftX;
            shiftY = settings.shiftY;
            font = Font.createFont(Font.TRUETYPE_FONT, file.toFile()).deriveFont(settings.size * settings.oversample);
        } catch (FontFormatException e) {
            throw new IOException("Unsupported font file: " + e.getMessage(), e);
        }
    }

    public boolean canDisplay(char c) {
        return c != '§' && (c == ' ' || c == ' ' || font.canDisplay(c));
    }

    public float advance(char c) {
        if (c == ' ') {
            c = ' ';
        }
        Glyph glyph = glyph(c);
        return glyph == null ? 0 : glyph.advance;
    }

    /** Draws one glyph with its top-left line corner at (x, y) in the current color and returns its advance. */
    public float draw(char c, float x, float y, boolean italic) {
        if (c == ' ') {
            c = ' ';
        }
        Glyph glyph = glyph(c);
        if (glyph == null) {
            return 0;
        }
        if (glyph.width > 0) {
            float left = x + glyph.x + shiftX;
            float top = y + BASELINE + shiftY + glyph.y;
            float slant = italic ? 1.0F : 0.0F;
            GlStateManager.bindTexture(glyph.texture);
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            GL11.glTexCoord2f(glyph.u0, glyph.v0);
            GL11.glVertex3f(left + slant, top, 0.0F);
            GL11.glTexCoord2f(glyph.u0, glyph.v1);
            GL11.glVertex3f(left - slant, top + glyph.height, 0.0F);
            GL11.glTexCoord2f(glyph.u1, glyph.v0);
            GL11.glVertex3f(left + glyph.width + slant, top, 0.0F);
            GL11.glTexCoord2f(glyph.u1, glyph.v1);
            GL11.glVertex3f(left + glyph.width - slant, top + glyph.height, 0.0F);
            GL11.glEnd();
        }
        return glyph.advance;
    }

    /** Plain text with an optional vanilla style shadow, for previews. Formatting codes are skipped. */
    public float drawString(String text, float x, float y, int color, boolean shadow) {
        if (shadow) {
            drawString(text, x + 1, y + 1, (color & 0xFCFCFC) >> 2 | color & 0xFF000000, false);
        }
        int alpha = color >>> 24 == 0 ? 0xFF : color >>> 24;
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, alpha / 255.0F);
        float start = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            x += draw(c, x, y, false);
        }
        GlStateManager.color(1, 1, 1, 1);
        return x - start;
    }

    public float width(String text) {
        float width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            width += advance(c);
        }
        return width;
    }

    private Glyph glyph(char c) {
        Page page = pages.computeIfAbsent(c >> 8, this::bake);
        return page.glyphs[c & 0xFF];
    }

    /** Rasterizes one 256 character block into a shelf packed texture. */
    private Page bake(int block) {
        LineMetrics metrics = font.getLineMetrics("Ag", context);
        int rowHeight = (int) Math.ceil(metrics.getAscent() + metrics.getDescent()) + PAD * 2 + 4;
        GlyphVector[] vectors = new GlyphVector[256];
        Rectangle[] bounds = new Rectangle[256];
        int[] cellX = new int[256];
        int[] cellY = new int[256];
        int cx = 0;
        int cy = 0;
        for (int i = 0; i < 256; i++) {
            char c = (char) (block << 8 | i);
            if (!canDisplay(c) || Character.isISOControl(c)) {
                continue;
            }
            vectors[i] = font.createGlyphVector(context, new char[] {c});
            bounds[i] = vectors[i].getPixelBounds(context, 0, 0);
            int w = bounds[i].width + PAD * 2;
            int h = bounds[i].height + PAD * 2;
            if (cx + w > PAGE_WIDTH) {
                cx = 0;
                cy += rowHeight;
            }
            rowHeight = Math.max(rowHeight, h);
            cellX[i] = cx;
            cellY[i] = cy;
            cx += w;
        }
        int height = Math.max(1, cy + rowHeight);
        BufferedImage image = new BufferedImage(PAGE_WIDTH, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setColor(java.awt.Color.WHITE);
        for (int i = 0; i < 256; i++) {
            if (vectors[i] != null && bounds[i].width > 0) {
                g.drawGlyphVector(vectors[i], cellX[i] - bounds[i].x + PAD, cellY[i] - bounds[i].y + PAD);
            }
        }
        g.dispose();

        int texture = TextureUtil.glGenTextures();
        TextureUtil.uploadTextureImageAllocate(texture, image, true, true);
        Page page = new Page(texture);
        for (int i = 0; i < 256; i++) {
            if (vectors[i] == null) {
                continue;
            }
            Rectangle b = bounds[i];
            float advance = (float) vectors[i].getGlyphMetrics(0).getAdvanceX() / oversample;
            if (b.width <= 0) {
                page.glyphs[i] = new Glyph(texture, 0, 0, 0, 0, 0, 0, 0, 0, advance);
                continue;
            }
            int w = b.width + PAD * 2;
            int h = b.height + PAD * 2;
            page.glyphs[i] = new Glyph(texture,
                    cellX[i] / (float) PAGE_WIDTH, cellY[i] / (float) height,
                    (cellX[i] + w) / (float) PAGE_WIDTH, (cellY[i] + h) / (float) height,
                    (b.x - PAD) / oversample, (b.y - PAD) / oversample, w / oversample, h / oversample, advance);
        }
        return page;
    }

    public void close() {
        for (Page page : pages.values()) {
            TextureUtil.deleteTexture(page.texture);
        }
        pages.clear();
    }
}
