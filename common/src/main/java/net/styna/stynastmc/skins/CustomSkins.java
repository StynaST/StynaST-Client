package net.styna.stynastmc.skins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.styna.stynastmc.StynaSTMC;

/** Local skin library: lists the configured folder, uploads skins on demand and overrides the own player's skin. */
public final class CustomSkins {
    private static final int MAX_LOADED = 48;
    private static final int MAX_LISTED = 512;
    private static final int SEARCH_DEPTH = 3;

    private static final Map<Path, SkinImages.Loaded> LOADED = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Path, SkinImages.Loaded> eldest) {
            if (size() <= MAX_LOADED || eldest.getKey().equals(selected())) {
                return false;
            }
            SkinImages.release(eldest.getValue().texture());
            return true;
        }
    };
    private static final Map<Path, String> ERRORS = new HashMap<>();

    private static List<Path> listing;

    private CustomSkins() {
    }

    /** A texture uploaded by this mod, standing in for a downloaded skin. */
    private record LocalTexture(Identifier texturePath) implements ClientAsset.Texture {
        @Override
        public Identifier id() {
            return this.texturePath;
        }
    }

    public static Path directory() {
        if (!SkinsConfig.loaded()) {
            return null;
        }
        String configured = SkinsConfig.DIRECTORY.get().trim();
        if (configured.isEmpty()) {
            return null;
        }
        try {
            return Path.of(configured);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    public static Path selected() {
        Path dir = directory();
        if (dir == null || !SkinsConfig.loaded()) {
            return null;
        }
        String name = SkinsConfig.SKIN.get().trim();
        if (name.isEmpty()) {
            return null;
        }
        try {
            Path file = dir.resolve(name);
            return Files.isRegularFile(file) ? file : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    public static boolean enabled() {
        return SkinsConfig.loaded() && SkinsConfig.ENABLED.get();
    }

    /** Skin files in the configured folder, cached until {@link #refresh()}. */
    public static List<Path> list() {
        if (listing != null) {
            return listing;
        }
        Path dir = directory();
        if (dir == null || !Files.isDirectory(dir)) {
            return listing = List.of();
        }
        try (Stream<Path> files = Files.walk(dir, SEARCH_DEPTH)) {
            listing = files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.comparing(file -> file.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .limit(MAX_LISTED)
                    .toList();
        } catch (IOException e) {
            StynaSTMC.LOGGER.warn("Could not read skins folder {}", dir, e);
            listing = List.of();
        }
        return listing;
    }

    public static void refresh() {
        listing = null;
        ERRORS.clear();
        List.copyOf(LOADED.keySet()).forEach(CustomSkins::unload);
    }

    /** Path shown in the UI and stored in the config, relative to the skins folder when possible. */
    public static String name(Path file) {
        Path dir = directory();
        if (dir != null) {
            try {
                return dir.relativize(file).toString().replace('\\', '/');
            } catch (IllegalArgumentException ignored) {
                // Different root, fall through to the absolute path.
            }
        }
        return file.toString();
    }

    public static SkinImages.Loaded texture(Path file) {
        SkinImages.Loaded loaded = LOADED.get(file);
        if (loaded != null || ERRORS.containsKey(file)) {
            return loaded;
        }
        try {
            loaded = SkinImages.load(file);
            LOADED.put(file, loaded);
            return loaded;
        } catch (IOException | RuntimeException e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            ERRORS.put(file, message);
            StynaSTMC.LOGGER.warn("Could not load skin {}: {}", file, message);
            return null;
        }
    }

    public static String error(Path file) {
        return ERRORS.get(file);
    }

    public static void select(Path file) {
        SkinsConfig.SKIN.set(file == null ? "" : name(file));
        SkinsConfig.ENABLED.set(file != null);
        SkinsConfig.save();
    }

    public static void setModel(SkinModel model) {
        SkinsConfig.MODEL.set(model);
        SkinsConfig.save();
    }

    public static void unload(Path file) {
        SkinImages.Loaded loaded = LOADED.remove(file);
        if (loaded != null) {
            SkinImages.release(loaded.texture());
        }
        ERRORS.remove(file);
    }

    /** Swaps the body texture of the account skin for the selected local one. */
    public static PlayerSkin apply(PlayerSkin base) {
        if (!enabled()) {
            return base;
        }
        Path file = selected();
        if (file == null) {
            return base;
        }
        SkinImages.Loaded loaded = texture(file);
        if (loaded == null) {
            return base;
        }
        return PlayerSkin.insecure(new LocalTexture(loaded.texture()), base.cape(), base.elytra(), SkinsConfig.MODEL.get().resolve(base.model()));
    }
}
