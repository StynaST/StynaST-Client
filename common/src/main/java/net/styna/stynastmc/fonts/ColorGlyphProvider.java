package net.styna.stynastmc.fonts;

import org.jspecify.annotations.Nullable;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;

import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.util.ARGB;

/**
 * Uploads TTF glyphs as white RGBA instead of grayscale so they use the regular text pipelines.
 * Iris has no shader override for text_grayscale_polygon_offset, which made sign text vanish with shaders on.
 */
final class ColorGlyphProvider implements GlyphProvider {
    private final GlyphProvider delegate;
    private final FT_Face face;

    ColorGlyphProvider(GlyphProvider delegate, FT_Face face) {
        this.delegate = delegate;
        this.face = face;
    }

    @Override
    public @Nullable UnbakedGlyph getGlyph(int codepoint) {
        UnbakedGlyph glyph = delegate.getGlyph(codepoint);
        if (glyph == null) {
            return null;
        }
        return new UnbakedGlyph() {
            @Override
            public GlyphInfo info() {
                return glyph.info();
            }

            @Override
            public BakedGlyph bake(Stitcher stitcher) {
                return glyph.bake(new Stitcher() {
                    @Override
                    public BakedGlyph stitch(GlyphInfo info, GlyphBitmap bitmap) {
                        return stitcher.stitch(info, new ColorBitmap(bitmap, codepoint));
                    }

                    @Override
                    public BakedGlyph getMissing() {
                        return stitcher.getMissing();
                    }
                });
            }
        };
    }

    @Override
    public IntSet getSupportedGlyphs() {
        return delegate.getSupportedGlyphs();
    }

    @Override
    public void close() {
        delegate.close();
    }

    private final class ColorBitmap implements GlyphBitmap {
        private final GlyphBitmap base;
        private final int codepoint;

        private ColorBitmap(GlyphBitmap base, int codepoint) {
            this.base = base;
            this.codepoint = codepoint;
        }

        @Override
        public int getPixelWidth() {
            return base.getPixelWidth();
        }

        @Override
        public int getPixelHeight() {
            return base.getPixelHeight();
        }

        @Override
        public float getOversample() {
            return base.getOversample();
        }

        @Override
        public float getBearingLeft() {
            return base.getBearingLeft();
        }

        @Override
        public float getBearingTop() {
            return base.getBearingTop();
        }

        @Override
        public boolean isColored() {
            return true;
        }

        @Override
        public void upload(int x, int y, GpuTexture texture) {
            int width = getPixelWidth();
            int height = getPixelHeight();
            try (NativeImage gray = new NativeImage(NativeImage.Format.LUMINANCE, width, height, false);
                 NativeImage rgba = new NativeImage(width, height, false)) {
                boolean loaded;
                synchronized (face) {
                    loaded = gray.copyFromFont(face, (int) FreeType.FT_Get_Char_Index(face, codepoint));
                }
                if (!loaded) {
                    return;
                }
                for (int py = 0; py < height; py++) {
                    for (int px = 0; px < width; px++) {
                        rgba.setPixel(px, py, ARGB.color(gray.getLuminanceOrAlpha(px, py) & 0xFF, 0xFFFFFF));
                    }
                }
                RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, rgba, 0, 0, x, y);
            }
        }
    }
}
