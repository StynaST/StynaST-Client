package net.styna.stynastmc.hotreload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.hotreload.mixin.TextureManagerAccessor;
import net.styna.stynastmc.platform.Platform;
import net.styna.stynastmc.skins.SkinImages;
import net.styna.stynastmc.skins.SkinsBridge;

/**
 * Turns the totem of undying into a sitting plushie of the player.
 *
 * <p>Our item model (assets/minecraft/models/item/totem_of_undying.json) is built from cubes UV mapped onto a
 * 64x64 skin layout, so the totem sprite simply holds the player's skin. It is stitched at the largest skin size
 * so any skin fits 1:1 and later skin changes are written in place. Slim arms are widened to the classic layout
 * because the model has one arm width.
 */
public final class SkinTotem {
    private static final Identifier SPRITE = Identifier.withDefaultNamespace("item/totem_of_undying");
    private static final int CANVAS = SkinImages.MAX_WIDTH;
    private static final int PLACEHOLDER = 0xFFE3B74A;

    private static final Object LOCK = new Object();
    private static NativeImage prepared;
    private static Object preparedKey;

    private static volatile boolean stitched;
    private static volatile Object written;
    private static boolean reloadRequested;
    private static Supplier<PlayerSkin> accountSkin;
    private static int timer;

    private SkinTotem() {
    }

    /** Forces the live pixels to be rewritten, e.g. after something else touched the sprite. */
    public static void invalidate() {
        written = null;
    }

    /** Called with the sprites about to be stitched into an atlas. */
    public static List<SpriteContents> substitute(List<SpriteContents> sprites) {
        int index = -1;
        for (int i = 0; i < sprites.size(); i++) {
            if (sprites.get(i).name().equals(SPRITE)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return sprites;
        }
        NativeImage image = new NativeImage(CANVAS, CANVAS, true);
        synchronized (LOCK) {
            if (prepared == null) {
                loadCache();
            }
            if (prepared != null) {
                image.copyFrom(prepared);
            } else {
                image.fillRect(0, 0, CANVAS, CANVAS, PLACEHOLDER);
            }
        }
        List<SpriteContents> result = new ArrayList<>(sprites);
        result.get(index).close();
        result.set(index, new SpriteContents(SPRITE, new FrameSize(CANVAS, CANVAS), image));
        stitched = true;
        written = null;
        return result;
    }

    public static void tick(Minecraft minecraft) {
        if (++timer < 10) {
            return;
        }
        timer = 0;
        prepare(minecraft);
        if (minecraft.gui.overlay() != null) {
            return;
        }
        Object key;
        synchronized (LOCK) {
            key = preparedKey;
        }
        if (key == null) {
            return;
        }
        List<TextureAtlasSprite> sprites = LiveTextures.sprites(SPRITE.getNamespace(), SPRITE.getPath());
        if (sprites.isEmpty()) {
            return;
        }
        TextureAtlasSprite sprite = sprites.getFirst();
        if (!stitched || !LiveTextures.canWrite(sprite, CANVAS, CANVAS)) {
            // Only possible if the atlas was built before this code ran; one reload fixes it for good.
            if (!reloadRequested) {
                reloadRequested = true;
                minecraft.reloadResourcePacks();
            }
            return;
        }
        // The atlas texture is replaced on every reload, which also wipes what we wrote.
        Object target = List.of(key, System.identityHashCode(LiveTextures.atlas(sprite).getTexture()));
        if (target.equals(written)) {
            return;
        }
        NativeImage copy = new NativeImage(CANVAS, CANVAS, true);
        synchronized (LOCK) {
            copy.copyFrom(prepared);
        }
        LiveTextures.write(sprite, copy);
        written = target;
    }

    /** Rebuilds the CPU copy whenever the skin texture or arm width changes. */
    private static void prepare(Minecraft minecraft) {
        PlayerSkin skin = currentSkin(minecraft);
        if (skin == null) {
            return;
        }
        Object key = List.of(skin.body().texturePath(), skin.model());
        synchronized (LOCK) {
            if (key.equals(preparedKey)) {
                return;
            }
        }
        NativeImage source = pixels(minecraft, skin.body().texturePath());
        if (source == null) {
            return;
        }
        NativeImage image;
        try {
            if (skin.model() == PlayerModelType.SLIM) {
                widenArms(source);
            }
            image = scale(source);
        } finally {
            source.close();
        }
        synchronized (LOCK) {
            if (prepared != null) {
                prepared.close();
            }
            prepared = image;
            preparedKey = key;
            saveCache();
        }
    }

    /** Own skin, or null on the menu while the account skin is still being fetched. */
    private static PlayerSkin currentSkin(Minecraft minecraft) {
        if (minecraft.player != null) {
            return minecraft.player.getSkin();
        }
        if (accountSkin == null) {
            accountSkin = minecraft.getSkinManager().createLookup(minecraft.getGameProfile(), false);
        }
        PlayerSkin fallback = DefaultPlayerSkin.get(minecraft.getGameProfile());
        PlayerSkin skin = SkinsBridge.forPlayer(minecraft.getGameProfile().id(), accountSkin.get());
        // Painting the default skin first would just flash before the real one arrives.
        return skin.body().texturePath().equals(fallback.body().texturePath()) ? null : skin;
    }

    /** A square copy of the skin's pixels, or null while it is still downloading. */
    private static NativeImage pixels(Minecraft minecraft, Identifier texture) {
        AbstractTexture loaded = ((TextureManagerAccessor) minecraft.getTextureManager()).stynastmc$byPath().get(texture);
        if (loaded instanceof DynamicTexture dynamic && dynamic.getPixels() != null) {
            return copy(dynamic.getPixels());
        }
        if (loaded instanceof ReloadableTexture reloadable) {
            try (TextureContents contents = TextureContents.load(minecraft.getResourceManager(), reloadable.resourceId())) {
                return copy(contents.image());
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    private static NativeImage copy(NativeImage image) {
        if (image.getWidth() != image.getHeight() || image.getWidth() < 64) {
            return null;
        }
        NativeImage copy = new NativeImage(image.getWidth(), image.getHeight(), true);
        copy.copyFrom(image);
        return copy;
    }

    /** Nearest neighbour up to the canvas so every skin pixel stays a crisp square. */
    private static NativeImage scale(NativeImage skin) {
        NativeImage out = new NativeImage(CANVAS, CANVAS, true);
        int size = skin.getWidth();
        for (int y = 0; y < CANVAS; y++) {
            int sy = y * size / CANVAS;
            for (int x = 0; x < CANVAS; x++) {
                out.setPixel(x, y, skin.getPixel(x * size / CANVAS, sy));
            }
        }
        return out;
    }

    /** Stretches each slim (3 wide) arm box into the classic 4 wide layout. */
    private static void widenArms(NativeImage skin) {
        int s = skin.getWidth() / 64;
        NativeImage original = new NativeImage(skin.getWidth(), skin.getHeight(), true);
        original.copyFrom(skin);
        try {
            for (int[] box : new int[][] {{40, 16}, {40, 32}, {32, 48}, {48, 48}}) {
                int u = box[0];
                int v = box[1];
                stretch(original, skin, s, u + 4, v, 3, 4, u + 4, 4);
                stretch(original, skin, s, u + 7, v, 3, 4, u + 8, 4);
                stretch(original, skin, s, u + 4, v + 4, 3, 12, u + 4, 4);
                stretch(original, skin, s, u + 7, v + 4, 4, 12, u + 8, 4);
                stretch(original, skin, s, u + 11, v + 4, 3, 12, u + 12, 4);
            }
        } finally {
            original.close();
        }
    }

    // Copies a rect (64 layout units) sw wide into dw wide at du, nearest sampled horizontally.
    private static void stretch(NativeImage from, NativeImage to, int s, int su, int v, int sw, int h, int du, int dw) {
        for (int y = v * s; y < (v + h) * s; y++) {
            for (int x = 0; x < dw * s; x++) {
                to.setPixel(du * s + x, y, from.getPixel(su * s + x * sw / dw, y));
            }
        }
    }

    private static Path cacheFile() {
        return Platform.INSTANCE.modConfigDir().resolve("totem.png");
    }

    // Caller holds LOCK.
    private static void loadCache() {
        Path file = cacheFile();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (var in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(in);
            if (image.getWidth() == CANVAS && image.getHeight() == CANVAS) {
                prepared = image;
            } else {
                image.close();
            }
        } catch (IOException e) {
            StynaSTMC.LOGGER.warn("Could not read cached totem {}", file, e);
        }
    }

    // Caller holds LOCK.
    private static void saveCache() {
        try {
            Files.createDirectories(cacheFile().getParent());
            prepared.writeToFile(cacheFile());
        } catch (IOException e) {
            StynaSTMC.LOGGER.warn("Could not cache totem", e);
        }
    }
}
