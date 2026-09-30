package net.styna.stynastmc.muffler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.config.ConfigKey;
import net.styna.stynastmc.config.StynaConfigScreen;
import net.styna.stynastmc.platform.Platform;

/** Client side sound muffler in the style of Extreme Sound Muffler: per sound volume, globally or inside anchors. */
public final class Muffler {
    private static final Path FILE = Platform.INSTANCE.modConfigFile("sound_muffler.json",
            Platform.INSTANCE.configDir().resolve(StynaSTMC.MODID).resolve("muffler.json"));
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final KeyMapping KEY = new KeyMapping("key.stynastmc.muffler", InputConstants.UNKNOWN.getValue(), ConfigKey.CATEGORY);
    private static final Identifier ICON = Identifier.withDefaultNamespace("icon/music_notes");
    private static final int RECENT_LIMIT = 300;

    public static boolean enabled = true;
    public static boolean inventoryButton = true;
    public static final Map<Identifier, Float> global = new LinkedHashMap<>();
    public static final List<Anchor> anchors = new ArrayList<>();
    public static final Map<Identifier, SoundSource> recent = new LinkedHashMap<>();

    private static SoundInstance preview;
    private static Button button;

    public static final class Anchor {
        public String name;
        public String dimension;
        public double x;
        public double y;
        public double z;
        public int radius;
        public final Map<Identifier, Float> sounds = new LinkedHashMap<>();

        public boolean contains(String dim, double px, double py, double pz) {
            double dx = px - x;
            double dy = py - y;
            double dz = pz - z;
            return dimension.equals(dim) && dx * dx + dy * dy + dz * dz <= (double) radius * radius;
        }
    }

    private Muffler() {
    }

    /** Volume multiplier for a sound, 1 when untouched. */
    public static float factor(SoundInstance instance) {
        if (!enabled || instance == preview) {
            return 1.0F;
        }
        Identifier id = instance.getIdentifier();
        float factor = global.getOrDefault(id, 1.0F);
        Minecraft minecraft = Minecraft.getInstance();
        if (!anchors.isEmpty() && !instance.isRelative() && minecraft.level != null) {
            String dim = dimension();
            for (Anchor anchor : anchors) {
                Float volume = anchor.sounds.get(id);
                if (volume != null && volume < factor && anchor.contains(dim, instance.getX(), instance.getY(), instance.getZ())) {
                    factor = volume;
                }
            }
        }
        return factor;
    }

    public static String dimension() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? "" : minecraft.level.dimension().identifier().toString();
    }

    /** Reapplies volumes to sounds that are already playing. */
    public static void refresh() {
        Minecraft.getInstance().getSoundManager().refreshCategoryVolume(SoundSource.MASTER);
    }

    public static void preview(Identifier id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (preview != null) {
            minecraft.getSoundManager().stop(preview);
        }
        preview = SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(id), 1.0F, 1.0F);
        minecraft.getSoundManager().play(preview);
    }

    public static void onPlaySound(SoundInstance sound) {
        if (sound == preview) {
            return;
        }
        recent.remove(sound.getIdentifier());
        recent.put(sound.getIdentifier(), sound.getSource());
        if (recent.size() > RECENT_LIMIT) {
            recent.remove(recent.keySet().iterator().next());
        }
    }

    public static void tick(Minecraft minecraft) {
        boolean pressed = false;
        while (KEY.consumeClick()) {
            pressed = true;
        }
        if (pressed && minecraft.gui.screen() == null) {
            open(null);
        }
    }

    private static void open(net.minecraft.client.gui.screens.Screen parent) {
        Minecraft.getInstance().gui.setScreen(new StynaConfigScreen(parent, StynaConfigScreen.MUFFLER_TAB));
    }

    public static void onScreenInit(net.minecraft.client.gui.screens.Screen screen, java.util.function.Consumer<Button> addWidget) {
        if (!inventoryButton || !(screen instanceof InventoryScreen inventory)) {
            return;
        }
        button = SpriteIconButton.builder(Component.translatable("stynastmc.ui.muffler"), b -> open(inventory), true)
                .sprite(ICON, 9, 9).size(13, 13).build();
        button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("stynastmc.ui.muffler")));
        place(inventory);
        addWidget.accept(button);
    }

    // The recipe book shifts the inventory without reinitializing, so follow it every frame.
    public static void onScreenRender(net.minecraft.client.gui.screens.Screen screen) {
        if (button != null && screen instanceof InventoryScreen inventory) {
            place(inventory);
        }
    }

    private static void place(InventoryScreen screen) {
        button.setPosition(screen.leftPos + screen.imageWidth - 18, screen.topPos + 4);
    }

    public static void load() {
        global.clear();
        anchors.clear();
        if (!Files.exists(FILE)) {
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
            inventoryButton = !json.has("inventory_button") || json.get("inventory_button").getAsBoolean();
            readSounds(json.getAsJsonObject("sounds"), global);
            if (json.has("anchors")) {
                for (var element : json.getAsJsonArray("anchors")) {
                    JsonObject a = element.getAsJsonObject();
                    Anchor anchor = new Anchor();
                    anchor.name = a.get("name").getAsString();
                    anchor.dimension = a.get("dimension").getAsString();
                    anchor.x = a.get("x").getAsDouble();
                    anchor.y = a.get("y").getAsDouble();
                    anchor.z = a.get("z").getAsDouble();
                    anchor.radius = a.get("radius").getAsInt();
                    readSounds(a.getAsJsonObject("sounds"), anchor.sounds);
                    anchors.add(anchor);
                }
            }
        } catch (IOException | RuntimeException e) {
            StynaSTMC.LOGGER.error("Failed to read {}", FILE, e);
        }
    }

    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        json.addProperty("inventory_button", inventoryButton);
        json.add("sounds", writeSounds(global));
        var list = new com.google.gson.JsonArray();
        for (Anchor anchor : anchors) {
            JsonObject a = new JsonObject();
            a.addProperty("name", anchor.name);
            a.addProperty("dimension", anchor.dimension);
            a.addProperty("x", anchor.x);
            a.addProperty("y", anchor.y);
            a.addProperty("z", anchor.z);
            a.addProperty("radius", anchor.radius);
            a.add("sounds", writeSounds(anchor.sounds));
            list.add(a);
        }
        json.add("anchors", list);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(json));
        } catch (IOException e) {
            StynaSTMC.LOGGER.error("Failed to write {}", FILE, e);
        }
    }

    private static void readSounds(JsonObject json, Map<Identifier, Float> into) {
        if (json == null) {
            return;
        }
        for (var entry : json.entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            if (id != null) {
                into.put(id, Math.clamp(entry.getValue().getAsFloat(), 0.0F, 1.0F));
            }
        }
    }

    private static JsonObject writeSounds(Map<Identifier, Float> sounds) {
        JsonObject json = new JsonObject();
        sounds.forEach((id, volume) -> json.addProperty(id.toString(), volume));
        return json;
    }
}
