package net.styna.stynastmc.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.IProgressMeter;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketClientStatus;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.stats.StatisticsManager;
import net.styna.stynastmc.config.tabs.Tab;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiPanel;
import net.styna.stynastmc.ui.UiTabScreen;
import net.styna.stynastmc.ui.UiWidgets;
import net.styna.stynastmc.util.Text;

/** Statistics in the Better Statistics Screen style: searchable general stats, an item grid and a mob list. */
public class StatsViewScreen extends UiTabScreen implements IProgressMeter {
    private static final int CELL = 20;
    private static int lastTab;

    private final StatisticsManager stats;
    private boolean requested;
    private boolean loading = true;

    public StatsViewScreen(GuiScreen parent) {
        super(Text.tr("gui.stats"), parent);
        stats = Minecraft.getMinecraft().player.getStatFileWriter();
        setTabs(Arrays.asList(new GeneralTab(), new ItemsTab(), new MobsTab()), lastTab);
    }

    @Override
    public void initGui() {
        super.initGui();
        if (!requested) {
            requested = true;
            mc.getConnection().sendPacket(new CPacketClientStatus(CPacketClientStatus.State.REQUEST_STATS));
        }
    }

    @Override
    public void onStatsUpdated() {
        if (loading) {
            loading = false;
            rebuild();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return !loading;
    }

    @Override
    protected void onTabChanged(int index) {
        lastTab = index;
    }

    private static boolean matches(String name, String query) {
        return Text.isBlank(query) || name.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));
    }

    private static String compact(int value) {
        if (value < 1000) {
            return String.valueOf(value);
        }
        if (value < 1_000_000) {
            return String.format(Locale.ROOT, value < 10_000 ? "%.1fk" : "%.0fk", value / 1000.0);
        }
        return String.format(Locale.ROOT, "%.1fm", value / 1_000_000.0);
    }

    private static String mobName(EntityList.EntityEggInfo info) {
        return Text.tr("entity." + EntityList.getTranslationName(info.spawnedID) + ".name");
    }

    private int read(StatBase stat) {
        return stat == null ? 0 : stats.readStat(stat);
    }

    private abstract class SearchTab implements Tab {
        protected String query = "";
        protected UiPanel panel;

        @Override
        public void build(UiElement area) {
            if (loading) {
                area.add(new UiWidgets.Label(Text.tr("multiplayer.downloadingStats"), Ui.MUTED))
                        .bounds(area.x + 10, area.y + 10, area.width - 20, 20);
                return;
            }
            int barRight = area.endX() - 10 - toolbar(area);
            UiWidgets.TextField search = area.add(new UiWidgets.TextField(query));
            search.bounds(area.x + 10, area.y + 8, barRight - area.x - 10, 18);
            search.placeholder = Text.tr("stynastmc.stats.search");
            search.onChange = text -> {
                query = text;
                refill();
            };
            panel = area.add(new UiPanel()).bounds(area.x + 10, area.y + 32, area.width - 20, area.height - 42);
            panel.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 6);
            fill();
        }

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
        public String title() {
            return Text.tr("stat.generalButton");
        }

        @Override
        protected void fill() {
            List<StatBase> list = new ArrayList<>(StatList.BASIC_STATS);
            list.sort(Comparator.comparing(stat -> stat.getStatName().getUnformattedText()));
            for (StatBase stat : list) {
                String name = stat.getStatName().getUnformattedText();
                if (matches(name, query)) {
                    panel.place(new UiWidgets.InfoRow(name, () -> stat.format(stats.readStat(stat))), 16, 1);
                }
            }
        }
    }

    /** A per item statistic column, with its label for sorting and tooltips. */
    private static final class ItemStat {
        final String label;
        final Function<Item, StatBase> stat;

        ItemStat(String label, Function<Item, StatBase> stat) {
            this.label = label;
            this.stat = stat;
        }
    }

    private static final List<ItemStat> ITEM_STATS = Arrays.asList(
            new ItemStat("stat.mined", item -> {
                Block block = Block.getBlockFromItem(item);
                return block == Blocks.AIR ? null : StatList.getBlockStats(block);
            }),
            new ItemStat("stat.depleted", StatList::getObjectBreakStats),
            new ItemStat("stat.crafted", StatList::getCraftStats),
            new ItemStat("stat.used", StatList::getObjectUseStats),
            new ItemStat("stat.pickup", StatList::getObjectsPickedUpStats),
            new ItemStat("stat.dropped", StatList::getDroppedObjectStats));

    private class ItemsTab extends SearchTab {
        // null sorts by name, anything else by that stat descending.
        private ItemStat sort;

        @Override
        public String title() {
            return Text.tr("stat.itemsButton");
        }

        @Override
        protected int toolbar(UiElement area) {
            int width = 130;
            UiWidgets.Button button = area.add(new UiWidgets.Button(() -> Text.tr("stynastmc.stats.sort",
                    sort == null ? Text.tr("stynastmc.stats.sort.name") : Text.tr(sort.label)), () -> {
                        List<ItemStat> order = new ArrayList<>();
                        order.add(null);
                        order.addAll(ITEM_STATS);
                        sort = order.get((order.indexOf(sort) + 1) % order.size());
                        refill();
                    }));
            button.bounds(area.endX() - 10 - width, area.y + 8, width, 18);
            return width + 4;
        }

        private int value(ItemStat type, Item item) {
            return read(type.stat.apply(item));
        }

        private boolean hasAny(Item item) {
            for (ItemStat type : ITEM_STATS) {
                if (value(type, item) > 0) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void fill() {
            List<Item> items = new ArrayList<>();
            for (Item item : Item.REGISTRY) {
                if (hasAny(item) && matches(new ItemStack(item).getDisplayName(), query)) {
                    items.add(item);
                }
            }
            Comparator<Item> byName = Comparator.comparing(item -> new ItemStack(item).getDisplayName());
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

            private String describe() {
                StringBuilder text = new StringBuilder("§e").append(stack.getDisplayName());
                for (ItemStat type : ITEM_STATS) {
                    int value = value(type, item);
                    if (value > 0) {
                        text.append("\n§7").append(Text.tr(type.label)).append(": §f").append(value);
                    }
                }
                return text.toString();
            }

            @Override
            protected void renderSelf(int mx, int my, float delta) {
                Ui.fill(x, y, width, height, contains(mx, my) ? Ui.HOVER_FILL : Ui.CARD_BACKGROUND);
                Ui.outline(x, y, width, height, Ui.ROW_OUTLINE);
                if (sort != null) {
                    Ui.itemWithCount(stack, x + 2, y + 2, compact(value(sort, item)));
                } else {
                    Ui.item(stack, x + 2, y + 2);
                }
            }
        }
    }

    private class MobsTab extends SearchTab {
        @Override
        public String title() {
            return Text.tr("stat.mobsButton");
        }

        @Override
        protected void fill() {
            List<EntityList.EntityEggInfo> mobs = new ArrayList<>();
            for (EntityList.EntityEggInfo info : EntityList.ENTITY_EGGS.values()) {
                if ((read(info.killEntityStat) > 0 || read(info.entityKilledByStat) > 0) && matches(mobName(info), query)) {
                    mobs.add(info);
                }
            }
            mobs.sort(Comparator.comparing(StatsViewScreen::mobName));
            for (EntityList.EntityEggInfo info : mobs) {
                panel.place(new MobRow(info), 20, 1);
            }
        }

        private class MobRow extends UiWidgets.Row {
            private final EntityList.EntityEggInfo info;
            private final ItemStack egg;

            MobRow(EntityList.EntityEggInfo info) {
                super(mobName(info));
                this.info = info;
                this.egg = new ItemStack(Items.SPAWN_EGG);
                ItemMonsterPlacer.applyEntityIdToItemStack(egg, info.spawnedID);
            }

            @Override
            protected void renderSelf(int mx, int my, float delta) {
                Ui.fill(x, y, width, height, Ui.PANEL_BACKGROUND);
                Ui.item(egg, x + 2, y + 2);
                Ui.textInBox(label, x + 22, y, width - 27, height, Ui.WHITE, false);
                String value = Text.tr("stynastmc.stats.kills", read(info.killEntityStat), read(info.entityKilledByStat));
                Ui.textInBox(value, x + 5, y, width - 10, height, Ui.MUTED, true);
            }
        }
    }
}
