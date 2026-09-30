package net.styna.stynastmc.hotreload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.CommandEncoder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.hotreload.mixin.SpriteContentsAccessor;
import net.styna.stynastmc.hotreload.mixin.TextureAtlasAccessor;
import net.styna.stynastmc.hotreload.mixin.TextureAtlasSpriteAccessor;
import net.styna.stynastmc.hotreload.mixin.TextureManagerAccessor;

/** Swaps texture pixels on the GPU without a resource reload. */
public final class LiveTextures {
    private LiveTextures() {
    }

    /** Atlas sprites built from {@code ns:textures/<path>.png}. Atlases name sprites with or without their folder. */
    public static List<TextureAtlasSprite> sprites(String namespace, String path) {
        List<String> names = new ArrayList<>();
        names.add(path);
        if (path.startsWith("gui/sprites/")) {
            names.add(path.substring("gui/sprites/".length()));
        }
        int slash = path.indexOf('/');
        if (slash > 0) {
            names.add(path.substring(slash + 1));
        }
        List<TextureAtlasSprite> found = new ArrayList<>();
        for (AbstractTexture texture : textures()) {
            if (!(texture instanceof TextureAtlas atlas)) {
                continue;
            }
            try {
                for (String name : names) {
                    TextureAtlasSprite sprite = atlas.getSprite(Identifier.fromNamespaceAndPath(namespace, name));
                    if (sprite != atlas.missingSprite()) {
                        found.add(sprite);
                        break;
                    }
                }
            } catch (IllegalStateException notStitched) {
                // Atlas is mid-reload; there is nothing to patch yet.
            }
        }
        return found;
    }

    private static List<AbstractTexture> textures() {
        return List.copyOf(((TextureManagerAccessor) Minecraft.getInstance().getTextureManager()).stynastmc$byPath().values());
    }

    public static TextureAtlas atlas(TextureAtlasSprite sprite) {
        return (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(sprite.atlasLocation());
    }

    /** False when the sprite can't be patched in place (size changed or animated), which needs a full reload. */
    public static boolean canWrite(TextureAtlasSprite sprite, int width, int height) {
        SpriteContents contents = sprite.contents();
        return !contents.isAnimated() && contents.width() == width && contents.height() == height;
    }

    /** Writes every mip level of {@code image} over the sprite. Takes ownership of the image. */
    public static void write(TextureAtlasSprite sprite, NativeImage image) {
        TextureAtlas atlas = atlas(sprite);
        SpriteContents contents = sprite.contents();
        SpriteContentsAccessor extra = (SpriteContentsAccessor) contents;
        int maxMip = ((TextureAtlasAccessor) atlas).stynastmc$maxMipLevel();
        NativeImage[] levels = MipmapGenerator.generateMipLevels(contents.name(), new NativeImage[] {image}, maxMip,
                extra.stynastmc$mipmapStrategy(), extra.stynastmc$alphaCutoffBias(), contents.transparency());
        int padding = ((TextureAtlasSpriteAccessor) sprite).stynastmc$padding();
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try {
            for (int mip = 0; mip < levels.length && mip <= maxMip; mip++) {
                encoder.writeToTexture(atlas.getTexture(), levels[mip], mip, 0, (sprite.getX() + padding) >> mip, (sprite.getY() + padding) >> mip);
            }
        } finally {
            image.close();
            for (NativeImage level : levels) {
                if (level != image) {
                    level.close();
                }
            }
        }
    }

    /** Re-reads the current top resource for an atlas sprite and writes it back. */
    public static boolean restore(TextureAtlasSprite sprite, Identifier file) {
        try (var in = Minecraft.getInstance().getResourceManager().open(file)) {
            NativeImage image = NativeImage.read(in);
            if (!canWrite(sprite, image.getWidth(), image.getHeight())) {
                image.close();
                return false;
            }
            write(sprite, image);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Reloads a texture that lives outside atlases, like entity or armor textures. */
    public static boolean reloadStandalone(Identifier file) {
        // getTexture would load it on a miss, only touch textures already in use.
        AbstractTexture loaded = ((TextureManagerAccessor) Minecraft.getInstance().getTextureManager()).stynastmc$byPath().get(file);
        if (!(loaded instanceof ReloadableTexture texture)) {
            return false;
        }
        try (TextureContents contents = texture.loadContents(Minecraft.getInstance().getResourceManager())) {
            texture.apply(contents);
            return true;
        } catch (IOException e) {
            StynaSTMC.LOGGER.warn("Hot reload could not read {}", file, e);
            return false;
        }
    }
}
