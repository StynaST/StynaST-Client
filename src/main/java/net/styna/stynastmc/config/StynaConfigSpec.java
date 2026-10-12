package net.styna.stynastmc.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/** Small shared TOML config model, same file format as the modern versions. */
public final class StynaConfigSpec {
    private final Map<String, Object> values;
    private final Map<List<String>, String> levelComments;
    private final Map<List<String>, ValueSpec> specs;
    private Path file;
    private long modified;

    private StynaConfigSpec(Map<String, Object> values, Map<List<String>, String> levelComments,
            Map<List<String>, ValueSpec> specs) {
        this.values = values;
        this.levelComments = levelComments;
        this.specs = specs;
    }

    public boolean isLoaded() {
        return file != null;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public String getLevelComment(List<String> path) {
        return levelComments.get(path);
    }

    public ValueSpec valueSpec(List<String> path) {
        return specs.get(path);
    }

    public void load(Path path) {
        file = path;
        if (!Files.exists(file)) {
            save();
            return;
        }
        try {
            for (ValueSpec spec : specs.values()) {
                spec.value.setParsed(spec.value.getDefault());
            }
            List<String> section = Collections.emptyList();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    section = Arrays.asList(trimmed.substring(1, trimmed.length() - 1).split("\\."));
                    continue;
                }
                int equals = trimmed.indexOf('=');
                if (equals < 1 || trimmed.startsWith("#")) {
                    continue;
                }
                List<String> key = new ArrayList<>(section);
                key.add(trimmed.substring(0, equals).trim());
                ValueSpec spec = specs.get(key);
                if (spec != null) {
                    Object value = parse(trimmed.substring(equals + 1).trim(), spec.value.getDefault());
                    if (value != null && spec.test(value)) {
                        spec.value.setParsed(value);
                    }
                }
            }
            modified = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load config " + file, e);
        }
    }

    public boolean reloadIfModified() {
        if (file == null || !Files.exists(file)) {
            return false;
        }
        try {
            if (Files.getLastModifiedTime(file).toMillis() == modified) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        load(file);
        return true;
    }

    public void save() {
        if (file == null) {
            return;
        }
        StringBuilder out = new StringBuilder();
        write(values, Collections.emptyList(), out);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, out.toString().getBytes(StandardCharsets.UTF_8));
            modified = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save config " + file, e);
        }
    }

    private void write(Map<String, Object> entries, List<String> section, StringBuilder out) {
        if (!section.isEmpty()) {
            if (out.length() > 0) {
                out.append('\n');
            }
            String comment = levelComments.get(section);
            if (comment != null) {
                out.append('#').append(comment).append('\n');
            }
            out.append('[').append(String.join(".", section)).append("]\n");
        }
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            if (entry.getValue() instanceof ConfigValue<?> value) {
                List<String> path = new ArrayList<>(section);
                path.add(entry.getKey());
                String comment = specs.get(path).comment;
                if (comment != null) {
                    out.append('#').append(comment).append('\n');
                }
                out.append(entry.getKey()).append(" = ").append(format(value.get())).append('\n');
            }
        }
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> map) {
                List<String> path = new ArrayList<>(section);
                path.add(entry.getKey());
                @SuppressWarnings("unchecked")
                Map<String, Object> child = (Map<String, Object>) map;
                write(child, path, out);
            }
        }
    }

    private static String format(Object value) {
        if (value instanceof Enum<?> e) {
            return new JsonPrimitive(e.name()).toString();
        }
        if (value instanceof String s) {
            return new JsonPrimitive(s).toString();
        }
        if (value instanceof List<?> list) {
            JsonArray array = new JsonArray();
            list.forEach(item -> array.add(new JsonPrimitive(String.valueOf(item))));
            return array.toString();
        }
        return String.valueOf(value);
    }

    private static Object parse(String raw, Object defaultValue) {
        try {
            JsonElement json = new JsonParser().parse(raw);
            if (defaultValue instanceof Boolean) return json.getAsBoolean();
            if (defaultValue instanceof Integer) return json.getAsInt();
            if (defaultValue instanceof Double) return json.getAsDouble();
            if (defaultValue instanceof String) return json.getAsString();
            if (defaultValue instanceof Enum<?> e) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Enum<?> parsed = Enum.valueOf((Class) e.getDeclaringClass(), json.getAsString());
                return parsed;
            }
            if (defaultValue instanceof List<?>) {
                List<String> parsed = new ArrayList<>();
                json.getAsJsonArray().forEach(item -> parsed.add(item.getAsString()));
                return parsed;
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    public static class ConfigValue<T> {
        private final List<String> path;
        private final T defaultValue;
        private final Predicate<Object> validator;
        private T value;

        private ConfigValue(List<String> path, T defaultValue, Predicate<Object> validator) {
            this.path = path;
            this.defaultValue = defaultValue;
            this.validator = validator;
            value = defaultValue;
        }

        public T get() { return value; }
        public T getDefault() { return defaultValue; }
        public List<String> getPath() { return path; }
        public String key() { return path.get(path.size() - 1); }
        public void set(T next) {
            if (validator.test(next)) value = next;
        }
        @SuppressWarnings("unchecked")
        private void setParsed(Object next) { value = (T) next; }
    }

    public static final class BooleanValue extends ConfigValue<Boolean> {
        private BooleanValue(List<String> path, boolean value) {
            super(path, value, Boolean.class::isInstance);
        }
    }

    public static final class IntValue extends ConfigValue<Integer> {
        private IntValue(List<String> path, int value, int min, int max) {
            super(path, value, input -> input instanceof Integer n && n >= min && n <= max);
        }
    }

    public static final class DoubleValue extends ConfigValue<Double> {
        private DoubleValue(List<String> path, double value, double min, double max) {
            super(path, value, input -> input instanceof Double n && n >= min && n <= max);
        }
    }

    public static final class EnumValue<E extends Enum<E>> extends ConfigValue<E> {
        private EnumValue(List<String> path, E value) {
            super(path, value, input -> value.getDeclaringClass().isInstance(input));
        }
    }

    public static final class Range<T> {
        private final T min;
        private final T max;

        Range(T min, T max) {
            this.min = min;
            this.max = max;
        }

        public T getMin() { return min; }
        public T getMax() { return max; }
    }

    public static final class ValueSpec {
        private final ConfigValue<?> value;
        private final String comment;
        private final Range<?> range;

        private ValueSpec(ConfigValue<?> value, String comment, Range<?> range) {
            this.value = value;
            this.comment = comment;
            this.range = range;
        }

        public boolean test(Object input) { return value.validator.test(input); }
        public String getComment() { return comment; }
        public Range<?> getRange() { return range; }
    }

    public static final class Builder {
        private final Map<String, Object> values = new LinkedHashMap<>();
        private final Map<List<String>, String> levelComments = new LinkedHashMap<>();
        private final Map<List<String>, ValueSpec> specs = new LinkedHashMap<>();
        private final List<String> section = new ArrayList<>();
        private String comment;

        public Builder comment(String text) {
            comment = text;
            return this;
        }

        public Builder push(String name) {
            section.add(name);
            if (comment != null) {
                levelComments.put(new ArrayList<>(section), comment);
                comment = null;
            }
            sectionMap();
            return this;
        }

        public Builder pop() {
            section.remove(section.size() - 1);
            return this;
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> sectionMap() {
            Map<String, Object> current = values;
            for (String part : section) {
                current = (Map<String, Object>) current.computeIfAbsent(part, key -> new LinkedHashMap<String, Object>());
            }
            return current;
        }

        private List<String> path(String name) {
            List<String> path = new ArrayList<>(section);
            path.add(name);
            return Collections.unmodifiableList(path);
        }

        private <T extends ConfigValue<?>> T add(String name, T value, Range<?> range) {
            sectionMap().put(name, value);
            specs.put(value.getPath(), new ValueSpec(value, comment, range));
            comment = null;
            return value;
        }

        public BooleanValue define(String name, boolean value) {
            return add(name, new BooleanValue(path(name), value), null);
        }

        public ConfigValue<String> define(String name, String value) {
            return define(name, value, String.class::isInstance);
        }

        public ConfigValue<String> define(String name, String value, Predicate<Object> validator) {
            return add(name, new ConfigValue<>(path(name), value, validator), null);
        }

        public IntValue defineInRange(String name, int value, int min, int max) {
            return add(name, new IntValue(path(name), value, min, max), new Range<>(min, max));
        }

        public DoubleValue defineInRange(String name, double value, double min, double max) {
            return add(name, new DoubleValue(path(name), value, min, max), new Range<>(min, max));
        }

        public <E extends Enum<E>> EnumValue<E> defineEnum(String name, E value) {
            return add(name, new EnumValue<>(path(name), value), null);
        }

        public ConfigValue<List<? extends String>> defineList(String name, Supplier<? extends List<String>> defaults,
                Predicate<Object> itemValidator) {
            Predicate<Object> validator = input -> input instanceof List<?> list && list.stream().allMatch(itemValidator);
            return add(name, new ConfigValue<>(path(name), defaults.get(), validator), null);
        }

        public StynaConfigSpec build() {
            return new StynaConfigSpec(values, levelComments, specs);
        }
    }
}
