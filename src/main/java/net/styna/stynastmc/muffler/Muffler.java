package net.styna.stynastmc.muffler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.config.ConfigKey;
import net.styna.stynastmc.config.StynaConfigScreen;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.util.Text;

/** Client side sound muffler in the style of Extreme Sound Muffler: per sound volume, globally or inside anchors. */
public final class Muffler {
    private static final Path FILE = StynaSTMC.modConfigFile("sound_muffler.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final KeyBinding KEY = new KeyBinding("key.stynastmc.muffler", Keyboard.KEY_NONE, ConfigKey.CATEGORY);
    private static final int RECENT_LIMIT = 300;
    private static final int BUTTON_ID = 0x57A5;

    public static boolean enabled = true;
    public static boolean inventoryButton = true;
    public static final Map<ResourceLocation, Float> global = new LinkedHashMap<>();
    public static final List<Anchor> anchors = new ArrayList<>();
    public static final Map<ResourceLocation, SoundCategory> recent = new LinkedHashMap<>();

    private static ISound preview;
    private static NoteButton button;

    public static final class Anchor {
        public String name;
        public String dimension;
        public double x;
        public double y;
        public double z;
        public int radius;
        public final Map<ResourceLocation, Float> sounds = new LinkedHashMap<>();

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
    public static float factor(ISound instance) {
        if (!enabled || instance == preview || instance == null) {
            return 1.0F;
        }
        ResourceLocation id = instance.getSoundLocation();
        Float base = global.get(id);
        float factor = base == null ? 1.0F : base;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!anchors.isEmpty() && instance.getAttenuationType() != ISound.AttenuationType.NONE && minecraft.world != null) {
            String dim = dimension();
            for (Anchor anchor : anchors) {
                Float volume = anchor.sounds.get(id);
                if (volume != null && volume < factor && anchor.contains(dim, instance.getXPosF(), instance.getYPosF(), instance.getZPosF())) {
                    factor = volume;
                }
            }
        }
        return factor;
    }

    public static String dimension() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.world == null ? "" : minecraft.world.provider.getDimensionType().getName();
    }

    /** Reapplies volumes to sounds that are already playing. */
    public static void refresh() {
        Minecraft minecraft = Minecraft.getMinecraft();
        for (SoundCategory category : SoundCategory.values()) {
            if (category != SoundCategory.MASTER) {
                minecraft.getSoundHandler().setSoundLevel(category, minecraft.gameSettings.getSoundLevel(category));
            }
        }
    }

    public static void preview(ResourceLocation id) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (preview != null) {
            minecraft.getSoundHandler().stopSound(preview);
        }
        preview = PositionedSoundRecord.getMasterRecord(new SoundEvent(id), 1.0F);
        minecraft.getSoundHandler().playSound(preview);
    }

    public static void onPlaySound(ISound sound) {
        if (sound == null || sound == preview) {
            return;
        }
        recent.remove(sound.getSoundLocation());
        recent.put(sound.getSoundLocation(), sound.getCategory());
        if (recent.size() > RECENT_LIMIT) {
            recent.remove(recent.keySet().iterator().next());
        }
    }

    public static void tick(Minecraft minecraft) {
        boolean pressed = false;
        while (KEY.isPressed()) {
            pressed = true;
        }
        if (pressed && minecraft.currentScreen == null) {
            open(null);
        }
    }

    private static void open(GuiScreen parent) {
        Minecraft.getMinecraft().displayGuiScreen(new StynaConfigScreen(parent, StynaConfigScreen.MUFFLER_TAB));
    }

    public static void onScreenInit(GuiScreen screen, List<GuiButton> buttons) {
        button = null;
        if (!inventoryButton || !(screen instanceof GuiInventory inventory)) {
            return;
        }
        button = new NoteButton();
        place(inventory);
        buttons.add(button);
    }

    public static boolean onButton(GuiScreen screen, GuiButton pressed) {
        if (pressed == button && screen instanceof GuiInventory) {
            open(screen);
            return true;
        }
        return false;
    }

    // The recipe book shifts the inventory without reinitializing, so follow it every frame.
    public static void onScreenRender(GuiScreen screen) {
        if (button != null && screen instanceof GuiInventory inventory) {
            place(inventory);
        }
    }

    private static void place(GuiInventory screen) {
        button.x = screen.getGuiLeft() + screen.getXSize() - 18;
        button.y = screen.getGuiTop() + 4;
    }

    public static void load() {
        global.clear();
        anchors.clear();
        if (!Files.exists(FILE)) {
            return;
        }
        try {
            JsonObject json = new JsonParser().parse(new String(Files.readAllBytes(FILE), StandardCharsets.UTF_8)).getAsJsonObject();
            enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
            inventoryButton = !json.has("inventory_button") || json.get("inventory_button").getAsBoolean();
            readSounds(json.getAsJsonObject("sounds"), global);
            if (json.has("anchors")) {
                for (JsonElement element : json.getAsJsonArray("anchors")) {
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
        JsonArray list = new JsonArray();
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
            Files.write(FILE, GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            StynaSTMC.LOGGER.error("Failed to write {}", FILE, e);
        }
    }

    private static void readSounds(JsonObject json, Map<ResourceLocation, Float> into) {
        if (json == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            into.put(new ResourceLocation(entry.getKey()), MathHelper.clamp(entry.getValue().getAsFloat(), 0.0F, 1.0F));
        }
    }

    private static JsonObject writeSounds(Map<ResourceLocation, Float> sounds) {
        JsonObject json = new JsonObject();
        sounds.forEach((id, volume) -> json.addProperty(id.toString(), volume));
        return json;
    }

    /** Small inventory button with a note glyph, standing in for the modern sprite icon button. */
    private static final class NoteButton extends GuiButton {
        NoteButton() {
            super(BUTTON_ID, 0, 0, 13, 13, "");
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            Ui.fill(x, y, width, height, hovered ? 0xFF8B8B8B : 0xFF6F6F6F);
            Ui.outline(x, y, width, height, hovered ? Ui.WHITE : Ui.BLACK);
            mc.fontRenderer.drawString("♪", x + 4, y + 3, Ui.WHITE);
            if (hovered && mc.currentScreen != null) {
                Ui.tooltip(Text.tr("stynastmc.ui.muffler"), mouseX, mouseY, mc.currentScreen.width, mc.currentScreen.height);
            }
        }
    }
}
