package net.styna.stynastmc.config.tabs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.styna.stynastmc.muffler.Muffler;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiPanel;
import net.styna.stynastmc.ui.UiWidgets;

public class MufflerTab implements Tab {
    private static final int ROW_HEIGHT = 18;

    private enum Filter {
        RECENT, MUFFLED, ALL
    }

    private String query = "";
    private Filter filter = Filter.RECENT;
    /** -1 is global, otherwise an index into the anchor list. */
    private int scope = -1;
    private boolean dirty;
    private double listScroll;
    private double navScroll;
    private UiPanel nav;
    private UiPanel list;

    @Override
    public Component title() {
        return Component.translatable("stynastmc.ui.muffler");
    }

    private Muffler.Anchor anchor() {
        return scope >= 0 && scope < Muffler.anchors.size() ? Muffler.anchors.get(scope) : null;
    }

    private Map<Identifier, Float> sounds() {
        Muffler.Anchor anchor = anchor();
        return anchor == null ? Muffler.global : anchor.sounds;
    }

    @Override
    public void build(UiElement area) {
        if (anchor() == null) {
            scope = -1;
        }
        int navWidth = Math.max(130, (int) (area.width * 0.28));
        nav = area.add(new UiPanel()).bounds(area.x + 10, area.y + 10, navWidth, area.height - 20);
        nav.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 5);
        fillNav();
        nav.setScroll(navScroll);

        int left = nav.endX() + 10;
        int right = area.endX() - 10;
        UiWidgets.TextField search = area.add(new UiWidgets.TextField(query));
        search.placeholder = Component.translatable("stynastmc.ui.muffler.search");
        search.onChange = s -> {
            query = s;
            listScroll = 0;
            fillList();
        };
        UiWidgets.Button clear = area.add(new UiWidgets.Button(Component.translatable("stynastmc.ui.muffler.clear"), () -> {
            sounds().clear();
            changed();
            rebuild();
        }));
        clear.bounds(right - 60, area.y + 10, 60, 16);
        clear.textColor = Ui.ERROR;
        clear.tooltip(Component.translatable("stynastmc.ui.muffler.clear.tooltip"));
        UiWidgets.Button filterButton = area.add(new UiWidgets.Button(
                () -> Component.translatable("stynastmc.ui.muffler.filter." + filter.name().toLowerCase(Locale.ROOT)), null));
        filterButton.onPressButton = mouse -> {
            Filter[] values = Filter.values();
            filter = values[(filter.ordinal() + (mouse == InputConstants.MOUSE_BUTTON_RIGHT ? values.length - 1 : 1)) % values.length];
            listScroll = 0;
            fillList();
        };
        filterButton.bounds(clear.x - Ui.GAP - 90, area.y + 10, 90, 16);
        search.bounds(left, area.y + 10, filterButton.x - Ui.GAP - left, 16);

        list = area.add(new UiPanel()).bounds(left, search.endY() + Ui.GAP, right - left, area.endY() - search.endY() - Ui.GAP - 10);
        list.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 5);
        fillList();
    }

    private void fillNav() {
        nav.clearChildren();
        nav.place(UiWidgets.group(Component.translatable("stynastmc.ui.muffler")), 14, Ui.GAP);
        toggle(Component.translatable("stynastmc.configuration.enabled"), Muffler.enabled, v -> {
            Muffler.enabled = v;
            changed();
        });
        toggle(Component.translatable("stynastmc.ui.muffler.inventory_button"), Muffler.inventoryButton, v -> {
            Muffler.inventoryButton = v;
            changed();
        });

        nav.place(UiWidgets.group(Component.translatable("stynastmc.ui.muffler.scope")), 14, Ui.GAP);
        scopeButton(-1, Component.translatable("stynastmc.ui.muffler.global"), Muffler.global.size());
        String dim = Muffler.dimension();
        for (int i = 0; i < Muffler.anchors.size(); i++) {
            Muffler.Anchor anchor = Muffler.anchors.get(i);
            UiWidgets.Button button = scopeButton(i, Component.literal(anchor.name), anchor.sounds.size());
            if (!anchor.dimension.equals(dim)) {
                button.textColor = Ui.MUTED;
            }
        }
        LocalPlayer player = Minecraft.getInstance().player;
        UiWidgets.Button add = nav.place(new UiWidgets.Button(Component.translatable("stynastmc.ui.muffler.add_anchor"), () -> {
            Muffler.Anchor anchor = new Muffler.Anchor();
            anchor.name = "Anchor " + (Muffler.anchors.size() + 1);
            anchor.radius = 16;
            moveHere(anchor);
            Muffler.anchors.add(anchor);
            scope = Muffler.anchors.size() - 1;
            changed();
            rebuild();
        }), 16, Ui.GAP);
        add.enabled = player != null;
        if (player == null) {
            add.tooltip(Component.translatable("stynastmc.ui.muffler.need_world"));
        }

        Muffler.Anchor anchor = anchor();
        if (anchor == null) {
            UiWidgets.Label hint = new UiWidgets.Label(Component.translatable("stynastmc.ui.muffler.hint"), Ui.MUTED);
            hint.wrap = true;
            nav.reserve(4, 0);
            nav.place(hint, hint.wrappedHeight(nav.width - 16), 0);
            return;
        }
        nav.place(UiWidgets.group(Component.translatable("stynastmc.ui.muffler.anchor")), 14, Ui.GAP);
        UiWidgets.TextField name = nav.place(new UiWidgets.TextField(anchor.name), 16, Ui.GAP);
        name.validator = s -> !s.isBlank();
        name.onChange = s -> {
            anchor.name = s.trim();
            dirty = true;
        };
        UiWidgets.Row radiusRow = nav.place(new UiWidgets.Row(Component.translatable("stynastmc.ui.muffler.radius")), ROW_HEIGHT, Ui.GAP);
        UiWidgets.TextField radius = radiusRow.add(new UiWidgets.TextField(String.valueOf(anchor.radius)));
        radius.validator = s -> s.matches("\\d{1,3}") && Integer.parseInt(s) >= 1 && Integer.parseInt(s) <= 256;
        radius.onChange = s -> {
            anchor.radius = Integer.parseInt(s);
            changed();
        };
        radius.bounds(radiusRow.endX() - 42, radiusRow.y + 1, 40, 16);
        radiusRow.tooltip(Component.translatable("stynastmc.ui.range", 1, 256));
        nav.place(new UiWidgets.InfoRow(Component.translatable("stynastmc.ui.muffler.position"),
                () -> Component.literal((int) anchor.x + ", " + (int) anchor.y + ", " + (int) anchor.z)), ROW_HEIGHT, Ui.GAP)
                .tooltip(Component.literal(anchor.dimension));

        UiElement actions = nav.place(new UiElement(), 16, Ui.GAP);
        int half = (actions.width - Ui.GAP) / 2;
        UiWidgets.Button move = actions.add(new UiWidgets.Button(Component.translatable("stynastmc.ui.muffler.move_here"), () -> {
            moveHere(anchor);
            changed();
            rebuild();
        }));
        move.bounds(actions.x, actions.y, half, 16);
        move.enabled = player != null;
        UiWidgets.Button delete = actions.add(new UiWidgets.Button(Component.translatable("stynastmc.ui.muffler.delete"), () -> {
            Muffler.anchors.remove(anchor);
            scope = -1;
            changed();
            rebuild();
        }));
        delete.bounds(move.endX() + Ui.GAP, actions.y, actions.width - half - Ui.GAP, 16);
        delete.textColor = Ui.ERROR;
    }

    private void toggle(Component label, boolean value, java.util.function.Consumer<Boolean> onChange) {
        UiWidgets.Row row = nav.place(new UiWidgets.Row(label), ROW_HEIGHT, Ui.GAP);
        row.add(new UiWidgets.Toggle(value, onChange)).bounds(row.endX() - 29, row.y + 2, 26, 14);
    }

    private UiWidgets.Button scopeButton(int index, Component name, int count) {
        Component label = Component.empty().append(name).append(Component.literal(" (" + count + ")").withColor(0x777777));
        UiWidgets.Button button = nav.place(new UiWidgets.Button(label, () -> {
            scope = index;
            listScroll = 0;
            rebuild();
        }), 14, 2);
        button.alignLeft = true;
        button.selected = scope == index;
        return button;
    }

    private static void moveHere(Muffler.Anchor anchor) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            anchor.dimension = Muffler.dimension();
            anchor.x = player.getX();
            anchor.y = player.getY();
            anchor.z = player.getZ();
        }
    }

    private List<Identifier> entries() {
        List<Identifier> ids = switch (filter) {
            case RECENT -> new ArrayList<>(Muffler.recent.keySet()).reversed();
            case MUFFLED -> new ArrayList<>(sounds().keySet());
            case ALL -> Minecraft.getInstance().getSoundManager().getAvailableSounds().stream().sorted().toList();
        };
        String q = query.trim().toLowerCase(Locale.ROOT);
        return q.isEmpty() ? ids : ids.stream().filter(id -> id.toString().contains(q)).toList();
    }

    private void fillList() {
        list.clearChildren();
        List<Identifier> ids = entries();
        if (ids.isEmpty()) {
            list.place(new UiWidgets.Label(Component.translatable("stynastmc.ui.muffler.empty." + filter.name().toLowerCase(Locale.ROOT)), Ui.MUTED), 14, 0);
            return;
        }
        for (Identifier id : ids) {
            soundRow(id);
        }
        list.setScroll(listScroll);
    }

    private void soundRow(Identifier id) {
        Map<Identifier, Float> sounds = sounds();
        Float volume = sounds.get(id);
        UiWidgets.Row row = list.place(new UiWidgets.Row(Component.empty()), ROW_HEIGHT, 2);
        int right = row.endX() - 2;

        UiWidgets.Button play = row.add(new UiWidgets.Button(Component.literal("▶"), () -> Muffler.preview(id)));
        play.bounds(right - 14, row.y + 2, 14, 14);
        play.tooltip(Component.translatable("stynastmc.ui.muffler.play"));
        right = play.x - Ui.GAP;

        UiWidgets.Toggle mute = row.add(new UiWidgets.Toggle(volume != null, on -> {
            if (on) {
                sounds.put(id, 0.0F);
            } else {
                sounds.remove(id);
            }
            changed();
            listScroll = list.scroll();
            rebuild();
        }));
        mute.bounds(right - 26, row.y + 2, 26, 14);
        mute.tooltip(Component.translatable("stynastmc.ui.muffler.muffle"));
        right = mute.x - Ui.GAP;

        if (volume != null) {
            UiWidgets.Slider slider = row.add(new UiWidgets.Slider(volume, v -> {
                sounds.put(id, v.floatValue());
                changed();
            }));
            slider.bounds(right - 60, row.y + 2, 60, 14);
            slider.tooltip(Component.translatable("stynastmc.ui.muffler.volume"));
            right = slider.x - Ui.GAP;
        }

        String text = id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) ? id.getPath() : id.toString();
        row.label = Component.literal(Ui.font().plainSubstrByWidth(text, right - row.x - 8));
        SoundSource source = Muffler.recent.get(id);
        Component tip = Component.literal(id.toString());
        if (source != null) {
            tip = tip.copy().append(Component.literal("\n" + source.getName()).withColor(0xAAAAAA));
        }
        row.tooltip(tip);
    }

    private void changed() {
        dirty = true;
        Muffler.refresh();
    }

    private void rebuild() {
        if (nav != null) {
            navScroll = nav.scroll();
            nav.screen().rebuild();
        }
    }

    @Override
    public void onLeave() {
        if (dirty) {
            Muffler.save();
            dirty = false;
        }
        if (list != null) {
            listScroll = list.scroll();
            navScroll = nav.scroll();
        }
    }
}
