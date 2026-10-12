package net.styna.stynastmc.skins;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.styna.stynastmc.StynaSTMC;

/**
 * Reads skin PNGs of any resolution that is a multiple of the vanilla 64x64 layout and uploads them
 * as dynamic textures. Model UVs are normalized, so the higher resolution needs no other change.
 */
public final class SkinImages {
    private static final int UNIT = 64;
    // Matches the service cap; anything larger is rejected before it reaches the GPU.
    public static final int MAX_WIDTH = 1024;

    private SkinImages() {
    }

    public static final class Loaded {
        public final ResourceLocation texture;
        public final int width;
        public final int height;

        Loaded(ResourceLocation texture, int width, int height) {
            this.texture = texture;
            this.width = width;
            this.height = height;
        }
    }

    public static boolean isSupported(int width, int height) {
        return width >= UNIT && width <= MAX_WIDTH && width % UNIT == 0 && (height == width || height * 2 == width);
    }

    public static Loaded load(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return upload(read(in), file.getFileName().toString());
        }
    }

    public static Loaded load(byte[] png, String label) throws IOException {
        return upload(read(new ByteArrayInputStream(png)), label);
    }

    private static BufferedImage read(InputStream in) throws IOException {
        BufferedImage image = ImageIO.read(in);
        if (image == null) {
            throw new IOException("Not a PNG image");
        }
        return image;
    }

    private static Loaded upload(BufferedImage image, String label) throws IOException {
        if (!isSupported(image.getWidth(), image.getHeight())) {
            throw new IOException("Unsupported skin size " + image.getWidth() + "x" + image.getHeight());
        }
        BufferedImage processed = process(image);
        ResourceLocation id = textureId(label, processed);
        Minecraft.getMinecraft().getTextureManager().loadTexture(id, new DynamicTexture(processed));
        return new Loaded(id, processed.getWidth(), processed.getHeight());
    }

    public static void release(ResourceLocation texture) {
        Minecraft.getMinecraft().getTextureManager().deleteTexture(texture);
    }

    private static ResourceLocation textureId(String label, BufferedImage image) {
        StringBuilder name = new StringBuilder();
        for (char c : label.toLowerCase(Locale.ROOT).toCharArray()) {
            name.append("abcdefghijklmnopqrstuvwxyz0123456789_.-/".indexOf(c) < 0 ? '_' : c);
        }
        int hash = (label + image.getWidth() + "x" + image.getHeight() + System.nanoTime()).hashCode();
        return StynaSTMC.id("custom_skins/" + name + "_" + Integer.toHexString(hash));
    }

    /** Vanilla's legacy fixups, with every coordinate scaled to the skin's resolution. Always returns an ARGB copy. */
    public static BufferedImage process(BufferedImage source) {
        int scale = source.getWidth() / UNIT;
        boolean legacy = source.getHeight() * 2 == source.getWidth();
        BufferedImage image = new BufferedImage(source.getWidth(), source.getWidth(), BufferedImage.TYPE_INT_ARGB);
        image.getGraphics().drawImage(source, 0, 0, null);
        if (legacy) {
            fillRect(image, scale, 0, 32, 64, 32, 0);
            copyRect(image, scale, 4, 16, 16, 32, 4, 4);
            copyRect(image, scale, 8, 16, 16, 32, 4, 4);
            copyRect(image, scale, 0, 20, 24, 32, 4, 12);
            copyRect(image, scale, 4, 20, 16, 32, 4, 12);
            copyRect(image, scale, 8, 20, 8, 32, 4, 12);
            copyRect(image, scale, 12, 20, 16, 32, 4, 12);
            copyRect(image, scale, 44, 16, -8, 32, 4, 4);
            copyRect(image, scale, 48, 16, -8, 32, 4, 4);
            copyRect(image, scale, 40, 20, 0, 32, 4, 12);
            copyRect(image, scale, 44, 20, -8, 32, 4, 12);
            copyRect(image, scale, 48, 20, -16, 32, 4, 12);
            copyRect(image, scale, 52, 20, -8, 32, 4, 12);
        }

        setNoAlpha(image, scale, 0, 0, 32, 16);
        if (legacy) {
            notchTransparencyHack(image, scale, 32, 0, 64, 32);
        }
        setNoAlpha(image, scale, 0, 16, 64, 32);
        setNoAlpha(image, scale, 16, 48, 48, 64);
        return image;
    }

    private static void fillRect(BufferedImage image, int scale, int x, int y, int width, int height, int pixel) {
        for (int px = x * scale; px < (x + width) * scale; px++) {
            for (int py = y * scale; py < (y + height) * scale; py++) {
                image.setRGB(px, py, pixel);
            }
        }
    }

    /** Copies a rect by an offset, mirrored horizontally like vanilla's legacy limb copies. */
    private static void copyRect(BufferedImage image, int scale, int x, int y, int offsetX, int offsetY, int width, int height) {
        int w = width * scale;
        int h = height * scale;
        int[] pixels = image.getRGB(x * scale, y * scale, w, h, null, 0, w);
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                image.setRGB((x + offsetX) * scale + (w - 1 - px), (y + offsetY) * scale + py, pixels[py * w + px]);
            }
        }
    }

    private static void notchTransparencyHack(BufferedImage image, int scale, int x0, int y0, int x1, int y1) {
        for (int x = x0 * scale; x < x1 * scale; x++) {
            for (int y = y0 * scale; y < y1 * scale; y++) {
                if ((image.getRGB(x, y) >>> 24) < 128) {
                    return;
                }
            }
        }
        for (int x = x0 * scale; x < x1 * scale; x++) {
            for (int y = y0 * scale; y < y1 * scale; y++) {
                image.setRGB(x, y, image.getRGB(x, y) & 0xFFFFFF);
            }
        }
    }

    private static void setNoAlpha(BufferedImage image, int scale, int x0, int y0, int x1, int y1) {
        for (int x = x0 * scale; x < x1 * scale; x++) {
            for (int y = y0 * scale; y < y1 * scale; y++) {
                image.setRGB(x, y, image.getRGB(x, y) | 0xFF000000);
            }
        }
    }
}
