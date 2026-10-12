package net.styna.stynastmc.capes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.styna.stynastmc.StynaSTMC;

/**
 * An uploaded cape, one texture per animation frame. Source sizes are padded onto the 64x32 cape layout or a multiple of it.
 * {@code elytra} is false when nothing is drawn in the elytra area (units 22..46 x 0..22).
 */
public record CapeTexture(List<Identifier> frames, int width, int height, boolean elytra) {
    private static final int FRAME_MS = 100;
    private static final int MAX_WIDTH = 2048;
    private static final int MAX_FRAMES = 256;

    record Asset(Identifier texturePath, boolean elytra) implements ClientAsset.Texture {
        @Override
        public Identifier id() {
            return this.texturePath;
        }
    }

    public Identifier current() {
        return frames.size() == 1 ? frames.getFirst() : frames.get((int) (System.currentTimeMillis() / FRAME_MS % frames.size()));
    }

    public ClientAsset.Texture asset() {
        return new Asset(current(), elytra);
    }

    public void release() {
        frames.forEach(Minecraft.getInstance().getTextureManager()::release);
    }

    public static CapeTexture load(byte[] png, boolean animated, String label) throws IOException {
        List<NativeImage> frames = frames(png, animated);
        int width = frames.getFirst().getWidth();
        int height = frames.getFirst().getHeight();
        boolean elytra = hasElytra(frames.getFirst());
        List<Identifier> ids = new ArrayList<>(frames.size());
        String base = sanitize(label) + "_" + Long.toHexString(System.nanoTime());
        for (int frame = 0; frame < frames.size(); frame++) {
            Identifier id = StynaSTMC.id("capes/" + base + "_" + frame);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, frames.get(frame)));
            ids.add(id);
        }
        return new CapeTexture(List.copyOf(ids), width, height, elytra);
    }

    private static boolean hasElytra(NativeImage image) {
        int unit = image.getWidth() / 64;
        for (int y = 0; y < 22 * unit; y++) {
            for (int x = 22 * unit; x < 46 * unit; x++) {
                if (ARGB.alpha(image.getPixel(x, y)) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The padded layout with frames stacked vertically, which is what the skin service stores. */
    public static byte[] normalized(byte[] png, boolean animated) throws IOException {
        List<NativeImage> frames = frames(png, animated);
        int width = frames.getFirst().getWidth();
        int height = frames.getFirst().getHeight();
        try (NativeImage sheet = new NativeImage(width, height * frames.size(), true)) {
            for (int frame = 0; frame < frames.size(); frame++) {
                try (NativeImage image = frames.get(frame)) {
                    image.copyRect(sheet, 0, 0, 0, frame * height, width, height, false, false);
                }
            }
            // NativeImage only encodes PNGs to a file.
            Path temp = Files.createTempFile("stynastmc-cape", ".png");
            try {
                sheet.writeToFile(temp);
                return Files.readAllBytes(temp);
            } finally {
                Files.deleteIfExists(temp);
            }
        }
    }

    private static List<NativeImage> frames(byte[] png, boolean animated) throws IOException {
        try (NativeImage image = NativeImage.read(png)) {
            int frameHeight = animated ? Math.max(1, image.getWidth() / 2) : image.getHeight();
            int count = Math.max(1, Math.min(MAX_FRAMES, image.getHeight() / frameHeight));
            if (image.getWidth() > MAX_WIDTH || frameHeight > MAX_WIDTH) {
                throw new IOException("Cape too large: " + image.getWidth() + "x" + image.getHeight());
            }
            int width = 64;
            int height = 32;
            while (width < image.getWidth() || height < frameHeight) {
                width *= 2;
                height *= 2;
            }
            List<NativeImage> frames = new ArrayList<>(count);
            for (int frame = 0; frame < count; frame++) {
                NativeImage canvas = new NativeImage(width, height, true);
                image.copyRect(canvas, 0, frame * frameHeight, 0, 0, image.getWidth(), frameHeight, false, false);
                frames.add(canvas);
            }
            return frames;
        }
    }

    private static String sanitize(String label) {
        StringBuilder name = new StringBuilder();
        for (char c : label.toLowerCase(Locale.ROOT).toCharArray()) {
            name.append("abcdefghijklmnopqrstuvwxyz0123456789_.-/".indexOf(c) < 0 ? '_' : c);
        }
        return name.toString();
    }
}
