package net.styna.stynastmc.advancements;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementWidgetType;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiPanel;
import net.styna.stynastmc.ui.UiScreen;
import net.styna.stynastmc.ui.UiWidgets;
import org.jspecify.annotations.Nullable;

/** Advancement tree on the left, criteria and progress for the picked advancement on the right. */
public class AdvancementsViewScreen extends UiScreen implements ClientAdvancements.Listener {
    private static final int TAB_SIZE = 24;
    private static final int ROW_HEIGHT = 20;
    private static final int DONE = 0xFF55FF55;
    private static final int PARTIAL = 0xFFFFDD55;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT).withZone(ZoneId.systemDefault());
    private static final String[] NAME_KINDS = {"entity", "biome", "item", "block", "effect", "enchantment"};

    private final ClientAdvancements advancements;
    // Vanilla widgets and tabs need an owning screen; this one is never shown.
    private final AdvancementsScreen owner;
    private final Map<AdvancementHolder, Category> categories = new LinkedHashMap<>();
    private @Nullable Category current;
    private @Nullable AdvancementHolder selected;
    private boolean listening;
    private UiPanel detail;
    private double detailScroll;
    private int treeWidth;
    private int treeHeight;

    public AdvancementsViewScreen(ClientAdvancements advancements, @Nullable Screen parent) {
        super(Component.translatable("gui.advancements"), parent);
        this.advancements = advancements;
        owner = new AdvancementsScreen(advancements, parent);
    }

    @Override
    protected void init() {
        if (!listening) {
            listening = true;
            advancements.setListener(this);
        }
        super.init();
    }

    @Override
    protected void build() {
        int margin = Math.max(8, (int) (width * 0.03));
        int detailWidth = Mth.clamp((int) (width * 0.32), 150, 260);
        int top = 6;

        int done = 0;
        int total = 0;
        for (Category category : categories.values()) {
            done += category.done;
            total += category.total;
        }
        Component heading = title.copy().append(Component.literal("  " + done + "/" + total).withColor(Ui.MUTED & 0xFFFFFF));
        root.add(new UiWidgets.Label(heading, Ui.WHITE)).bounds(margin, top, width - margin * 2, 12);

        int tx = margin;
        int tabY = top + 14;
        for (Category category : categories.values()) {
            root.add(new CategoryTab(category)).bounds(tx, tabY, TAB_SIZE, TAB_SIZE);
            tx += TAB_SIZE + 2;
        }

        int bodyY = tabY + TAB_SIZE + 4;
        int bodyH = height - bodyY - 30;
        treeWidth = width - margin * 2 - detailWidth - Ui.GAP * 2;
        treeHeight = bodyH;
        if (current == null) {
            UiWidgets.Label empty = root.add(new UiWidgets.Label(Component.translatable("advancements.empty"), Ui.MUTED));
            empty.bounds(margin, bodyY + bodyH / 2 - 6, treeWidth, 12);
        } else {
            root.add(new TreeView(current)).bounds(margin, bodyY, treeWidth, bodyH);
        }

        detail = root.add(new UiPanel()).bounds(width - margin - detailWidth, bodyY, detailWidth, bodyH);
        detail.style(Ui.PANEL_BACKGROUND, Ui.PANEL_OUTLINE, 6);
        fillDetail();

        UiWidgets.Button close = root.add(new UiWidgets.Button(CommonComponents.GUI_DONE, this::onClose));
        close.bounds(width / 2 - 100, height - 25, 200, 20);
    }

    private void fillDetail() {
        if (current == null) {
            return;
        }
        Widget widget = selected == null ? null : current.widgets.get(selected);
        if (widget == null || !widget.visible()) {
            fillOverview(current);
        } else {
            fillAdvancement(widget);
        }
        detail.setScroll(detailScroll);
    }

    private void fillOverview(Category category) {
        detail.place(new Header(category.icon, null, category.title, null), 22, Ui.GAP);
        detail.place(new ProgressBar(category.done, category.total, null), 12, Ui.GAP * 2);
        detail.place(UiWidgets.group(Component.translatable("stynastmc.advancements.list")), 12, Ui.GAP);
        for (Widget widget : category.widgets.values()) {
            if (widget.visible()) {
                detail.place(new AdvancementRow(widget), ROW_HEIGHT, 1);
            }
        }
    }

    private void fillAdvancement(Widget widget) {
        DisplayInfo display = widget.display();
        UiWidgets.Button back = detail.place(new UiWidgets.Button(Component.literal("< ").append(current.title), () -> select(null)), 14, Ui.GAP);
        back.transparent();
        back.alignLeft = true;

        Component kind = display.type().getDisplayName().copy().withStyle(display.type().getChatColor());
        detail.place(new Header(widget.icon, widget, display.title(), kind), 22, Ui.GAP);

        UiWidgets.Label description = new UiWidgets.Label(display.description(), Ui.MUTED);
        description.wrap = true;
        detail.place(description, description.wrappedHeight(detail.innerWidth()), Ui.GAP * 2);

        AdvancementProgress progress = widget.progress();
        List<String> missing = new ArrayList<>();
        List<String> obtained = new ArrayList<>();
        if (progress != null) {
            progress.getRemainingCriteria().forEach(missing::add);
            progress.getCompletedCriteria().forEach(obtained::add);
        }
        int groups = widget.node.advancement().requirements().size();
        int groupsDone = progress == null ? 0 : Math.round(progress.getPercent() * groups);
        detail.place(new ProgressBar(groupsDone, groups, progress != null && progress.isDone() ? completedAt(progress, obtained) : null), 12, Ui.GAP * 2);

        if (missing.size() + obtained.size() <= 1 && groups <= 1) {
            return;
        }
        Comparator<String> byName = Comparator.comparing(id -> criterionName(id).getString().toLowerCase(Locale.ROOT));
        missing.sort(byName);
        obtained.sort(byName);
        if (!missing.isEmpty()) {
            detail.place(UiWidgets.group(Component.translatable("stynastmc.advancements.missing", missing.size())), 12, Ui.GAP);
            for (String id : missing) {
                detail.place(new CriterionRow(id, false), 11, 0);
            }
            detail.reserve(0, Ui.GAP * 2);
        }
        if (!obtained.isEmpty()) {
            detail.place(UiWidgets.group(Component.translatable("stynastmc.advancements.obtained", obtained.size())), 12, Ui.GAP);
            for (String id : obtained) {
                detail.place(new CriterionRow(id, true), 11, 0);
            }
        }
    }

    private static @Nullable Component completedAt(AdvancementProgress progress, List<String> obtained) {
        Instant latest = null;
        for (String id : obtained) {
            CriterionProgress criterion = progress.getCriterion(id);
            Instant at = criterion == null ? null : criterion.getObtained();
            if (at != null && (latest == null || at.isAfter(latest))) {
                latest = at;
            }
        }
        return latest == null ? null : Component.translatable("stynastmc.advancements.completed", DATE.format(latest));
    }

    /** Resolves ids like "minecraft:zombie" to an entity, biome or item name, else a tidied id. */
    static Component criterionName(String id) {
        int colon = id.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : id.substring(0, colon);
        String path = colon < 0 ? id : id.substring(colon + 1);
        Language language = Language.getInstance();
        for (String kind : NAME_KINDS) {
            String key = kind + "." + namespace + "." + path.replace('/', '.');
            if (language.has(key)) {
                return Component.translatable(key);
            }
        }
        String last = path.substring(path.lastIndexOf('/') + 1).replace(".png", "");
        StringBuilder out = new StringBuilder();
        for (String word : last.split("_")) {
            if (!word.isEmpty()) {
                out.append(out.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return Component.literal(out.toString());
    }

    private void select(@Nullable AdvancementHolder holder) {
        selected = holder;
        detailScroll = 0;
        rebuild();
    }

    @Override
    public void rebuild() {
        if (detail != null) {
            detailScroll = detail.scroll();
        }
        super.rebuild();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minecraft.options.keyAdvancements.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void tick() {
        super.tick();
        for (Category category : categories.values()) {
            category.fade = Math.max(0, category.fade - 1);
        }
    }

    @Override
    public void removed() {
        advancements.setListener(null);
        listening = false;
        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null) {
            connection.send(ServerboundSeenAdvancementsPacket.closedScreen());
        }
        super.removed();
    }

    @Override
    public void onAdvancementsUpdated() {
        Map<AdvancementHolder, Category> old = new HashMap<>(categories);
        categories.clear();
        AdvancementTree tree = advancements.tree();
        for (AdvancementNode node : tree.roots()) {
            AdvancementTab tab = AdvancementTab.create(minecraft, owner, 0, node);
            if (tab != null) {
                Category category = new Category(tab, node);
                Category previous = old.get(node.holder());
                if (previous != null) {
                    category.copyView(previous);
                }
                categories.put(node.holder(), category);
            }
        }
        for (AdvancementNode node : tree.tasks()) {
            Category category = categories.get(node.root().holder());
            if (category != null) {
                category.tab.addAdvancement(node);
                category.add(node);
            }
        }
        advancements.progress().forEach((holder, progress) -> {
            AdvancementNode node = tree.get(holder);
            Category category = node == null ? null : categories.get(node.root().holder());
            Widget widget = category == null ? null : category.widgets.get(holder);
            if (widget != null) {
                widget.setProgress(progress);
            }
        });
        categories.values().forEach(Category::count);

        current = current == null ? null : categories.get(current.root.holder());
        if (current == null && !categories.isEmpty()) {
            current = categories.values().iterator().next();
            advancements.setSelectedTab(current.root.holder(), true);
        }
        if (detail != null) {
            rebuild();
        }
    }

    @Override
    public void onSelectedTabChanged(@Nullable AdvancementHolder holder) {
        Category next = holder == null ? null : categories.get(holder);
        if (next != null && next != current) {
            current = next;
            selected = null;
            detailScroll = 0;
            if (detail != null) {
                rebuild();
            }
        }
    }

    @Override
    public void onAdvancementsCleared() {
        categories.clear();
        current = null;
        selected = null;
        if (detail != null) {
            rebuild();
        }
    }

    private static final class Widget {
        final AdvancementNode node;
        final AdvancementWidget vanilla;
        final ItemStack icon;
        private @Nullable AdvancementProgress progress;

        Widget(AdvancementNode node, AdvancementWidget vanilla) {
            this.node = node;
            this.vanilla = vanilla;
            icon = vanilla.getDisplay().icon().create();
        }

        DisplayInfo display() {
            return vanilla.getDisplay();
        }

        @Nullable AdvancementProgress progress() {
            return progress;
        }

        void setProgress(AdvancementProgress progress) {
            this.progress = progress;
            vanilla.setProgress(progress);
        }

        boolean done() {
            return progress != null && progress.isDone();
        }

        boolean visible() {
            return !display().hidden() || done();
        }

        float percent() {
            return progress == null ? 0 : progress.getPercent();
        }
    }

    private static final class Category {
        final AdvancementTab tab;
        final AdvancementNode root;
        final Component title;
        final ItemStack icon;
        final Identifier background;
        final Map<AdvancementHolder, Widget> widgets = new LinkedHashMap<>();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int done;
        int total;
        double scrollX;
        double scrollY;
        boolean centered;
        // Ticks left on the flash around an advancement picked from the list.
        int fade;

        Category(AdvancementTab tab, AdvancementNode root) {
            this.tab = tab;
            this.root = root;
            DisplayInfo display = root.advancement().display().orElseThrow();
            title = display.title();
            icon = display.icon().create();
            background = display.background().orElseThrow().texturePath();
            add(root);
        }

        void add(AdvancementNode node) {
            AdvancementWidget vanilla = tab.getWidget(node.holder());
            if (vanilla == null) {
                return;
            }
            widgets.put(node.holder(), new Widget(node, vanilla));
            minX = Math.min(minX, vanilla.getX());
            minY = Math.min(minY, vanilla.getY());
            maxX = Math.max(maxX, vanilla.getX() + 28);
            maxY = Math.max(maxY, vanilla.getY() + 27);
        }

        void count() {
            done = 0;
            total = 0;
            for (Widget widget : widgets.values()) {
                if (!widget.display().hidden() || widget.done()) {
                    total++;
                    done += widget.done() ? 1 : 0;
                }
            }
        }

        void copyView(Category other) {
            scrollX = other.scrollX;
            scrollY = other.scrollY;
            centered = other.centered;
        }

        AdvancementWidget rootWidget() {
            return widgets.get(root.holder()).vanilla;
        }
    }

    private class CategoryTab extends UiElement {
        private final Category category;

        CategoryTab(Category category) {
            this.category = category;
            tooltip = () -> category.title.copy().append(Component.literal("  " + category.done + "/" + category.total).withColor(Ui.MUTED & 0xFFFFFF));
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean active = category == current;
            Ui.fill(g, x, y, width, height, active ? Ui.HOVER_FILL : Ui.PANEL_BACKGROUND);
            Ui.outline(g, x, y, width, height, active ? Ui.FOCUS_OUTLINE : contains(mx, my) ? Ui.WHITE : Ui.PANEL_OUTLINE);
            g.fakeItem(category.icon, x + (width - 16) / 2, y + (height - 16) / 2 - 1);
            if (category.total > 0) {
                int bar = (width - 4) * category.done / category.total;
                Ui.fill(g, x + 2, endY() - 3, width - 4, 1, Ui.BLACK);
                Ui.fill(g, x + 2, endY() - 3, bar, 1, category.done == category.total ? DONE : PARTIAL);
            }
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            if (category != current) {
                UiWidgets.playClick();
                advancements.setSelectedTab(category.root.holder(), true);
            }
            return true;
        }
    }

    private class TreeView extends UiElement {
        private static final int PAD = 12;
        private final Category category;
        private double pressX;
        private double pressY;
        private double lastX;
        private double lastY;
        private boolean moved;

        TreeView(Category category) {
            this.category = category;
        }

        private void clampScroll() {
            category.scrollX = clampAxis(category.scrollX, category.minX, category.maxX, width);
            category.scrollY = clampAxis(category.scrollY, category.minY, category.maxY, height);
        }

        private static double clampAxis(double scroll, int min, int max, int view) {
            if (max - min + PAD * 2 <= view) {
                return (view - max - min) / 2.0;
            }
            return Mth.clamp(scroll, view - max - PAD, PAD - min);
        }

        private @Nullable Widget widgetAt(double mx, double my) {
            if (!contains(mx, my)) {
                return null;
            }
            int sx = Mth.floor(category.scrollX);
            int sy = Mth.floor(category.scrollY);
            for (Widget widget : category.widgets.values()) {
                if (widget.vanilla.isMouseOver(sx, sy, (int) mx - x, (int) my - y)) {
                    return widget;
                }
            }
            return null;
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            if (!category.centered) {
                category.scrollX = width / 2.0 - (category.maxX + category.minX) / 2.0;
                category.scrollY = height / 2.0 - (category.maxY + category.minY) / 2.0;
                category.centered = true;
            }
            clampScroll();
            int sx = Mth.floor(category.scrollX);
            int sy = Mth.floor(category.scrollY);

            g.enableScissor(x, y, endX(), endY());
            g.pose().pushMatrix();
            g.pose().translate(x, y);
            int ox = Math.floorMod(sx, 16);
            int oy = Math.floorMod(sy, 16);
            for (int tx = -1; tx <= width / 16 + 1; tx++) {
                for (int ty = -1; ty <= height / 16 + 1; ty++) {
                    g.blit(RenderPipelines.GUI_TEXTURED, category.background, ox + 16 * tx, oy + 16 * ty, 0, 0, 16, 16, 16, 16);
                }
            }
            AdvancementWidget root = category.rootWidget();
            root.extractConnectivity(g, sx, sy, true);
            root.extractConnectivity(g, sx, sy, false);
            root.extractRenderState(g, sx, sy);

            Widget picked = selected == null ? null : category.widgets.get(selected);
            if (picked != null && picked.visible()) {
                boolean flash = category.fade > 0 && category.fade / 3 % 2 == 0;
                Ui.outline(g, sx + picked.vanilla.getX() + 2, sy + picked.vanilla.getY() - 1, 28, 28, flash ? Ui.WHITE : Ui.FOCUS_OUTLINE);
            }
            g.pose().popMatrix();
            g.disableScissor();
            Ui.outline(g, x - 1, y - 1, width + 2, height + 2, Ui.PANEL_OUTLINE);
        }

        @Override
        protected void renderOverlay(GuiGraphicsExtractor g, int mx, int my, float delta) {
            Widget hovered = moved ? null : widgetAt(mx, my);
            if (hovered == null) {
                return;
            }
            g.nextStratum();
            g.pose().pushMatrix();
            g.pose().translate(x, y);
            hovered.vanilla.extractHover(g, Mth.floor(category.scrollX), Mth.floor(category.scrollY), 1.0F, x, y, endX());
            g.pose().popMatrix();
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            pressX = lastX = mx;
            pressY = lastY = my;
            moved = false;
            if (screen() != null) {
                screen().startDrag(this);
            }
            return true;
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button) {
            if (!moved && Math.abs(mx - pressX) + Math.abs(my - pressY) < 3) {
                return true;
            }
            moved = true;
            category.scrollX += mx - lastX;
            category.scrollY += my - lastY;
            lastX = mx;
            lastY = my;
            return true;
        }

        @Override
        public void mouseReleased(double mx, double my, int button) {
            if (!moved) {
                Widget widget = widgetAt(mx, my);
                if (widget != null) {
                    UiWidgets.playClick();
                    select(widget.node.holder());
                } else if (selected != null) {
                    select(null);
                }
            }
            moved = false;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double amount) {
            if (minecraft.hasShiftDown()) {
                category.scrollX += amount * 16;
            } else {
                category.scrollY += amount * 16;
            }
            return true;
        }
    }

    /** Frame and icon, title and a coloured subtitle. */
    private static class Header extends UiElement {
        private final ItemStack icon;
        private final @Nullable Widget widget;
        private final Component title;
        private final @Nullable Component subtitle;

        Header(ItemStack icon, @Nullable Widget widget, Component title, @Nullable Component subtitle) {
            this.icon = icon;
            this.widget = widget;
            this.title = title;
            this.subtitle = subtitle;
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            if (widget != null) {
                AdvancementWidgetType frame = widget.done() ? AdvancementWidgetType.OBTAINED : AdvancementWidgetType.UNOBTAINED;
                g.blitSprite(RenderPipelines.GUI_TEXTURED, frame.frameSprite(widget.display().type()), x - 2, y - 2, 26, 26);
            }
            g.fakeItem(icon, x + 3, y + 3);
            int tx = x + 26;
            int tw = width - 26;
            if (subtitle == null) {
                Ui.textInBox(g, clip(title, tw), tx, y, tw, height, Ui.WHITE, false);
            } else {
                g.text(Ui.font(), clip(title, tw), tx, y + 1, Ui.WHITE, true);
                g.text(Ui.font(), subtitle, tx, y + 12, Ui.WHITE, true);
            }
        }
    }

    private static Component clip(Component text, int width) {
        if (Ui.font().width(text) <= width) {
            return text;
        }
        return Component.literal(Ui.font().plainSubstrByWidth(text.getString(), width - Ui.font().width("...")) + "...");
    }

    private static class ProgressBar extends UiElement {
        private final int done;
        private final int total;
        private final @Nullable Component note;

        ProgressBar(int done, int total, @Nullable Component note) {
            this.done = done;
            this.total = total;
            this.note = note;
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean complete = total > 0 && done >= total;
            Ui.fill(g, x, y, width, height, Ui.BLACK);
            if (total > 0) {
                Ui.fill(g, x + 1, y + 1, (width - 2) * Math.min(done, total) / total, height - 2, complete ? 0xFF2E8B2E : 0xFF8B7A2E);
            }
            Component label = complete && note != null ? note
                    : Component.literal(done + " / " + total + (total > 0 ? "  (" + done * 100 / total + "%)" : ""));
            Ui.centeredText(g, label, x, y, width, height, Ui.WHITE);
        }
    }

    private class AdvancementRow extends UiElement {
        private final Widget widget;

        AdvancementRow(Widget widget) {
            this.widget = widget;
            tooltip = () -> widget.display().description();
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            if (contains(mx, my)) {
                Ui.fill(g, x, y, width, height, Ui.HOVER_FILL);
            }
            g.fakeItem(widget.icon, x + 2, y + (height - 16) / 2);
            float percent = widget.percent();
            int color = widget.done() ? DONE : percent > 0 ? PARTIAL : Ui.WHITE;
            Component status = widget.done() ? Component.literal("✔")
                    : percent > 0 ? widget.progress().getProgressText() : null;
            int statusWidth = status == null ? 0 : Ui.font().width(status) + 4;
            Ui.textInBox(g, clip(widget.display().title(), width - 24 - statusWidth), x + 22, y, width - 24 - statusWidth, height, color, false);
            if (status != null) {
                Ui.textInBox(g, status, x, y, width - 3, height, color, true);
            }
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            UiWidgets.playClick();
            current.fade = 18;
            focus(widget);
            select(widget.node.holder());
            return true;
        }

        private void focus(Widget widget) {
            current.scrollX = treeWidth / 2.0 - widget.vanilla.getX() - 13;
            current.scrollY = treeHeight / 2.0 - widget.vanilla.getY() - 13;
            current.centered = true;
        }
    }

    private static class CriterionRow extends UiElement {
        private final Component text;
        private final int color;

        CriterionRow(String id, boolean done) {
            text = Component.literal((done ? "✔ " : "✘ ") + criterionName(id).getString());
            color = done ? DONE : Ui.ERROR;
            tooltip = () -> Component.literal(id).withColor(Ui.MUTED & 0xFFFFFF);
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            Ui.textInBox(g, clip(text, width - 2), x + 2, y, width - 2, height, color, false);
        }
    }
}
