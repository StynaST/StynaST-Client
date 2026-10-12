package net.styna.stynastmc.capes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.platform.Platform;

/** Local cape PNGs. Only this client sees them. */
public final class CustomCapes {
    private static final int MAX_LISTED = 512;

    private static final Map<Path, CapeTexture> LOADED = new HashMap<>();
    private static final Map<Path, String> ERRORS = new HashMap<>();
    private static List<Path> listing;

    private CustomCapes() {
    }

    public static Path directory() {
        if (!CapesConfig.loaded()) {
            return null;
        }
        String configured = CapesConfig.DIRECTORY.get().trim();
        if (configured.isEmpty()) {
            return defaultDirectory();
        }
        try {
            return Path.of(configured).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            return null;
        }
    }

    public static Path defaultDirectory() {
        Path dir = Platform.INSTANCE.modConfigDir().resolve("capes").toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            StynaSTMC.LOGGER.warn("Could not create capes folder {}", dir, e);
        }
        return dir;
    }

    public static List<Path> list() {
        if (listing != null) {
            return listing;
        }
        listing = List.of();
        Path dir = directory();
        if (dir != null && Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                listing = files.filter(file -> Files.isRegularFile(file) && file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .sorted(Comparator.comparing(file -> file.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .limit(MAX_LISTED)
                        .toList();
            } catch (IOException e) {
                StynaSTMC.LOGGER.warn("Could not read capes folder {}", dir, e);
            }
        }
        return listing;
    }

    public static CapeTexture texture(Path file) {
        CapeTexture loaded = LOADED.get(file);
        if (loaded != null || ERRORS.containsKey(file)) {
            return loaded;
        }
        try {
            byte[] png = Files.readAllBytes(file);
            String name = file.getFileName().toString();
            // name.anim.png holds animation frames stacked vertically.
            boolean animated = name.toLowerCase(Locale.ROOT).endsWith(".anim.png");
            loaded = CapeTexture.load(png, animated, "custom/" + name);
            LOADED.put(file, loaded);
            return loaded;
        } catch (IOException | RuntimeException e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            ERRORS.put(file, message);
            StynaSTMC.LOGGER.warn("Could not load cape {}: {}", file, message);
            return null;
        }
    }

    public static String error(Path file) {
        return ERRORS.get(file);
    }

    public static void refresh() {
        listing = null;
        ERRORS.clear();
        LOADED.values().forEach(CapeTexture::release);
        LOADED.clear();
    }
}
