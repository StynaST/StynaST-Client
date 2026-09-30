package net.styna.stynastmc.hotreload;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.styna.stynastmc.StynaSTMC;

/**
 * Watches folder resource packs whose pack.mcmeta sets {@code "StynaSTMC.hotreload": true} and applies edits live.
 * Textures and lang files update in place; anything else falls back to a normal resource reload.
 */
public final class HotReload {
    private static final String FLAG = "StynaSTMC.hotreload";
    private static final long QUIET_MS = 250;
    private static final Set<String> EXTENSIONS = Set.of("png", "json", "mcmeta", "ogg", "fsh", "vsh", "glsl", "txt");

    private static WatchService watcher;
    private static final Map<WatchKey, Path> DIRS = new HashMap<>();
    private static Set<Path> packs = Set.of();
    private static final Set<Path> pending = new LinkedHashSet<>();
    private static long lastEvent;
    private static int scanTimer;

    private HotReload() {
    }

    public static void tick(Minecraft minecraft) {
        if (++scanTimer >= 20) {
            scanTimer = 0;
            rescan(minecraft);
        }
        poll();
        if (!pending.isEmpty() && System.currentTimeMillis() - lastEvent >= QUIET_MS && minecraft.gui.overlay() == null) {
            List<Path> changes = new ArrayList<>(pending);
            pending.clear();
            apply(minecraft, changes);
        }
    }

    private static void rescan(Minecraft minecraft) {
        Set<Path> flagged = new HashSet<>();
        for (Pack pack : minecraft.getResourcePackRepository().getSelectedPacks()) {
            String id = pack.getId();
            if (!id.startsWith("file/")) {
                continue;
            }
            Path dir = minecraft.getResourcePackDirectory().resolve(id.substring("file/".length()));
            if (Files.isDirectory(dir) && flagged(dir)) {
                flagged.add(dir.toAbsolutePath().normalize());
            }
        }
        if (flagged.equals(packs)) {
            return;
        }
        packs = flagged;
        restart();
    }

    private static boolean flagged(Path pack) {
        try (Reader reader = Files.newBufferedReader(pack.resolve("pack.mcmeta"))) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root instanceof JsonObject object && object.get("pack") instanceof JsonObject meta) {
                JsonElement flag = meta.get(FLAG);
                return flag != null && flag.isJsonPrimitive() && (flag.getAsJsonPrimitive().isBoolean() ? flag.getAsBoolean()
                        : "true".equalsIgnoreCase(flag.getAsString()));
            }
        } catch (IOException | RuntimeException ignored) {
            // Missing or broken pack.mcmeta means no hot reload.
        }
        return false;
    }

    private static void restart() {
        close();
        pending.clear();
        if (packs.isEmpty()) {
            return;
        }
        try {
            watcher = FileSystems.getDefault().newWatchService();
            for (Path pack : packs) {
                Path assets = pack.resolve("assets");
                if (Files.isDirectory(assets)) {
                    registerTree(assets);
                }
            }
            StynaSTMC.LOGGER.info("Hot reload watching {}", packs);
        } catch (IOException e) {
            StynaSTMC.LOGGER.warn("Hot reload could not start", e);
            close();
        }
    }

    private static void close() {
        DIRS.clear();
        if (watcher != null) {
            try {
                watcher.close();
            } catch (IOException ignored) {
                // Nothing left to release.
            }
            watcher = null;
        }
    }

    private static void registerTree(Path root) throws IOException {
        try (Stream<Path> dirs = Files.walk(root)) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                DIRS.put(dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE), dir);
            }
        }
    }

    private static void poll() {
        if (watcher == null) {
            return;
        }
        try {
            WatchKey key;
            while ((key = watcher.poll()) != null) {
                Path dir = DIRS.get(key);
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (dir == null || event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    Path child = dir.resolve((Path) event.context());
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(child)) {
                        registerTree(child);
                    }
                    pending.add(child);
                    lastEvent = System.currentTimeMillis();
                }
                if (!key.reset()) {
                    DIRS.remove(key);
                }
            }
        } catch (ClosedWatchServiceException | IOException e) {
            StynaSTMC.LOGGER.warn("Hot reload watcher stopped", e);
            packs = Set.of();
            close();
        }
    }

    private static void apply(Minecraft minecraft, List<Path> changes) {
        boolean full = false;
        boolean lang = false;
        Set<Identifier> textures = new LinkedHashSet<>();
        for (Path change : changes) {
            String relative = relative(change);
            if (relative == null || !relevant(relative)) {
                continue;
            }
            // assets/<namespace>/<path>
            String[] parts = relative.split("/", 3);
            if (parts.length < 3) {
                continue;
            }
            String path = parts[2];
            if (path.startsWith("lang/") && path.endsWith(".json")) {
                lang = true;
            } else if (path.startsWith("textures/") && path.endsWith(".png")) {
                textures.add(Identifier.fromNamespaceAndPath(parts[1], path));
            } else {
                full = true;
            }
        }
        if (!full) {
            for (Identifier texture : textures) {
                if (!reloadTexture(texture)) {
                    full = true;
                    break;
                }
            }
        }
        if (full) {
            StynaSTMC.LOGGER.info("Hot reload: reloading resources");
            minecraft.reloadResourcePacks();
            return;
        }
        if (lang) {
            minecraft.getLanguageManager().onResourceManagerReload(minecraft.getResourceManager());
        }
        if (!textures.isEmpty() || lang) {
            SkinTotem.invalidate();
            StynaSTMC.LOGGER.info("Hot reloaded {} texture(s){}", textures.size(), lang ? " and lang" : "");
        }
    }

    private static String relative(Path change) {
        for (Path pack : packs) {
            if (change.startsWith(pack)) {
                return pack.relativize(change).toString().replace('\\', '/');
            }
        }
        return null;
    }

    // Editors write swap and backup files next to the real one, only react to resource file types.
    private static boolean relevant(String relative) {
        String name = relative.substring(relative.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return !name.startsWith(".") && dot > 0 && EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /** False means the change needs a full reload (new file, size change, animation, deleted). */
    private static boolean reloadTexture(Identifier file) {
        String path = file.getPath();
        String spriteName = path.substring("textures/".length(), path.length() - ".png".length());
        List<TextureAtlasSprite> sprites = LiveTextures.sprites(file.getNamespace(), spriteName);
        if (sprites.isEmpty()) {
            return LiveTextures.reloadStandalone(file);
        }
        NativeImage image;
        try (var in = Minecraft.getInstance().getResourceManager().open(file)) {
            image = NativeImage.read(in);
        } catch (IOException e) {
            return false;
        }
        try {
            for (TextureAtlasSprite sprite : sprites) {
                if (!LiveTextures.canWrite(sprite, image.getWidth(), image.getHeight())) {
                    return false;
                }
            }
            for (TextureAtlasSprite sprite : sprites) {
                NativeImage copy = new NativeImage(image.getWidth(), image.getHeight(), false);
                copy.copyFrom(image);
                LiveTextures.write(sprite, copy);
            }
            return true;
        } finally {
            image.close();
        }
    }
}
