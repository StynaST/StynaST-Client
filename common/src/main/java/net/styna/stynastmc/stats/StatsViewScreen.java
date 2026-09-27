package net.styna.stynastmc.stats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.styna.stynastmc.config.tabs.Tab;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiPanel;
import net.styna.stynastmc.ui.UiTabScreen;
import net.styna.stynastmc.ui.UiWidgets;

/** Statistics in the Better Statistics Screen style: searchable general stats, an item grid and a mob list. */
public class StatsViewScreen extends UiTabScreen {
    private static final int CELL = 20;
    private static int lastTab;

    private final StatsCounter stats;
    private boolean requested;
    private boolean loading = true;

    public StatsViewScreen(Screen parent) {
        super(Component.translatable("gui.stats"), parent);
        stats = Minecraft.getInstance().player.getStats();
        setTabs(List.of(new GeneralTab(), new ItemsTab(), new MobsTab()), lastTab);
    }

    @Override
    protected void init() {
        super.init();
        if (!requested) {
            requested = true;
            minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
        }
    }

    public void onStatsUpdated() {
        if (loading) {
            loading = false;
            rebuild();
        }
    }

    @Override
    protected void onTabChanged(int index) {
        lastTab = index;
    }

    private static boolean matches(Component name, String query) {
        return query.isBlank() || name.getString().toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));
    }

    /** Short count for item slot overlays. */
    private static String compact(int value) {
        if (value < 1000) {
            return String.valueOf(value);
        }
        if (value < 1_000_000) {
            return String.format(Locale.ROOT, value < 10_000 ? "%.1fk" : "%.0fk", value / 1000.0);
        }
        return String.format(Locale.ROOT, "%.1fm", value / 1_000_000.0);
    }

    /** Search bar over a scrolling panel that refills whenever the query changes. */
    private abstract class SearchTab implements Tab {
        protected String query = "";
        protected UiPanel panel;

        @Override
        public void build(UiElement area) {
            if (loading) {
                area.add(new UiWidgets.Label(Component.translatable("multiplayer.downloadingStats"), Ui.MUTED))
                        .bounds(area.x + 10, area.y + 10, area.width - 20, 20);
                return;
            }
            int barRight = area.endX() - 10 - toolbar(area);
            UiWidgets.TextField search = area.add(new UiWidgets.TextField(query));
            search.bounds(area.x + 10, area.y + 8, barRight - area.x - 10, 18);
            search.placeholder = Component.translatable("stynastmc.stats.search");
            search.onChange = text -> {
                query = text;
                refill();
            };
            panel = area.add(new UiPanel()).bounds(area.x + 10, area.y + 32, area.width - 20, area.height - 42);
            panel.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 6);
            fill();
        }

        /** Adds widgets right of the search bar, returning the width used. */
        protected int toolbar(UiElement area) {
            return 0;
        }

        protected void refill() {
            if (panel != null) {
                panel.clearChildren();
                fill();
            }
        }

        protected abstract void fill();
    }

    private class GeneralTab extends SearchTab {
        @Override
        public Component title() {
            return Component.translatable("stat.generalButton");
        }

        @Override
        protected void fill() {
            List<Stat<Identifier>> list = new ArrayList<>();
            Stats.CUSTOM.iterator().forEachRemaining(list::add);
            list.sort(Comparator.comparing(stat -> name(stat).getString()));
            for (Stat<Identifier> stat : list) {
                Component name = name(stat);
                if (matches(name, query)) {
                    panel.place(new UiWidgets.InfoRow(name, () -> Component.literal(stat.format(stats.getValue(stat)))), 16, 1);
                }
            }
        }

        private static Component name(Stat<Identifier> stat) {
            return Component.translatable("stat." + stat.getValue().toString().replace(':', '.'));
        }
    }

    private class ItemsTab extends SearchTab {
        private static final List<StatType<Item>> ITEM_TYPES = List.of(Stats.ITEM_BROKEN, Stats.ITEM_CRAFTED, Stats.ITEM_USED,
                Stats.ITEM_PICKED_UP, Stats.ITEM_DROPPED);
        // null sorts by name, anything else by that stat descending.
        private StatType<?> sort;

        @Override
        public Component title() {
            return Component.translatable("stat.itemsButton");
        }

        @Override
        protected int toolbar(UiElement area) {
            int width = 130;
            UiWidgets.Button button = area.add(new UiWidgets.Button(() -> Component.translatable("stynastmc.stats.sort",
                    sort == null ? Component.translatable("stynastmc.stats.sort.name") : sort.getDisplayName()), () -> {
                        List<StatType<?>> order = new ArrayList<>();
                        order.add(null);
                        order.add(Stats.BLOCK_MINED);
                        order.addAll(ITEM_TYPES);
                        sort = order.get((order.indexOf(sort) + 1) % order.size());
                        refill();
                    }));
            button.bounds(area.endX() - 10 - width, area.y + 8, width, 18);
            return width + 4;
        }

        private int value(StatType<?> type, Item item) {
            if (type == Stats.BLOCK_MINED) {
                Block block = item instanceof BlockItem blockItem ? blockItem.getBlock() : null;
                return block != null && Stats.BLOCK_MINED.contains(block) ? stats.getValue(Stats.BLOCK_MINED.get(block)) : 0;
            }
            @SuppressWarnings("unchecked")
            StatType<Item> itemType = (StatType<Item>) type;
            return itemType.contains(item) ? stats.getValue(itemType.get(item)) : 0;
        }

        private List<StatType<?>> types() {
            List<StatType<?>> types = new ArrayList<>();
            types.add(Stats.BLOCK_MINED);
            types.addAll(ITEM_TYPES);
            return types;
        }

        @Override
        protected void fill() {
            List<Item> items = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (types().stream().anyMatch(type -> value(type, item) > 0) && matches(item.getDefaultInstance().getHoverName(), query)) {
                    items.add(item);
                }
            }
            Comparator<Item> byName = Comparator.comparing(item -> item.getDefaultInstance().getHoverName().getString());
            items.sort(sort == null ? byName : Comparator.<Item>comparingInt(item -> -value(sort, item)).thenComparing(byName));

            int columns = Math.max(1, panel.innerWidth() / CELL);
            for (int i = 0; i < items.size(); i += columns) {
                int top = panel.reserve(CELL, 0);
                for (int c = 0; c < columns && i + c < items.size(); c++) {
                    panel.add(new ItemCell(items.get(i + c))).bounds(panel.innerX() + c * CELL, top, CELL, CELL);
                }
            }
        }

        private class ItemCell extends UiElement {
            private final Item item;
            private final ItemStack stack;

            ItemCell(Item item) {
                this.item = item;
                this.stack = new ItemStack(item);
                tooltip = this::describe;
            }

            private Component describe() {
                MutableComponent text = Component.empty().append(stack.getHoverName()).withColor(0xFFFF55);
                for (StatType<?> type : types()) {
                    int value = value(type, item);
                    if (value > 0) {
                        text.append(Component.literal("\n").append(type.getDisplayName()).append(": ").withColor(0xAAAAAA))
                                .append(Component.literal(String.valueOf(value)).withColor(0xFFFFFF));
                    }
                }
                return text;
            }

            @Override
            protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
                Ui.fill(g, x, y, width, height, contains(mx, my) ? Ui.HOVER_FILL : Ui.CARD_BACKGROUND);
                Ui.outline(g, x, y, width, height, Ui.ROW_OUTLINE);
                g.fakeItem(stack, x + 2, y + 2);
                if (sort != null) {
                    g.itemDecorations(Ui.font(), stack, x + 2, y + 2, compact(value(sort, item)));
                }
            }
        }
    }

    private class MobsTab extends SearchTab {
        @Override
        public Component title() {
            return Component.translatable("stat.mobsButton");
        }

        @Override
        protected void fill() {
            List<EntityType<?>> types = new ArrayList<>();
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                if ((stats.getValue(Stats.ENTITY_KILLED.get(type)) > 0 || stats.getValue(Stats.ENTITY_KILLED_BY.get(type)) > 0)
                        && matches(type.getDescription(), query)) {
                    types.add(type);
                }
            }
            types.sort(Comparator.comparing(type -> type.getDescription().getString()));
            for (EntityType<?> type : types) {
                panel.place(new MobRow(type), 20, 1);
            }
        }

        private class MobRow extends UiWidgets.Row {
            private final EntityType<?> type;
            private final ItemStack egg;

            MobRow(EntityType<?> type) {
                super(type.getDescription());
                this.type = type;
                this.egg = SpawnEggItem.byId(type).map(holder -> new ItemStack(holder.value())).orElse(ItemStack.EMPTY);
            }

            @Override
            protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
                Ui.fill(g, x, y, width, height, Ui.PANEL_BACKGROUND);
                if (!egg.isEmpty()) {
                    g.fakeItem(egg, x + 2, y + 2);
                }
                Ui.textInBox(g, label, x + 22, y, width - 27, height, Ui.WHITE, false);
                Component value = Component.translatable("stynastmc.stats.kills",
                        stats.getValue(Stats.ENTITY_KILLED.get(type)), stats.getValue(Stats.ENTITY_KILLED_BY.get(type)));
                Ui.textInBox(g, value, x + 5, y, width - 10, height, Ui.MUTED, true);
            }
        }
    }
}
