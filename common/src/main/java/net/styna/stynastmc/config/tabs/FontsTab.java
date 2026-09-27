package net.styna.stynastmc.config.tabs;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.styna.stynastmc.config.StynaConfigScreen;
import net.styna.stynastmc.fonts.FontRuntime;
import net.styna.stynastmc.fonts.FontSettings;
import net.styna.stynastmc.fonts.FontsConfig;
import net.styna.stynastmc.fonts.GoogleFonts;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiPanel;
import net.styna.stynastmc.ui.UiWidgets;

/** Browse Google Fonts, tune and preview one live, then apply it to the whole game. */
public class FontsTab implements Tab {
    private static final List<String> CATEGORIES = List.of("", "Sans Serif", "Serif", "Display", "Handwriting", "Monospace");
    private static final Identifier BUTTON_SPRITE = Identifier.withDefaultNamespace("widget/button");

    private final StynaConfigScreen screen;
    private List<GoogleFonts.Family> catalog;
    private String catalogError;
    private String query = "";
    private int category;
    private GoogleFonts.Family selected;
    private FontSettings settings;
    private Component status = Component.empty();
    private int statusColor = Ui.MUTED;
    private int previewToken;
    private double listScroll;
    private double detailScroll;
    private UiPanel list;
    private UiPanel detail;

    public FontsTab(StynaConfigScreen screen) {
        this.screen = screen;
    }

    @Override
    public Component title() {
        return Component.translatable("stynastmc.ui.fonts");
    }

    @Override
    public void build(UiElement area) {
        if (settings == null) {
            settings = FontsConfig.SPEC.isLoaded() ? FontsConfig.settings() : new FontSettings("", "400", 10, 4, 0, 0);
        }
        if (catalog == null && catalogError == null) {
            loadCatalog(false);
        }

        int leftWidth = Math.max(140, (int) (area.width * 0.32));
        int left = area.x + 10;
        UiWidgets.TextField search = area.add(new UiWidgets.TextField(query));
        search.bounds(left, area.y + 10, leftWidth, 16);
        search.placeholder = Component.translatable("stynastmc.ui.fonts.search", catalog == null ? "…" : catalog.size());
        search.onChange = s -> {
            query = s;
            listScroll = 0;
            fillList();
        };

        UiWidgets.Button categoryButton = area.add(new UiWidgets.Button(() -> Component.translatable("stynastmc.ui.fonts.category",
                category == 0 ? Component.translatable("stynastmc.ui.fonts.all") : Component.literal(CATEGORIES.get(category))), null));
        categoryButton.onPressButton = mouse -> {
            category = (category + (mouse == InputConstants.MOUSE_BUTTON_RIGHT ? CATEGORIES.size() - 1 : 1)) % CATEGORIES.size();
            listScroll = 0;
            fillList();
        };
        categoryButton.bounds(left, search.endY() + Ui.GAP, leftWidth - 19, 16);
        UiWidgets.Button refresh = area.add(new UiWidgets.Button(Component.literal("⟳"), () -> loadCatalog(true)));
        refresh.bounds(categoryButton.endX() + Ui.GAP, categoryButton.y, 16, 16);
        refresh.tooltip(Component.translatable("stynastmc.ui.fonts.refresh"));

        list = area.add(new UiPanel()).bounds(left, categoryButton.endY() + Ui.GAP, leftWidth, area.endY() - categoryButton.endY() - Ui.GAP - 10);
        list.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 3);
        fillList();

        detail = area.add(new UiPanel()).bounds(list.endX() + 10, area.y + 10, area.endX() - list.endX() - 20, area.height - 20);
        detail.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 10);
        fillDetail();
        detail.setScroll(detailScroll);
    }

    private void loadCatalog(boolean refresh) {
        catalogError = null;
        setStatus(Component.translatable("stynastmc.ui.fonts.loading_catalog"), Ui.MUTED);
        GoogleFonts.catalog(refresh).whenComplete((families, error) -> Minecraft.getInstance().execute(() -> {
            if (error != null) {
                catalogError = rootMessage(error);
                setStatus(Component.translatable("stynastmc.ui.fonts.catalog_failed", catalogError), Ui.ERROR);
            } else {
                catalog = families;
                setStatus(Component.empty(), Ui.MUTED);
                if (selected == null && !settings.family().isBlank()) {
                    families.stream().filter(f -> f.family().equals(settings.family())).findFirst().ifPresent(f -> {
                        selected = f;
                        requestPreview();
                    });
                }
            }
            rebuild();
        }));
    }

    private void fillList() {
        list.clearChildren();
        if (catalog == null) {
            list.place(new UiWidgets.Label(catalogError == null ? Component.translatable("stynastmc.ui.fonts.loading_catalog")
                    : Component.translatable("stynastmc.ui.fonts.catalog_failed", catalogError), catalogError == null ? Ui.MUTED : Ui.ERROR), 14, 0);
            return;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        FontSettings applied = FontRuntime.appliedSettings();
        for (GoogleFonts.Family family : catalog) {
            if ((category != 0 && !family.category().equals(CATEGORIES.get(category)))
                    || (!q.isEmpty() && !family.family().toLowerCase(Locale.ROOT).contains(q))) {
                continue;
            }
            boolean isApplied = applied != null && applied.family().equals(family.family());
            Component name = isApplied ? Component.literal("✔ ").withColor(0x55FF55).append(Component.literal(family.family()).withColor(0xFFFFFF))
                    : Component.literal(family.family());
            UiWidgets.Button row = list.place(new UiWidgets.Button(name, () -> select(family)).transparent(), 14, 0);
            row.selected = family == selected;
            row.tooltip(Component.literal(family.category() + " · " + family.variants().size() + " styles"));
        }
        list.setScroll(listScroll);
    }

    private void select(GoogleFonts.Family family) {
        selected = family;
        settings = settings.withFamily(family.family(), family.defaultVariant());
        detailScroll = 0;
        requestPreview();
        rebuild();
    }

    private void fillDetail() {
        if (selected == null) {
            detail.place(UiWidgets.group(Component.translatable("stynastmc.ui.fonts.pick")), 17, Ui.GAP);
            wrapped(Component.translatable("stynastmc.ui.fonts.pick_hint"), Ui.MUTED);
            detail.place(new UiWidgets.Label(() -> status, Ui.WHITE), 12, Ui.GAP);
            return;
        }

        detail.place(new FontCard(), 40, Ui.GAP * 2);

        detail.place(UiWidgets.group(Component.translatable("stynastmc.ui.fonts.style")), 17, Ui.GAP);
        int rowTop = detail.reserve(16, Ui.GAP);
        int bx = detail.innerX();
        int limit = detail.innerX() + detail.width - 26;
        for (String variant : selected.variants()) {
            Component name = Component.literal(GoogleFonts.variantName(variant));
            int w = Ui.font().width(name) + 10;
            if (bx + w > limit) {
                bx = detail.innerX();
                rowTop = detail.reserve(16, Ui.GAP);
            }
            UiWidgets.Button button = detail.add(new UiWidgets.Button(name, () -> {
                settings = settings.withVariant(variant);
                requestPreview();
                rebuild();
            }));
            button.selected = variant.equals(settings.variant());
            button.bounds(bx, rowTop, w, 16);
            bx += w + Ui.GAP;
        }
        detail.reserve(0, Ui.GAP * 2);

        detail.place(UiWidgets.group(Component.translatable("stynastmc.ui.fonts.tuning")), 17, Ui.GAP);
        stepper("size", settings.size(), 0.5F, 4, 32, v -> settings.withSize(v));
        stepper("oversample", settings.oversample(), 1, 1, 16, v -> settings.withOversample(v));
        stepper("shift_x", settings.shiftX(), 0.5F, -8, 8, v -> settings.withShift(v, settings.shiftY()));
        stepper("shift_y", settings.shiftY(), 0.5F, -8, 8, v -> settings.withShift(settings.shiftX(), v));
        detail.reserve(0, Ui.GAP * 2);

        detail.place(UiWidgets.group(Component.translatable("stynastmc.ui.fonts.preview")), 17, Ui.GAP);
        detail.place(new PreviewBox(), 138, Ui.GAP * 3);

        int actionsTop = detail.reserve(20, Ui.GAP);
        UiWidgets.Button apply = detail.add(new UiWidgets.Button(Component.translatable("stynastmc.ui.fonts.apply"), this::apply));
        apply.bounds(detail.innerX(), actionsTop, 110, 20);
        apply.outline = 0xFF55FF55;
        UiWidgets.Button vanilla = detail.add(new UiWidgets.Button(Component.translatable("stynastmc.ui.fonts.use_vanilla"), this::useVanilla));
        vanilla.bounds(apply.endX() + Ui.GAP * 2, actionsTop, 110, 20);
        vanilla.enabled = FontRuntime.appliedSettings() != null;
        detail.place(new UiWidgets.Label(() -> status, Ui.WHITE), 12, Ui.GAP);
    }

    private void stepper(String key, float value, float step, float min, float max, Function<Float, FontSettings> update) {
        UiWidgets.Row row = detail.place(new UiWidgets.Row(ConfigTab.label(key)), 20, Ui.GAP);
        UiWidgets.Button plus = row.add(new UiWidgets.Button(Component.literal("+"), () -> nudge(update, Math.min(max, value + step))));
        plus.bounds(row.endX() - 19, row.y + 2, 16, 16);
        UiWidgets.Label shown = row.add(new UiWidgets.Label(Component.literal(format(value)), Ui.WHITE));
        shown.bounds(plus.x - 44, row.y, 40, row.height);
        shown.alignRight = true;
        UiWidgets.Button minus = row.add(new UiWidgets.Button(Component.literal("-"), () -> nudge(update, Math.max(min, value - step))));
        minus.bounds(shown.x - 19, row.y + 2, 16, 16);
    }

    private void nudge(Function<Float, FontSettings> update, float value) {
        settings = update.apply(value);
        requestPreview();
        rebuild();
    }

    private static String format(float value) {
        return value == (int) value ? String.valueOf((int) value) : String.valueOf(value);
    }

    private void requestPreview() {
        int token = ++previewToken;
        FontSettings wanted = settings;
        if (!GoogleFonts.isCached(wanted.family(), wanted.variant())) {
            setStatus(Component.translatable("stynastmc.ui.fonts.downloading", wanted.family()), Ui.MUTED);
        }
        GoogleFonts.download(wanted.family(), wanted.variant()).whenComplete((file, error) -> Minecraft.getInstance().execute(() -> {
            if (token != previewToken) {
                return;
            }
            if (error != null) {
                setStatus(Component.translatable("stynastmc.ui.fonts.download_failed", rootMessage(error)), Ui.ERROR);
                return;
            }
            FontRuntime.setPreview(wanted, file, e -> setStatus(Component.translatable("stynastmc.ui.fonts.load_failed", rootMessage(e)), Ui.ERROR));
            if (statusColor != Ui.ERROR) {
                setStatus(Component.empty(), Ui.MUTED);
            }
        }));
    }

    private void apply() {
        FontSettings wanted = settings;
        setStatus(Component.translatable("stynastmc.ui.fonts.downloading", wanted.family()), Ui.MUTED);
        GoogleFonts.download(wanted.family(), wanted.variant()).whenComplete((file, error) -> Minecraft.getInstance().execute(() -> {
            if (error != null) {
                setStatus(Component.translatable("stynastmc.ui.fonts.download_failed", rootMessage(error)), Ui.ERROR);
                return;
            }
            FontRuntime.apply(wanted, file, e -> setStatus(Component.translatable("stynastmc.ui.fonts.load_failed", rootMessage(e)), Ui.ERROR));
            if (wanted.equals(FontRuntime.appliedSettings())) {
                FontsConfig.save(true, wanted);
                setStatus(Component.translatable("stynastmc.ui.fonts.applied", wanted.family()), 0xFF55FF55);
            }
            rebuild();
        }));
    }

    private void useVanilla() {
        FontRuntime.resetToVanilla();
        FontsConfig.save(false, settings);
        setStatus(Component.translatable("stynastmc.ui.fonts.vanilla_restored"), Ui.MUTED);
        rebuild();
    }

    private void setStatus(Component text, int color) {
        status = text.copy().withColor(color & 0xFFFFFF);
        statusColor = color;
    }

    private void wrapped(Component text, int color) {
        UiWidgets.Label label = new UiWidgets.Label(text, color);
        label.wrap = true;
        detail.place(label, label.wrappedHeight(detail.width - 26), Ui.GAP);
    }

    private void rebuild() {
        if (list != null) {
            listScroll = list.scroll();
        }
        if (detail != null) {
            detailScroll = detail.scroll();
        }
        screen.rebuild();
    }

    private static String rootMessage(Throwable error) {
        while (error.getCause() != null) {
            error = error.getCause();
        }
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private static MutableComponent preview(String text) {
        return Component.literal(text).setStyle(FontRuntime.PREVIEW_STYLE);
    }

    private class FontCard extends UiElement {
        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            Ui.fill(g, x, y, width, height, Ui.CARD_BACKGROUND);
            Ui.outline(g, x, y, width, height, Ui.CARD_OUTLINE);
            Ui.text(g, preview(selected.family()), x + 6, y + 6, Ui.GROUP_LABEL, 1.5F);
            String by = selected.designers().isEmpty() ? "" : " · " + String.join(", ", selected.designers());
            Ui.text(g, Component.literal(selected.category() + by), x + 6, y + 26, Ui.MUTED, 0.75F);
        }
    }

    /** Sample text in the preview font set, plus a vanilla line to compare against. */
    private class PreviewBox extends UiElement {
        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            Ui.fill(g, x, y, width, height, 0x80000000);
            Ui.outline(g, x, y, width, height, Ui.PANEL_OUTLINE);
            g.enableScissor(x + 1, y + 1, endX() - 1, endY() - 1);
            int tx = x + 6;
            int ty = y + 6;
            g.text(Ui.font(), preview("The quick brown fox jumps over the lazy dog"), tx, ty, Ui.WHITE, true);
            g.text(Ui.font(), preview("ABCDEFGHIJKLMNOPQRSTUVWXYZ"), tx, ty += 12, Ui.WHITE, true);
            g.text(Ui.font(), preview("abcdefghijklmnopqrstuvwxyz"), tx, ty += 11, Ui.WHITE, true);
            g.text(Ui.font(), preview("0123456789 !?.,:;'\"()[]{}<>@#$%&*+-=/"), tx, ty += 11, Ui.WHITE, true);
            g.text(Ui.font(), preview("<Steve> ").append(preview("gg, see you tomorrow! :)").withColor(0xE0E0E0)), tx, ty += 14, Ui.WHITE, true);
            ty += 14;
            Ui.sprite(g, BUTTON_SPRITE, tx, ty, 150, 20);
            Ui.centeredText(g, preview("Singleplayer"), tx, ty, 150, 20, Ui.WHITE);
            Ui.text(g, preview("Aa Bb Cc 123"), tx + 160, ty + 2, Ui.GROUP_LABEL, 2.0F);
            ty += 28;
            g.text(Ui.font(), Component.translatable("stynastmc.ui.fonts.vanilla_compare"), tx, ty, Ui.MUTED, true);
            ty += 12;
            g.text(Ui.font(), Component.literal("The quick brown fox jumps over the lazy dog").setStyle(FontRuntime.VANILLA_STYLE), tx, ty, Ui.MUTED, true);
            g.disableScissor();
        }
    }
}
