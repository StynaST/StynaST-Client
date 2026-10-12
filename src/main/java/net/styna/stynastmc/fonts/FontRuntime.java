package net.styna.stynastmc.fonts;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.util.Text;

/**
 * Swaps Google Fonts into the game font without a resource reload. Characters the font lacks
 * fall through to the vanilla glyphs. A separate font backs the live preview in the fonts tab.
 */
public final class FontRuntime {
    private static TtfFont applied;
    private static FontSettings appliedSettings;
    private static TtfFont preview;
    private static FontSettings previewSettings;
    private static boolean bypass;

    private FontRuntime() {
    }

    /** The font replacing {@code renderer}'s glyphs, or null to draw vanilla. */
    public static TtfFont active(FontRenderer renderer) {
        return bypass || applied == null || renderer != Minecraft.getMinecraft().fontRenderer ? null : applied;
    }

    public static FontSettings appliedSettings() {
        return appliedSettings;
    }

    public static TtfFont preview() {
        return preview;
    }

    public static FontSettings previewSettings() {
        return previewSettings;
    }

    /** Draws with the vanilla glyphs even while a custom font is applied. */
    public static void drawVanilla(String text, int x, int y, int color) {
        bypass = true;
        try {
            Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text, x, y, color);
        } finally {
            bypass = false;
        }
    }

    public static void applyConfigured() {
        if (!FontsConfig.SPEC.isLoaded() || !FontsConfig.ENABLED.get() || Text.isBlank(FontsConfig.FAMILY.get())) {
            return;
        }
        FontSettings settings = FontsConfig.settings();
        GoogleFonts.download(settings.family, settings.variant).whenComplete((file, error) -> Minecraft.getMinecraft().addScheduledTask(() -> {
            if (error != null) {
                StynaSTMC.LOGGER.error("Could not download font {}", settings.family, error);
            } else if (FontsConfig.ENABLED.get() && settings.equals(FontsConfig.settings())) {
                apply(settings, file, e -> StynaSTMC.LOGGER.error("Could not load font {}", settings.family, e));
            }
        }));
    }

    public static void apply(FontSettings settings, Path file, Consumer<Exception> onError) {
        TtfFont old = applied;
        try {
            applied = new TtfFont(file, settings);
        } catch (IOException | RuntimeException e) {
            onError.accept(e);
            return;
        }
        appliedSettings = settings;
        close(old);
    }

    public static void resetToVanilla() {
        TtfFont old = applied;
        applied = null;
        appliedSettings = null;
        close(old);
    }

    public static void setPreview(FontSettings settings, Path file, Consumer<Exception> onError) {
        TtfFont old = preview;
        try {
            preview = new TtfFont(file, settings);
        } catch (IOException | RuntimeException e) {
            preview = null;
            previewSettings = null;
            close(old);
            onError.accept(e);
            return;
        }
        previewSettings = settings;
        close(old);
    }

    private static void close(TtfFont font) {
        if (font != null) {
            font.close();
        }
    }
}
