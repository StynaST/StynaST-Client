package net.styna.stynastmc.capes;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.styna.stynastmc.StynaSTMC;

/**
 * A cape, one texture per animation frame, padded onto the 64x32 cape layout or a multiple of it.
 * {@code elytra} is false when nothing is drawn in the elytra area (units 22..46 x 0..22).
 */
public final class CapeTexture {
    private static final int FRAME_MS = 100;
    private static final int MAX_WIDTH = 2048;
    private static final int MAX_FRAMES = 256;

    public final List<ResourceLocation> frames;
    public final int width;
    public final int height;
    public final boolean elytra;

    private CapeTexture(List<ResourceLocation> frames, int width, int height, boolean elytra) {
        this.frames = frames;
        this.width = width;
        this.height = height;
        this.elytra = elytra;
    }

    public ResourceLocation current() {
        return frames.size() == 1 ? frames.get(0) : frames.get((int) (System.currentTimeMillis() / FRAME_MS % frames.size()));
    }

    public void release() {
        frames.forEach(Minecraft.getMinecraft().getTextureManager()::deleteTexture);
    }

    public static CapeTexture load(byte[] png, boolean animated, String label) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        if (image == null) {
            throw new IOException("Not a PNG image");
        }
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
        List<ResourceLocation> ids = new ArrayList<>(count);
        boolean elytra = false;
        String base = sanitize(label) + "_" + Long.toHexString(System.nanoTime());
        for (int frame = 0; frame < count; frame++) {
            BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            canvas.getGraphics().drawImage(image.getSubimage(0, frame * frameHeight, image.getWidth(), frameHeight), 0, 0, null);
            if (frame == 0) {
                elytra = hasElytra(canvas);
            }
            ResourceLocation id = StynaSTMC.id("capes/" + base + "_" + frame);
            Minecraft.getMinecraft().getTextureManager().loadTexture(id, new DynamicTexture(canvas));
            ids.add(id);
        }
        return new CapeTexture(Collections.unmodifiableList(ids), width, height, elytra);
    }

    private static boolean hasElytra(BufferedImage image) {
        int unit = image.getWidth() / 64;
        for (int y = 0; y < 22 * unit; y++) {
            for (int x = 22 * unit; x < 46 * unit; x++) {
                if ((image.getRGB(x, y) >>> 24) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String sanitize(String label) {
        StringBuilder name = new StringBuilder();
        for (char c : label.toLowerCase(Locale.ROOT).toCharArray()) {
            name.append("abcdefghijklmnopqrstuvwxyz0123456789_.-/".indexOf(c) < 0 ? '_' : c);
        }
        return name.toString();
    }
}
