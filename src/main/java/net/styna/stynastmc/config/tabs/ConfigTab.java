package net.styna.stynastmc.config.tabs;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.config.StynaConfigSpec;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiPanel;
import net.styna.stynastmc.ui.UiWidgets;
import net.styna.stynastmc.util.Text;

/**
 * Editor for any {@link StynaConfigSpec}: sections on the left, the selected section's values on the right.
 * Edits apply immediately; the file is written when leaving the tab.
 */
public class ConfigTab implements Tab {
    private static final Pattern COLOR = Pattern.compile("#?[0-9a-fA-F]{6}");
    private static final Pattern STOP_COLOR = Pattern.compile(".*=\\s*(#?[0-9a-fA-F]{6})\\s*");
    private static final int ROW_HEIGHT = 20;
    private static final int NAV_ITEM = 14;

    private final String title;
    private final StynaConfigSpec spec;
    private final Runnable onChange;
    private final List<Section> sections = new ArrayList<>();
    private int selected;
    private double contentScroll;
    private boolean dirty;
    private UiPanel content;

    private static final class Section {
        final List<String> path;
        final List<StynaConfigSpec.ConfigValue<?>> values;

        Section(List<String> path, List<StynaConfigSpec.ConfigValue<?>> values) {
            this.path = path;
            this.values = values;
        }

        String last() {
            return path.get(path.size() - 1);
        }
    }

    public ConfigTab(String title, StynaConfigSpec spec, Runnable onChange) {
        this.title = title;
        this.spec = spec;
        this.onChange = onChange;
    }

    @Override
    public String title() {
        return title;
    }

    private void collect(Map<String, Object> config, List<String> path) {
        List<StynaConfigSpec.ConfigValue<?>> values = new ArrayList<>();
        List<Map.Entry<String, Map<String, Object>>> subsections = new ArrayList<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof StynaConfigSpec.ConfigValue<?> configValue) {
                values.add(configValue);
            } else if (value instanceof Map<?, ?> sub) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) sub;
                subsections.add(new AbstractMap.SimpleEntry<>(entry.getKey(), nested));
            }
        }
        if (!path.isEmpty()) {
            sections.add(new Section(path, values));
        }
        for (Map.Entry<String, Map<String, Object>> sub : subsections) {
            List<String> subPath = new ArrayList<>(path);
            subPath.add(sub.getKey());
            collect(sub.getValue(), subPath);
        }
    }

    @Override
    public void build(UiElement area) {
        if (sections.isEmpty()) {
            collect(spec.getValues(), Collections.emptyList());
            selected = 0;
            for (int i = 0; i < sections.size(); i++) {
                if (!sections.get(i).values.isEmpty()) {
                    selected = i;
                    break;
                }
            }
        }
        if (!spec.isLoaded()) {
            area.add(new UiWidgets.Label(Text.tr("stynastmc.ui.config_not_loaded"), Ui.ERROR))
                    .bounds(area.x + 10, area.y + 10, area.width - 20, 12);
            return;
        }

        int navWidth = Math.max(110, (int) (area.width * 0.28));
        UiPanel nav = area.add(new UiPanel()).bounds(area.x + 10, area.y + 10, navWidth, area.height - 20);
        nav.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 5);
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            if (section.values.isEmpty()) {
                nav.place(new UiWidgets.Label(label(section.last()), Ui.MUTED).scale(0.85F), 12, 2).x += 2;
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
                int depth = sections.get(j).path.size() - section.path.size();
                String name = "";
                if (depth > 0) {
                    name = "§8" + Text.repeat("  ", depth - 1) + (lastChild(j, end) ? "└ " : "├ ") + "§r";
                }
                name += label(sections.get(j).last());
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
        return section.path.size() > parent.path.size() && section.path.subList(0, parent.path.size()).equals(parent.path);
    }

    /** Whether no later section in the group shares this one's depth, so it draws the closing branch. */
    private boolean lastChild(int index, int end) {
        int depth = sections.get(index).path.size();
        for (int k = index + 1; k < end; k++) {
            int d = sections.get(k).path.size();
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
        protected void renderSelf(int mx, int my, float delta) {
            Ui.fill(x, y, width, height, Ui.QUICK_BACKGROUND);
            Ui.outline(x, y, width, height, Ui.PANEL_OUTLINE);
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
        StringBuilder heading = new StringBuilder();
        for (String part : section.path) {
            if (heading.length() > 0) {
                heading.append(" › ");
            }
            heading.append(label(part));
        }
        content.place(UiWidgets.group(heading.length() == 0 ? title : heading.toString()), 17, Ui.GAP);
        String comment = spec.getLevelComment(section.path);
        if (!Text.isBlank(comment)) {
            UiWidgets.Label note = new UiWidgets.Label(comment, Ui.MUTED);
            note.wrap = true;
            content.place(note, note.wrappedHeight(content.width - 26), 6);
        }
        for (StynaConfigSpec.ConfigValue<?> value : section.values) {
            buildValue(value);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void buildValue(StynaConfigSpec.ConfigValue<?> value) {
        StynaConfigSpec.ValueSpec valueSpec = spec.valueSpec(value.getPath());
        UiWidgets.Row row = content.place(new UiWidgets.Row(label(value.key())), ROW_HEIGHT, Ui.GAP);
        row.tooltip = () -> tooltip(value, valueSpec);
        Object current = value.get();
        int right = row.endX() - 3;

        if (!current.equals(value.getDefault())) {
            UiWidgets.Button reset = row.add(new UiWidgets.Button("⟲", () -> {
                set((StynaConfigSpec.ConfigValue) value, value.getDefault());
                rebuild();
            }));
            reset.bounds(right - 14, row.y + 3, 14, 14);
            reset.tooltip(Text.tr("stynastmc.ui.reset_default"));
            right = reset.x - 3;
        }

        if (current instanceof Boolean bool) {
            row.add(new UiWidgets.Toggle(bool, v -> set((StynaConfigSpec.ConfigValue) value, v))).bounds(right - 26, row.y + 3, 26, 14);
        } else if (current instanceof Enum<?> e) {
            Object[] constants = e.getDeclaringClass().getEnumConstants();
            UiWidgets.Button cycle = row.add(new UiWidgets.Button(() -> value.get().toString(), null));
            cycle.onPressButton = mouse -> {
                int index = ((Enum<?>) value.get()).ordinal() + (mouse == 1 ? constants.length - 1 : 1);
                set((StynaConfigSpec.ConfigValue) value, constants[index % constants.length]);
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
            field.onChange = s -> set((StynaConfigSpec.ConfigValue) value, parse.apply(s.trim()));
            field.bounds(right - 100, row.y + 2, 100, 16);
        } else if (current instanceof String s) {
            UiWidgets.TextField field = row.add(new UiWidgets.TextField(s));
            field.validator = valueSpec::test;
            field.onChange = v -> {
                set((StynaConfigSpec.ConfigValue) value, v);
                field.swatch = swatch(v);
            };
            field.swatch = swatch(s);
            int w = Math.min(160, row.width / 2);
            field.bounds(right - w, row.y + 2, w, 16);
        } else if (current instanceof List<?> list) {
            buildList((StynaConfigSpec.ConfigValue<List<? extends String>>) (StynaConfigSpec.ConfigValue) value, valueSpec, row, right,
                    (List<String>) list);
        } else {
            UiWidgets.Label shown = row.add(new UiWidgets.Label(String.valueOf(current), Ui.MUTED));
            shown.bounds(row.x, row.y, row.width - 8, row.height);
            shown.alignRight = true;
        }
    }

    private void buildList(StynaConfigSpec.ConfigValue<List<? extends String>> value, StynaConfigSpec.ValueSpec valueSpec, UiWidgets.Row row,
            int right, List<String> items) {
        UiWidgets.Button add = row.add(new UiWidgets.Button("+", () -> {
            List<String> next = new ArrayList<>(items);
            next.add(items.isEmpty() ? "" : items.get(items.size() - 1));
            set(value, next);
            rebuild();
        }));
        add.bounds(right - 16, row.y + 2, 16, 16);
        add.tooltip(Text.tr("stynastmc.ui.list_add"));
        UiWidgets.Label count = row.add(new UiWidgets.Label(Text.tr("stynastmc.ui.list_count", items.size()), Ui.MUTED));
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
            UiWidgets.Button remove = line.add(new UiWidgets.Button("✕", () -> {
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

    private <T> void set(StynaConfigSpec.ConfigValue<T> value, T newValue) {
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

    private static Integer swatch(String s) {
        String hex = null;
        if (COLOR.matcher(s.trim()).matches()) {
            hex = s.trim();
        } else {
            Matcher stop = STOP_COLOR.matcher(s);
            if (stop.matches()) {
                hex = stop.group(1);
            }
        }
        return hex == null ? null : Integer.parseUnsignedInt(hex.startsWith("#") ? hex.substring(1) : hex, 16);
    }

    private static String tooltip(StynaConfigSpec.ConfigValue<?> value, StynaConfigSpec.ValueSpec valueSpec) {
        StringBuilder text = new StringBuilder("§l").append(label(value.key())).append("§r");
        String comment = valueSpec.getComment();
        if (!Text.isBlank(comment)) {
            text.append("\n").append(comment);
        }
        StynaConfigSpec.Range<?> range = valueSpec.getRange();
        if (range != null) {
            text.append("\n§a").append(Text.tr("stynastmc.ui.range", range.getMin(), range.getMax()));
        }
        text.append("\n§7").append(Text.tr("stynastmc.ui.default", String.valueOf(value.getDefault())));
        return text.toString();
    }

    /** Uses the shared stynastmc.configuration.<key> translations, falling back to Title Case. */
    public static String label(String key) {
        String translationKey = StynaSTMC.MODID + ".configuration." + key;
        if (Text.has(translationKey)) {
            return Text.tr(translationKey);
        }
        StringBuilder out = new StringBuilder();
        for (String word : key.split("_")) {
            if (!word.isEmpty()) {
                out.append(out.length() == 0 ? "" : " ").append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
            }
        }
        return out.toString();
    }
}
