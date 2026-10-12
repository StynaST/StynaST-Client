package net.styna.stynastmc.config.tabs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.styna.stynastmc.skins.OfflineSkins;
import net.styna.stynastmc.skins.SkinsConfig;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiPanel;
import net.styna.stynastmc.ui.UiWidgets;
import net.styna.stynastmc.util.Text;

/** Picks which players on offline-mode servers get their official Mojang skin. */
public class OfflinePlayersTab implements Tab {
    private static final int ROW_HEIGHT = 18;

    private String draft = "";
    private boolean dirty;
    private double listScroll;
    private String lastPlayers = "";
    private UiPanel nav;
    private UiPanel list;

    @Override
    public String title() {
        return Text.tr("stynastmc.ui.offline");
    }

    @Override
    public void build(UiElement area) {
        int navWidth = Math.max(130, (int) (area.width * 0.3));
        nav = area.add(new UiPanel()).bounds(area.x + 10, area.y + 10, navWidth, area.height - 20);
        nav.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 5);
        fillNav();

        int left = nav.endX() + 10;
        int right = area.endX() - 10;
        UiWidgets.Button add = area.add(new UiWidgets.Button(Text.tr("stynastmc.ui.offline.add"), this::addDraft));
        add.bounds(right - 50, area.y + 10, 50, 16);
        UiWidgets.TextField name = area.add(new UiWidgets.TextField(draft));
        name.placeholder = Text.tr("stynastmc.ui.offline.add.placeholder");
        name.validator = s -> s.isEmpty() || s.matches("[A-Za-z0-9_]{1,16}");
        name.onChange = s -> draft = s;
        name.bounds(left, area.y + 10, add.x - Ui.GAP - left, 16);

        list = area.add(new UiPanel()).bounds(left, name.endY() + Ui.GAP, right - left, area.endY() - name.endY() - Ui.GAP - 10);
        list.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 5);
        fillList();
    }

    private void fillNav() {
        nav.place(UiWidgets.group(Text.tr("stynastmc.ui.offline")), 14, Ui.GAP);
        nav.place(new UiWidgets.InfoRow(Text.tr("stynastmc.ui.offline.server"), OfflinePlayersTab::serverState), ROW_HEIGHT, Ui.GAP);
        toggle(Text.tr("stynastmc.configuration.enabled"), SkinsConfig.OFFLINE_ENABLED.get(), v -> {
            SkinsConfig.OFFLINE_ENABLED.set(v);
            dirty = true;
        });
        toggle(Text.tr("stynastmc.ui.offline.all"), SkinsConfig.OFFLINE_ALL.get(), v -> {
            SkinsConfig.OFFLINE_ALL.set(v);
            dirty = true;
        });
        UiWidgets.Label hint = new UiWidgets.Label(Text.tr("stynastmc.ui.offline.hint"), Ui.MUTED);
        hint.wrap = true;
        nav.reserve(4, 0);
        nav.place(hint, hint.wrappedHeight(nav.width - 16), 0);
    }

    private void toggle(String label, boolean value, Consumer<Boolean> onChange) {
        UiWidgets.Row row = nav.place(new UiWidgets.Row(label), ROW_HEIGHT, Ui.GAP);
        row.add(new UiWidgets.Toggle(value, onChange)).bounds(row.endX() - 29, row.y + 2, 26, 14);
    }

    private static String serverState() {
        if (Minecraft.getMinecraft().getConnection() == null) {
            return "§7" + Text.tr("stynastmc.ui.offline.server.none");
        }
        return OfflineSkins.serverIsOffline() ? "§a" + Text.tr("stynastmc.ui.offline.server.offline")
                : "§7" + Text.tr("stynastmc.ui.offline.server.online");
    }

    /** Players in the tab list, keyed by lowercase name. */
    private static Map<String, NetworkPlayerInfo> onlinePlayers() {
        Map<String, NetworkPlayerInfo> players = new TreeMap<>();
        if (Minecraft.getMinecraft().getConnection() != null) {
            for (NetworkPlayerInfo info : Minecraft.getMinecraft().getConnection().getPlayerInfoMap()) {
                players.put(info.getGameProfile().getName().toLowerCase(Locale.ROOT), info);
            }
        }
        return players;
    }

    private void fillList() {
        list.clearChildren();
        Map<String, NetworkPlayerInfo> online = onlinePlayers();
        lastPlayers = String.join(",", online.keySet());
        Map<String, String> names = new TreeMap<>();
        online.forEach((key, info) -> names.put(key, info.getGameProfile().getName()));
        SkinsConfig.OFFLINE_PLAYERS.get().forEach(n -> names.putIfAbsent(n.toLowerCase(Locale.ROOT), n));
        if (names.isEmpty()) {
            list.place(new UiWidgets.Label(Text.tr("stynastmc.ui.offline.empty"), Ui.MUTED), 14, 0);
            return;
        }
        names.forEach((key, name) -> playerRow(name, online.get(key)));
        list.setScroll(listScroll);
    }

    private void playerRow(String name, NetworkPlayerInfo info) {
        boolean premium = info != null && !OfflineSkins.isOfflineProfile(info.getGameProfile());
        StatusRow row = list.place(new StatusRow(info == null ? "§7" + name : name, () -> state(name, premium)), ROW_HEIGHT, 2);
        UiWidgets.Toggle use = row.add(new UiWidgets.Toggle(OfflineSkins.isSelected(name), on -> {
            List<String> next = new ArrayList<>(SkinsConfig.OFFLINE_PLAYERS.get());
            next.removeIf(n -> n.equalsIgnoreCase(name));
            if (on) {
                next.add(name);
            }
            SkinsConfig.OFFLINE_PLAYERS.set(next);
            dirty = true;
            listScroll = list.scroll();
            fillList();
        }));
        use.bounds(row.endX() - 29, row.y + 2, 26, 14);
        use.tooltip(Text.tr("stynastmc.ui.offline.use"));
        row.tooltip(info == null ? Text.tr("stynastmc.ui.offline.not_here") : info.getGameProfile().getId().toString());
    }

    private static String state(String name, boolean premium) {
        if (premium) {
            return "§7" + Text.tr("stynastmc.ui.offline.state.premium");
        }
        OfflineSkins.Status status = OfflineSkins.status(name);
        if (status == null) {
            return "";
        }
        String color = switch (status) {
            case LOADED -> "§a";
            case PENDING -> "§e";
            case NO_SKIN, NOT_FOUND -> "§7";
            case FAILED -> "§c";
        };
        return color + Text.tr("stynastmc.ui.offline.state." + status.name().toLowerCase(Locale.ROOT));
    }

    private void addDraft() {
        String name = draft.trim();
        if (!name.matches("[A-Za-z0-9_]{1,16}") || OfflineSkins.isSelected(name)) {
            return;
        }
        List<String> next = new ArrayList<>(SkinsConfig.OFFLINE_PLAYERS.get());
        next.add(name);
        SkinsConfig.OFFLINE_PLAYERS.set(next);
        dirty = true;
        draft = "";
        if (nav != null) {
            listScroll = list.scroll();
            nav.screen().rebuild();
        }
    }

    @Override
    public void tick() {
        if (list != null && !String.join(",", onlinePlayers().keySet()).equals(lastPlayers)) {
            listScroll = list.scroll();
            fillList();
        }
    }

    @Override
    public void onLeave() {
        if (dirty) {
            SkinsConfig.save();
            dirty = false;
        }
        if (list != null) {
            listScroll = list.scroll();
        }
    }

    /** Name on the left, live lookup state on the right, leaving room for the toggle. */
    private static final class StatusRow extends UiWidgets.Row {
        private final Supplier<String> value;

        StatusRow(String label, Supplier<String> value) {
            super(label);
            this.value = value;
        }

        @Override
        protected void renderSelf(int mx, int my, float delta) {
            super.renderSelf(mx, my, delta);
            Ui.textInBox(value.get(), x + 5, y, width - 42, height, Ui.WHITE, true);
        }
    }
}
