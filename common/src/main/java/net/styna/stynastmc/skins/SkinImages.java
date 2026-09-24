package net.styna.stynastmc.skins;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.styna.stynastmc.StynaSTMC;

/**
 * Reads skin PNGs of any resolution that is a multiple of the vanilla 64x64 layout and uploads them
 * as dynamic textures. Model UVs are normalized, so the higher resolution needs no other change.
 */
public final class SkinImages {
    private static final int UNIT = 64;

    private SkinImages() {
    }

    /** A skin uploaded to the GPU, at its source resolution. */
    public record Loaded(Identifier texture, int width, int height) {
    }

    public static boolean isSupported(int width, int height) {
        return width >= UNIT && width % UNIT == 0 && (height == width || height * 2 == width);
    }

    public static Loaded load(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return upload(NativeImage.read(in), file.getFileName().toString());
        }
    }

    public static Loaded load(byte[] png, String label) throws IOException {
        return upload(NativeImage.read(png), label);
    }

    private static Loaded upload(NativeImage image, String label) throws IOException {
        if (!isSupported(image.getWidth(), image.getHeight())) {
            int width = image.getWidth();
            int height = image.getHeight();
            image.close();
            throw new IOException("Unsupported skin size " + width + "x" + height);
        }
        NativeImage processed = process(image);
        Identifier id = textureId(label, processed);
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, processed));
        return new Loaded(id, processed.getWidth(), processed.getHeight());
    }

    public static void release(Identifier texture) {
        Minecraft.getInstance().getTextureManager().release(texture);
    }

    private static Identifier textureId(String label, NativeImage image) {
        StringBuilder name = new StringBuilder();
        for (char c : label.toLowerCase(Locale.ROOT).toCharArray()) {
            name.append("abcdefghijklmnopqrstuvwxyz0123456789_.-/".indexOf(c) < 0 ? '_' : c);
        }
        int hash = (label + image.getWidth() + "x" + image.getHeight() + System.nanoTime()).hashCode();
        return StynaSTMC.id("custom_skins/" + name + "_" + Integer.toHexString(hash));
    }

    /** Vanilla's legacy fixups, with every coordinate scaled to the skin's resolution. */
    public static NativeImage process(NativeImage image) {
        int scale = image.getWidth() / UNIT;
        boolean legacy = image.getHeight() * 2 == image.getWidth();
        if (legacy) {
            NativeImage grown = new NativeImage(image.getWidth(), image.getWidth(), true);
            grown.copyFrom(image);
            image.close();
            image = grown;
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

    private static void fillRect(NativeImage image, int scale, int x, int y, int width, int height, int pixel) {
        image.fillRect(x * scale, y * scale, width * scale, height * scale, pixel);
    }

    private static void copyRect(NativeImage image, int scale, int x, int y, int offsetX, int offsetY, int width, int height) {
        image.copyRect(x * scale, y * scale, offsetX * scale, offsetY * scale, width * scale, height * scale, true, false);
    }

    private static void notchTransparencyHack(NativeImage image, int scale, int x0, int y0, int x1, int y1) {
        for (int x = x0 * scale; x < x1 * scale; x++) {
            for (int y = y0 * scale; y < y1 * scale; y++) {
                if (ARGB.alpha(image.getPixel(x, y)) < 128) {
                    return;
                }
            }
        }
        for (int x = x0 * scale; x < x1 * scale; x++) {
            for (int y = y0 * scale; y < y1 * scale; y++) {
                image.setPixel(x, y, image.getPixel(x, y) & 0xFFFFFF);
            }
        }
    }

    private static void setNoAlpha(NativeImage image, int scale, int x0, int y0, int x1, int y1) {
        for (int x = x0 * scale; x < x1 * scale; x++) {
            for (int y = y0 * scale; y < y1 * scale; y++) {
                image.setPixel(x, y, ARGB.opaque(image.getPixel(x, y)));
            }
        }
    }
}
