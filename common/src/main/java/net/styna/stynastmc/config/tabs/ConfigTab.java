package net.styna.stynastmc.config.tabs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.electronwill.nightconfig.core.UnmodifiableConfig;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiPanel;
import net.styna.stynastmc.ui.UiWidgets;

/**
 * Editor for any {@link ModConfigSpec}: sections on the left, the selected section's values on the right.
 * Edits apply immediately; the file is written when leaving the tab.
 */
public class ConfigTab implements Tab {
    private static final Pattern COLOR = Pattern.compile("#?[0-9a-fA-F]{6}");
    private static final Pattern STOP_COLOR = Pattern.compile(".*=\\s*(#?[0-9a-fA-F]{6})\\s*");
    private static final int ROW_HEIGHT = 20;
    private static final int NAV_ITEM = 14;

    private final Component title;
    private final ModConfigSpec spec;
    private final Runnable onChange;
    private final List<Section> sections = new ArrayList<>();
    private int selected;
    private double contentScroll;
    private boolean dirty;
    private UiPanel content;

    /** A config section. Header sections only group their children and hold no values. */
    private record Section(List<String> path, List<ModConfigSpec.ConfigValue<?>> values) {
    }

    public ConfigTab(Component title, ModConfigSpec spec, Runnable onChange) {
        this.title = title;
        this.spec = spec;
        this.onChange = onChange;
    }

    @Override
    public Component title() {
        return title;
    }

    private void collect(UnmodifiableConfig config, List<String> path) {
        List<ModConfigSpec.ConfigValue<?>> values = new ArrayList<>();
        List<Map.Entry<String, UnmodifiableConfig>> subsections = new ArrayList<>();
        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof ModConfigSpec.ConfigValue<?> configValue) {
                values.add(configValue);
            } else if (value instanceof UnmodifiableConfig sub) {
                subsections.add(Map.entry(entry.getKey(), sub));
            }
        }
        if (!path.isEmpty()) {
            sections.add(new Section(path, values));
        }
        for (Map.Entry<String, UnmodifiableConfig> sub : subsections) {
            List<String> subPath = new ArrayList<>(path);
            subPath.add(sub.getKey());
            collect(sub.getValue(), subPath);
        }
    }

    @Override
    public void build(UiElement area) {
        if (sections.isEmpty()) {
            collect(spec.getValues(), List.of());
            selected = sections.stream().filter(s -> !s.values().isEmpty()).findFirst().map(sections::indexOf).orElse(0);
        }
        if (!spec.isLoaded()) {
            area.add(new UiWidgets.Label(Component.translatable("stynastmc.ui.config_not_loaded"), Ui.ERROR))
                    .bounds(area.x + 10, area.y + 10, area.width - 20, 12);
            return;
        }

        int navWidth = Math.max(110, (int) (area.width * 0.28));
        UiPanel nav = area.add(new UiPanel()).bounds(area.x + 10, area.y + 10, navWidth, area.height - 20);
        nav.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 5);
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            if (section.values().isEmpty()) {
                nav.place(new UiWidgets.Label(label(section.path().getLast()), Ui.MUTED).scale(0.85F), 12, 2).x += 2;
                continue;
            }
            // A group is a section plus every deeper section after it, boxed so each one reads as a unit.
            int end = i + 1;
            while (end < sections.size() && isInside(sections.get(end), section)) {
                end++;
            }
            UiElement box = nav.place(new NavGroup(), (end - i) * NAV_ITEM + 4, 5);
            for (int j = i; j < end; j++) {
                int index = j;
                int depth = sections.get(j).path().size() - section.path().size();
                MutableComponent name = Component.empty();
                if (depth > 0) {
                    name.append(Component.literal("  ".repeat(depth - 1) + (lastChild(j, end) ? "└ " : "├ ")).withColor(0x777777));
                }
                name.append(label(sections.get(j).path().getLast()));
                UiWidgets.Button button = box.add(new UiWidgets.Button(name, () -> select(index)).transparent());
                button.alignLeft = true;
                button.selected = j == selected;
                button.bounds(box.x + 2, box.y + 2 + (j - i) * NAV_ITEM, box.width - 4, NAV_ITEM);
            }
            i = end - 1;
        }

        content = area.add(new UiPanel()).bounds(nav.endX() + 10, area.y + 10, area.endX() - nav.endX() - 20, area.height - 20);
        content.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 10);
        buildSection(sections.get(selected));
        content.setScroll(contentScroll);
    }

    private static boolean isInside(Section section, Section parent) {
        return section.path().size() > parent.path().size() && section.path().subList(0, parent.path().size()).equals(parent.path());
    }

    /** Whether no later section in the group shares this one's depth, so it draws the closing branch. */
    private boolean lastChild(int index, int end) {
        int depth = sections.get(index).path().size();
        for (int k = index + 1; k < end; k++) {
            int d = sections.get(k).path().size();
            if (d == depth) {
                return false;
            }
            if (d < depth) {
                return true;
            }
        }
        return true;
    }

    private static class NavGroup extends UiElement {
        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            Ui.fill(g, x, y, width, height, Ui.QUICK_BACKGROUND);
            Ui.outline(g, x, y, width, height, Ui.PANEL_OUTLINE);
        }
    }

    private void select(int index) {
        selected = index;
        contentScroll = 0;
        rebuild();
    }

    private void rebuild() {
        if (content != null) {
            contentScroll = content.scroll();
            content.screen().rebuild();
        }
    }

    private void buildSection(Section section) {
        List<Component> crumbs = section.path().stream().map(ConfigTab::label).toList();
        Component heading = crumbs.stream().reduce((a, b) -> Component.empty().append(a).append(" › ").append(b)).orElse(title);
        content.place(UiWidgets.group(heading), 17, Ui.GAP);
        String comment = spec.getLevelComment(section.path());
        if (comment != null && !comment.isBlank()) {
            UiWidgets.Label note = new UiWidgets.Label(Component.literal(comment), Ui.MUTED);
            note.wrap = true;
            content.place(note, note.wrappedHeight(content.width - 26), 6);
        }
        for (ModConfigSpec.ConfigValue<?> value : section.values()) {
            buildValue(value);
        }
    }

    private ModConfigSpec.ValueSpec valueSpec(ModConfigSpec.ConfigValue<?> value) {
        return spec.getSpec().get(value.getPath());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void buildValue(ModConfigSpec.ConfigValue<?> value) {
        ModConfigSpec.ValueSpec valueSpec = valueSpec(value);
        String key = value.getPath().getLast();
        UiWidgets.Row row = content.place(new UiWidgets.Row(label(key)), ROW_HEIGHT, Ui.GAP);
        row.tooltip = () -> tooltip(value, valueSpec);
        Object current = value.get();
        int right = row.endX() - 3;

        if (!current.equals(value.getDefault())) {
            UiWidgets.Button reset = row.add(new UiWidgets.Button(Component.literal("⟲"), () -> {
                set((ModConfigSpec.ConfigValue) value, value.getDefault());
                rebuild();
            }));
            reset.bounds(right - 14, row.y + 3, 14, 14);
            reset.tooltip(Component.translatable("stynastmc.ui.reset_default"));
            right = reset.x - 3;
        }

        if (current instanceof Boolean bool) {
            row.add(new UiWidgets.Toggle(bool, v -> set((ModConfigSpec.ConfigValue) value, v))).bounds(right - 26, row.y + 3, 26, 14);
        } else if (current instanceof Enum<?> e) {
            Object[] constants = e.getDeclaringClass().getEnumConstants();
            UiWidgets.Button cycle = row.add(new UiWidgets.Button(() -> Component.literal(value.get().toString()), null));
            cycle.onPressButton = mouse -> {
                int index = ((Enum<?>) value.get()).ordinal() + (mouse == 1 ? constants.length - 1 : 1);
                set((ModConfigSpec.ConfigValue) value, constants[index % constants.length]);
            };
            cycle.bounds(right - 100, row.y + 2, 100, 16);
        } else if (current instanceof Number) {
            Function<String, Object> parse = numberParser(current);
            UiWidgets.TextField field = row.add(new UiWidgets.TextField(current.toString()));
            field.validator = s -> {
                try {
                    return valueSpec.test(parse.apply(s.trim()));
                } catch (NumberFormatException ex) {
                    return false;
                }
            };
            field.onChange = s -> set((ModConfigSpec.ConfigValue) value, parse.apply(s.trim()));
            field.bounds(right - 100, row.y + 2, 100, 16);
        } else if (current instanceof String s) {
            UiWidgets.TextField field = row.add(new UiWidgets.TextField(s));
            field.validator = valueSpec::test;
            field.onChange = v -> {
                set((ModConfigSpec.ConfigValue) value, v);
                field.swatch = swatch(v);
            };
            field.swatch = swatch(s);
            int w = Math.min(160, row.width / 2);
            field.bounds(right - w, row.y + 2, w, 16);
        } else if (current instanceof List<?> list) {
            buildList((ModConfigSpec.ConfigValue<List<? extends String>>) (ModConfigSpec.ConfigValue) value, valueSpec, row, right,
                    (List<String>) list);
        } else {
            UiWidgets.Label shown = row.add(new UiWidgets.Label(Component.literal(String.valueOf(current)), Ui.MUTED));
            shown.bounds(row.x, row.y, row.width - 8, row.height);
            shown.alignRight = true;
        }
    }

    private void buildList(ModConfigSpec.ConfigValue<List<? extends String>> value, ModConfigSpec.ValueSpec valueSpec, UiWidgets.Row row, int right,
            List<String> items) {
        UiWidgets.Button add = row.add(new UiWidgets.Button(Component.literal("+"), () -> {
            List<String> next = new ArrayList<>(items);
            next.add(items.isEmpty() ? "" : items.getLast());
            set(value, next);
            rebuild();
        }));
        add.bounds(right - 16, row.y + 2, 16, 16);
        add.tooltip(Component.translatable("stynastmc.ui.list_add"));
        UiWidgets.Label count = row.add(new UiWidgets.Label(Component.translatable("stynastmc.ui.list_count", items.size()), Ui.MUTED));
        count.bounds(row.x, row.y, add.x - row.x - 6, row.height);
        count.alignRight = true;

        for (int i = 0; i < items.size(); i++) {
            int index = i;
            UiElement line = content.place(new UiElement(), 16, 2);
            line.x += 14;
            line.width -= 14;
            UiWidgets.TextField field = line.add(new UiWidgets.TextField(items.get(i)));
            field.bounds(line.x, line.y, line.width - 20, 16);
            field.validator = s -> {
                List<String> next = new ArrayList<>(value.get());
                next.set(index, s);
                return valueSpec.test(next);
            };
            field.onChange = s -> {
                List<String> next = new ArrayList<>(value.get());
                next.set(index, s);
                set(value, next);
                field.swatch = swatch(s);
            };
            field.swatch = swatch(items.get(i));
            UiWidgets.Button remove = line.add(new UiWidgets.Button(Component.literal("✕"), () -> {
                List<String> next = new ArrayList<>(value.get());
                next.remove(index);
                if (valueSpec.test(next)) {
                    set(value, next);
                }
                rebuild();
            }));
            remove.bounds(field.endX() + 4, line.y, 16, 16);
            remove.textColor = Ui.ERROR;
        }
        content.reserve(0, 3);
    }

    private <T> void set(ModConfigSpec.ConfigValue<T> value, T newValue) {
        value.set(newValue);
        dirty = true;
        onChange.run();
    }

    @Override
    public void onLeave() {
        if (dirty) {
            spec.save();
            dirty = false;
        }
        if (content != null) {
            contentScroll = content.scroll();
        }
    }

    private static Function<String, Object> numberParser(Object current) {
        if (current instanceof Integer) {
            return Integer::parseInt;
        } else if (current instanceof Long) {
            return Long::parseLong;
        }
        return Double::parseDouble;
    }

    /** Swatch color for hex colors and gradient stops, otherwise null. */
    private static Integer swatch(String s) {
        String hex = null;
        if (COLOR.matcher(s.trim()).matches()) {
            hex = s.trim();
        } else {
            var stop = STOP_COLOR.matcher(s);
            if (stop.matches()) {
                hex = stop.group(1);
            }
        }
        return hex == null ? null : Integer.parseUnsignedInt(hex.startsWith("#") ? hex.substring(1) : hex, 16);
    }

    private static Component tooltip(ModConfigSpec.ConfigValue<?> value, ModConfigSpec.ValueSpec valueSpec) {
        var text = Component.empty().append(label(value.getPath().getLast()).copy().withStyle(s -> s.withBold(true)));
        String comment = valueSpec.getComment();
        if (comment != null && !comment.isBlank()) {
            text.append("\n").append(Component.literal(comment));
        }
        ModConfigSpec.Range<?> range = valueSpec.getRange();
        if (range != null) {
            text.append("\n").append(Component.translatable("stynastmc.ui.range", range.getMin(), range.getMax()).withStyle(s -> s.withColor(0x55FF55)));
        }
        text.append("\n").append(Component.translatable("stynastmc.ui.default", String.valueOf(value.getDefault())).withStyle(s -> s.withColor(0xAAAAAA)));
        return text;
    }

    /** Uses the shared stynastmc.configuration.<key> translations, falling back to Title Case. */
    static Component label(String key) {
        String translationKey = StynaSTMC.MODID + ".configuration." + key;
        if (Language.getInstance().has(translationKey)) {
            return Component.translatable(translationKey);
        }
        StringBuilder out = new StringBuilder();
        for (String word : key.split("_")) {
            if (!word.isEmpty()) {
                out.append(out.isEmpty() ? "" : " ").append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
            }
        }
        return Component.literal(out.toString());
    }
}
