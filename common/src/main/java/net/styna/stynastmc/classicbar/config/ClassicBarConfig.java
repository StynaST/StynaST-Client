package net.styna.stynastmc.classicbar.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.styna.stynastmc.classicbar.api.BarOverlay;
import net.styna.stynastmc.classicbar.api.BarRegistry;
import net.styna.stynastmc.classicbar.api.BarSide;

/**
 * Every bar setting lives here so the NeoForge config screen can edit it.
 * The spec is generated from each bar's codec output, and {@link #readBar} rebuilds that JSON for decoding.
 */
public final class ClassicBarConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue TRANSITION_SPEED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PRIORITY;

    private static final Pattern COLOR = Pattern.compile("#?[0-9a-fA-F]{6}");
    private static final Pattern STOP = Pattern.compile("\\s*\\d*\\.?\\d+\\s*=\\s*#?[0-9a-fA-F]{6}\\s*");
    private static final Predicate<Object> IS_COLOR = o -> o instanceof String s && COLOR.matcher(s).matches();
    private static final Predicate<Object> IS_STOP = o -> o instanceof String s && STOP.matcher(s).matches();
    private static final Map<String, String> COMMENTS = Map.ofEntries(
            Map.entry("enabled", "Draw this bar. When false the vanilla HUD element it replaces comes back."),
            Map.entry("side", "Which side of the hotbar the bar sits on."),
            Map.entry("fitted", "Shrink the bar background to the current value instead of drawing the full width."),
            Map.entry("show_text", "Draw the value next to the bar."),
            Map.entry("show_icon", "Draw the icon next to the bar."),
            Map.entry("show_saturation", "Draw the saturation overlay on top of the bar."),
            Map.entry("show_exhaustion", "Draw the exhaustion overlay. Needs the mod on the server."),
            Map.entry("show_predicted_healing", "Preview how much the held food restores."),
            Map.entry("hardcore", "Bar color in hardcore worlds while no effect color applies."),
            Map.entry("mob_effect", "Effect that switches the bar to the under_effect colors."),
            Map.entry("primary", "Main bar color."),
            Map.entry("secondary", "Overlay color, e.g. saturation."),
            Map.entry("primary_under_effect", "Main bar color while the effect is active."),
            Map.entry("secondary_under_effect", "Overlay color while the effect is active."),
            Map.entry("color", "Bar color."));

    private static final Map<String, Node> BARS = new LinkedHashMap<>();
    private static final List<ModConfigSpec.ConfigValue<?>> VALUES = new ArrayList<>();

    private ClassicBarConfig() {
    }

    @FunctionalInterface
    private interface Node {
        JsonElement read();
    }

    /** Current config for a bar, in the shape its codec expects. */
    public static JsonElement readBar(String name) {
        return BARS.get(name).read();
    }

    public static void resetToDefaults() {
        VALUES.forEach(ClassicBarConfig::reset);
        SPEC.save();
    }

    private static <T> void reset(ModConfigSpec.ConfigValue<T> value) {
        value.set(value.getDefault());
    }

    private static <T extends ModConfigSpec.ConfigValue<?>> T track(T value) {
        VALUES.add(value);
        return value;
    }

    @SuppressWarnings("unchecked")
    private static JsonObject encode(BarOverlay bar) {
        return ((Codec<BarOverlay>) bar.codec()).encodeStart(JsonOps.INSTANCE, bar).getOrThrow().getAsJsonObject();
    }

    private static Node defineBar(ModConfigSpec.Builder builder, JsonObject json) {
        Map<String, Node> fields = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            // Flatten bar_settings so side/enabled/etc. sit directly in the bar's section.
            fields.put(entry.getKey(), entry.getKey().equals("bar_settings")
                    ? defineFields(builder, entry.getValue().getAsJsonObject())
                    : define(builder, entry.getKey(), entry.getValue()));
        }
        return () -> build(fields);
    }

    private static Node defineFields(ModConfigSpec.Builder builder, JsonObject json) {
        Map<String, Node> fields = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            fields.put(entry.getKey(), define(builder, entry.getKey(), entry.getValue()));
        }
        return () -> build(fields);
    }

    private static JsonObject build(Map<String, Node> fields) {
        JsonObject object = new JsonObject();
        fields.forEach((key, node) -> object.add(key, node.read()));
        return object;
    }

    private static Node define(ModConfigSpec.Builder builder, String key, JsonElement value) {
        if (key.equals("disables_overlay") || key.equals("type")) {
            return () -> value;
        }
        if (key.equals("color_provider")) {
            builder.comment("Bar colors, as hex like #FF0000.").push("colors");
            Node node = defineFields(builder, value.getAsJsonObject());
            builder.pop();
            return node;
        }
        if (key.equals("side")) {
            ModConfigSpec.EnumValue<BarSide> side = track(comment(builder, key)
                    .defineEnum(key, BarSide.valueOf(value.getAsString().toUpperCase(Locale.ROOT))));
            return () -> new JsonPrimitive(side.get().getSerializedName());
        }
        if (value instanceof JsonPrimitive primitive) {
            if (primitive.isBoolean()) {
                ModConfigSpec.BooleanValue bool = track(comment(builder, key).define(key, primitive.getAsBoolean()));
                return () -> new JsonPrimitive(bool.get());
            }
            if (primitive.isString()) {
                String s = primitive.getAsString();
                ModConfigSpec.ConfigValue<String> string = track(comment(builder, key)
                        .define(key, s, IS_COLOR.test(s) ? IS_COLOR : String.class::isInstance));
                return () -> new JsonPrimitive(string.get());
            }
        }
        if (value instanceof JsonArray array) {
            List<String> defaults = array.asList().stream().map(JsonElement::getAsString).toList();
            ModConfigSpec.ConfigValue<List<? extends String>> list = track(comment(builder, key)
                    .defineList(key, () -> defaults, () -> "#FFFFFF", IS_COLOR));
            return () -> {
                JsonArray out = new JsonArray();
                list.get().forEach(out::add);
                return out;
            };
        }
        if (value instanceof JsonObject object && isGradient(object)) {
            List<String> defaults = object.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue().getAsString()).toList();
            ModConfigSpec.ConfigValue<List<? extends String>> list = track(builder
                    .comment("Gradient stops as fraction=color. The bar blends between them, e.g. 0.25=#FF0000.")
                    .defineList(key, () -> defaults, () -> "0.5=#FFFFFF", IS_STOP));
            return () -> readGradient(list.get());
        }
        if (value instanceof JsonObject object) {
            builder.push(key);
            Node node = defineFields(builder, object);
            builder.pop();
            return node;
        }
        return () -> value;
    }

    private static ModConfigSpec.Builder comment(ModConfigSpec.Builder builder, String key) {
        String comment = COMMENTS.get(key);
        return comment == null ? builder : builder.comment(comment);
    }

    private static boolean isGradient(JsonObject object) {
        return !object.isEmpty() && object.keySet().stream().allMatch(k -> k.matches("\\d*\\.?\\d+"));
    }

    private static JsonObject readGradient(List<? extends String> stops) {
        JsonObject out = new JsonObject();
        stops.stream()
                .map(s -> s.split("="))
                .sorted(Comparator.comparingDouble(parts -> Float.parseFloat(parts[0].trim())))
                .forEach(parts -> out.addProperty(String.valueOf(Float.parseFloat(parts[0].trim())), parts[1].trim()));
        return out;
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");
        ENABLED = track(builder.comment("Master switch. When false the vanilla status icons are used instead.")
                .define("enabled", true));
        TRANSITION_SPEED = track(builder.comment("How fast the held food preview pulses.")
                .defineInRange("transition_speed", 3.0, 0.0, Double.MAX_VALUE));
        PRIORITY = track(builder.comment("Draw order, bottom to top on each side. Bars missing from the list go last.")
                .defineList("priority", () -> new ArrayList<>(BarRegistry.REGISTRY.keySet()), () -> "", String.class::isInstance));
        builder.pop();

        builder.push("bars");
        for (Map.Entry<String, BarOverlay> entry : BarRegistry.REGISTRY.entrySet()) {
            builder.push(entry.getKey());
            BARS.put(entry.getKey(), defineBar(builder, encode(entry.getValue())));
            builder.pop();
        }
        builder.pop();
        SPEC = builder.build();
    }
}
