package net.styna.stynastmc.hotreload;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.mixin.TextureManagerAccessor;
import net.styna.stynastmc.mixin.TextureMapAccessor;

/** Swaps texture pixels on the GPU without a resource reload. */
public final class LiveTextures {
    private LiveTextures() {
    }

    /** False means the change needs a full reload (new file, size change, animation, deleted). */
    public static boolean reload(ResourceLocation file) {
        String path = file.getPath();
        String spriteName = file.getNamespace() + ":" + path.substring("textures/".length(), path.length() - ".png".length());
        TextureMap atlas = Minecraft.getMinecraft().getTextureMapBlocks();
        TextureAtlasSprite sprite = atlas.getAtlasSprite(spriteName);
        if (sprite == atlas.getMissingSprite()) {
            return reloadStandalone(file);
        }
        BufferedImage image;
        try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(file)) {
            image = ImageIO.read(resource.getInputStream());
        } catch (IOException e) {
            return false;
        }
        if (image == null || sprite.hasAnimationMetadata() || sprite.getFrameCount() > 1
                || image.getWidth() != sprite.getIconWidth() || image.getHeight() != sprite.getIconHeight()) {
            return false;
        }
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        int[][] levels = TextureUtil.generateMipmapData(((TextureMapAccessor) atlas).stynastmc$mipmapLevels(), image.getWidth(), new int[][] {pixels});
        GlStateManager.bindTexture(atlas.getGlTextureId());
        TextureUtil.uploadTextureMipmap(levels, image.getWidth(), image.getHeight(), sprite.getOriginX(), sprite.getOriginY(), false, false);
        return true;
    }

    /** Reloads a texture that lives outside the atlas, like entity or GUI textures. */
    private static boolean reloadStandalone(ResourceLocation file) {
        // getTexture would load it on a miss, only touch textures already in use.
        ITextureObject loaded = ((TextureManagerAccessor) Minecraft.getMinecraft().getTextureManager()).stynastmc$textures().get(file);
        if (!(loaded instanceof SimpleTexture texture) || loaded instanceof ThreadDownloadImageData) {
            return false;
        }
        try {
            texture.loadTexture(Minecraft.getMinecraft().getResourceManager());
            return true;
        } catch (IOException e) {
            StynaSTMC.LOGGER.warn("Hot reload could not read {}", file, e);
            return false;
        }
    }
}
