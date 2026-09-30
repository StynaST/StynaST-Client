package net.styna.stynastmc.skins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.platform.Platform;

/** Local skin library: lists the configured folder, uploads skins on demand and overrides the own player's skin. */
public final class CustomSkins {
    private static final int MAX_LOADED = 48;
    private static final int MAX_LISTED = 512;

    private static final Map<Path, SkinImages.Loaded> LOADED = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Path, SkinImages.Loaded> eldest) {
            if (size() <= MAX_LOADED || eldest.getKey().equals(selected())) {
                return false;
            }
            SkinImages.release(eldest.getValue().texture());
            FILES.remove(eldest.getKey());
            CHANGES.remove(eldest.getKey());
            return true;
        }
    };
    private static final Map<Path, String> ERRORS = new HashMap<>();
    private static final Map<Path, FileState> FILES = new HashMap<>();
    private static final Map<Path, FileState> CHANGES = new HashMap<>();
    private static long nextCheck;
    private static long nextShare;
    private static SharedSkin shared;
    private static SharedSkin uploading;
    private static CompletableFuture<Void> upload;

    private static final Map<Path, List<Path>> LISTINGS = new HashMap<>();

    private CustomSkins() {
    }

    private record SharedSkin(Path file, Identifier texture, SkinModel model) {
    }

    private record FileState(FileTime modified, long size, Object key) {
        static FileState read(Path file) throws IOException {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            return new FileState(attrs.lastModifiedTime(), attrs.size(), attrs.fileKey());
        }
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        if (now < nextCheck) {
            return;
        }
        nextCheck = now + 500;
        for (Path file : List.copyOf(FILES.keySet())) {
            if (Files.notExists(file)) {
                unload(file);
                continue;
            }
            try {
                FileState current = FileState.read(file);
                if (current.equals(FILES.get(file))) {
                    CHANGES.remove(file);
                    continue;
                }
                // Wait for a stable save, keeping the last valid texture while the editor writes.
                if (!current.equals(CHANGES.put(file, current))) {
                    continue;
                }
                SkinImages.Loaded replacement = SkinImages.load(file);
                SkinImages.Loaded previous = LOADED.put(file, replacement);
                FILES.put(file, current);
                CHANGES.remove(file);
                ERRORS.remove(file);
                if (previous != null) {
                    SkinImages.release(previous.texture());
                }
            } catch (IOException | RuntimeException e) {
                String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                if (!Objects.equals(ERRORS.put(file, message), message)) {
                    StynaSTMC.LOGGER.warn("Could not reload skin {}: {}", file, message);
                }
            }
        }
        if (uploading == null && now >= nextShare) {
            shareSelected();
        }
    }

    public static CompletableFuture<Void> shareSelected() {
        Path file = selected();
        SkinImages.Loaded image = file == null ? null : LOADED.get(file);
        if (!enabled() || !SkinService.configured() || image == null || CHANGES.containsKey(file) || ERRORS.containsKey(file)) {
            return CompletableFuture.completedFuture(null);
        }
        SharedSkin current = new SharedSkin(file, image.texture(), SkinsConfig.MODEL.get());
        if (current.equals(uploading)) {
            return upload;
        }
        if (uploading == null && current.equals(shared)) {
            return CompletableFuture.completedFuture(null);
        }
        uploading = current;
        upload = SkinService.upload(file, current.model());
        upload.whenComplete((unused, error) -> Minecraft.getInstance().execute(() -> {
            if (uploading != current) {
                return;
            }
            uploading = null;
            if (error == null) {
                shared = current;
                nextShare = 0;
            } else {
                nextShare = System.currentTimeMillis() + 30_000;
                StynaSTMC.LOGGER.debug("Skin live sharing failed: {}", SkinService.message(error));
            }
        }));
        return upload;
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
            return defaultDirectory();
        }
        try {
            return Path.of(configured).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            return null;
        }
    }

    /** $CONFIG/StynaSTMC/skins, used while no folder is configured. */
    public static Path defaultDirectory() {
        Path dir = Platform.INSTANCE.modConfigDir().resolve("skins").toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            StynaSTMC.LOGGER.warn("Could not create skins folder {}", dir, e);
        }
        return dir;
    }

    public static Path selected() {
        if (!SkinsConfig.loaded()) {
            return null;
        }
        String name = SkinsConfig.SKIN.get().trim();
        if (name.isEmpty()) {
            return null;
        }
        try {
            Path file = Path.of(name);
            if (!file.isAbsolute()) {
                // Older configs stored the path relative to the skins folder.
                Path dir = directory();
                if (dir == null) {
                    return null;
                }
                file = dir.resolve(file);
            }
            file = file.toAbsolutePath().normalize();
            return Files.isRegularFile(file) ? file : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    public static boolean enabled() {
        return SkinsConfig.loaded() && SkinsConfig.ENABLED.get();
    }

    /** Subfolders then skin PNGs directly inside {@code dir}. */
    public static List<Path> list(Path dir) {
        List<Path> cached = LISTINGS.get(dir);
        if (cached != null) {
            return cached;
        }
        List<Path> listing = List.of();
        if (dir != null && Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                listing = files.filter(file -> Files.isDirectory(file)
                                || Files.isRegularFile(file) && file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .sorted(Comparator.comparing((Path file) -> !Files.isDirectory(file))
                                .thenComparing(file -> file.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .limit(MAX_LISTED)
                        .toList();
            } catch (IOException e) {
                StynaSTMC.LOGGER.warn("Could not read skins folder {}", dir, e);
            }
        }
        LISTINGS.put(dir, listing);
        return listing;
    }

    public static void refresh() {
        LISTINGS.clear();
        ERRORS.clear();
        List.copyOf(LOADED.keySet()).forEach(CustomSkins::unload);
        FILES.clear();
        CHANGES.clear();
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
            FILES.put(file, FileState.read(file));
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
        shared = null;
        nextShare = 0;
        SkinsConfig.SKIN.set(file == null ? "" : file.toAbsolutePath().normalize().toString());
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
        FILES.remove(file);
        CHANGES.remove(file);
    }

    /** Own player's skin: the selected local file when present, else the one uploaded to the skin service. */
    public static PlayerSkin apply(UUID uuid, PlayerSkin base) {
        if (!enabled()) {
            return base;
        }
        Path file = selected();
        SkinImages.Loaded loaded = file == null ? null : texture(file);
        if (loaded == null) {
            return RemoteSkins.apply(uuid, base);
        }
        return PlayerSkin.insecure(new LocalTexture(loaded.texture()), base.cape(), base.elytra(), SkinsConfig.MODEL.get().resolve(base.model()));
    }
}
