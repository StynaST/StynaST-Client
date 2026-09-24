package net.styna.stynastmc.fonts;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import com.mojang.blaze3d.platform.TextureUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.GlyphStitcher;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.styna.stynastmc.StynaSTMC;

/**
 * Swaps Google Fonts into the game without a resource reload.
 * The default font set is reloaded in place with our TTF provider in front of the vanilla ones, which stay as fallback
 * for glyphs the font lacks. A separate font set backs the live preview in the fonts tab.
 */
public final class FontRuntime {
    public static final Identifier PREVIEW_FONT = StynaSTMC.id("preview");
    public static final Style PREVIEW_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(PREVIEW_FONT));
    /** The default font as the resource packs define it, for side by side comparison. */
    public static final Identifier VANILLA_FONT = StynaSTMC.id("vanilla");
    public static final Style VANILLA_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(VANILLA_FONT));

    private static FontManager manager;
    private static List<GlyphProvider.Conditional> vanillaProviders = List.of();
    private static GlyphProvider applied;
    private static FontSettings appliedSettings;
    private static GlyphProvider preview;
    private static FontSettings previewSettings;
    private static FontSet previewSet;

    private FontRuntime() {
    }

    /** Runs after every font reload, which rebuilds all font sets from resource packs. */
    public static void onFontsReloaded(FontManager fontManager) {
        manager = fontManager;
        FontSet defaultSet = manager.fontSets.get(Minecraft.DEFAULT_FONT);
        vanillaProviders = List.copyOf(defaultSet.allProviders);
        previewSet = new FontSet(new GlyphStitcher(Minecraft.getInstance().getTextureManager(), PREVIEW_FONT));
        manager.fontSets.put(PREVIEW_FONT, previewSet);
        FontSet vanillaSet = new FontSet(new GlyphStitcher(Minecraft.getInstance().getTextureManager(), VANILLA_FONT));
        manager.fontSets.put(VANILLA_FONT, vanillaSet);
        reload(vanillaSet, null);
        reload(defaultSet, applied);
        reload(previewSet, preview);
        if (applied == null) {
            applyConfigured();
        }
    }

    public static boolean isReady() {
        return manager != null;
    }

    public static FontSettings appliedSettings() {
        return appliedSettings;
    }

    public static FontSettings previewSettings() {
        return previewSettings;
    }

    /** Applies the font saved in the config, downloading it first if needed. */
    public static void applyConfigured() {
        if (!isReady() || !FontsConfig.SPEC.isLoaded() || !FontsConfig.ENABLED.get() || FontsConfig.FAMILY.get().isBlank()) {
            return;
        }
        FontSettings settings = FontsConfig.settings();
        GoogleFonts.download(settings.family(), settings.variant()).whenComplete((file, error) -> Minecraft.getInstance().execute(() -> {
            if (error != null) {
                StynaSTMC.LOGGER.error("Could not download font {}", settings.family(), error);
            } else if (FontsConfig.ENABLED.get() && settings.equals(FontsConfig.settings())) {
                apply(settings, file, e -> StynaSTMC.LOGGER.error("Could not load font {}", settings.family(), e));
            }
        }));
    }

    public static void apply(FontSettings settings, Path file, Consumer<Exception> onError) {
        GlyphProvider old = applied;
        try {
            applied = load(file, settings);
        } catch (IOException | RuntimeException e) {
            onError.accept(e);
            return;
        }
        appliedSettings = settings;
        reload(manager.fontSets.get(Minecraft.DEFAULT_FONT), applied);
        close(old);
    }

    public static void resetToVanilla() {
        GlyphProvider old = applied;
        applied = null;
        appliedSettings = null;
        if (isReady()) {
            reload(manager.fontSets.get(Minecraft.DEFAULT_FONT), null);
        }
        close(old);
    }

    public static void setPreview(FontSettings settings, Path file, Consumer<Exception> onError) {
        if (!isReady()) {
            return;
        }
        GlyphProvider old = preview;
        try {
            preview = load(file, settings);
        } catch (IOException | RuntimeException e) {
            preview = null;
            previewSettings = null;
            reload(previewSet, null);
            close(old);
            onError.accept(e);
            return;
        }
        previewSettings = settings;
        reload(previewSet, preview);
        close(old);
    }

    private static void reload(FontSet set, GlyphProvider custom) {
        List<GlyphProvider.Conditional> providers = new ArrayList<>();
        if (custom != null) {
            providers.add(new GlyphProvider.Conditional(custom, FontOption.Filter.ALWAYS_PASS));
        }
        providers.addAll(vanillaProviders);
        set.reload(providers, FontManager.getFontOptions(Minecraft.getInstance().options));
    }

    private static void close(GlyphProvider provider) {
        if (provider != null) {
            provider.close();
        }
    }

    /** Same FreeType setup as vanilla's TTF provider, minus its TrueType-only check so CFF fonts load too. */
    private static GlyphProvider load(Path file, FontSettings settings) throws IOException {
        ByteBuffer buffer;
        try (InputStream in = Files.newInputStream(file)) {
            buffer = TextureUtil.readResource(in);
        }
        FT_Face face = null;
        try {
            synchronized (FreeTypeUtil.LIBRARY_LOCK) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    PointerBuffer pointer = stack.mallocPointer(1);
                    FreeTypeUtil.assertError(FreeType.FT_New_Memory_Face(FreeTypeUtil.getLibrary(), buffer, 0L, pointer), "Initializing font face");
                    face = FT_Face.create(pointer.get());
                }
                FreeTypeUtil.assertError(FreeType.FT_Select_Charmap(face, FreeType.FT_ENCODING_UNICODE), "Find unicode charmap");
            }
            return new TrueTypeGlyphProvider(buffer, face, settings.size(), settings.oversample(), settings.shiftX(), settings.shiftY(), "");
        } catch (RuntimeException e) {
            if (face != null) {
                FreeType.FT_Done_Face(face);
            }
            MemoryUtil.memFree(buffer);
            throw e;
        }
    }
}
