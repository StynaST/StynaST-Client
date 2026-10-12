package net.styna.stynastmc.config.tabs;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.styna.stynastmc.config.StynaConfigScreen;
import net.styna.stynastmc.fonts.FontRuntime;
import net.styna.stynastmc.fonts.FontSettings;
import net.styna.stynastmc.fonts.FontsConfig;
import net.styna.stynastmc.fonts.GoogleFonts;
import net.styna.stynastmc.fonts.TtfFont;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiPanel;
import net.styna.stynastmc.ui.UiWidgets;
import net.styna.stynastmc.util.Text;

public class FontsTab implements Tab {
    private static final List<String> CATEGORIES = Arrays.asList("", "Sans Serif", "Serif", "Display", "Handwriting", "Monospace");
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");

    private final StynaConfigScreen screen;
    private List<GoogleFonts.Family> catalog;
    private String catalogError;
    private String query = "";
    private int category;
    private GoogleFonts.Family selected;
    private FontSettings settings;
    private String status = "";
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
    public String title() {
        return Text.tr("stynastmc.ui.fonts");
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
        search.placeholder = Text.tr("stynastmc.ui.fonts.search", catalog == null ? "…" : catalog.size());
        search.onChange = s -> {
            query = s;
            listScroll = 0;
            fillList();
        };

        UiWidgets.Button categoryButton = area.add(new UiWidgets.Button(() -> Text.tr("stynastmc.ui.fonts.category",
                category == 0 ? Text.tr("stynastmc.ui.fonts.all") : CATEGORIES.get(category)), null));
        categoryButton.onPressButton = mouse -> {
            category = (category + (mouse == 1 ? CATEGORIES.size() - 1 : 1)) % CATEGORIES.size();
            listScroll = 0;
            fillList();
        };
        categoryButton.bounds(left, search.endY() + Ui.GAP, leftWidth - 19, 16);
        UiWidgets.Button refresh = area.add(new UiWidgets.Button("⟳", () -> loadCatalog(true)));
        refresh.bounds(categoryButton.endX() + Ui.GAP, categoryButton.y, 16, 16);
        refresh.tooltip(Text.tr("stynastmc.ui.fonts.refresh"));

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
        setStatus(Text.tr("stynastmc.ui.fonts.loading_catalog"), Ui.MUTED);
        GoogleFonts.catalog(refresh).whenComplete((families, error) -> Minecraft.getMinecraft().addScheduledTask(() -> {
            if (error != null) {
                catalogError = rootMessage(error);
                setStatus(Text.tr("stynastmc.ui.fonts.catalog_failed", catalogError), Ui.ERROR);
            } else {
                catalog = families;
                setStatus("", Ui.MUTED);
                if (selected == null && !Text.isBlank(settings.family)) {
                    for (GoogleFonts.Family family : families) {
                        if (family.family.equals(settings.family)) {
                            selected = family;
                            requestPreview();
                            break;
                        }
                    }
                }
            }
            rebuild();
        }));
    }

    private void fillList() {
        list.clearChildren();
        if (catalog == null) {
            list.place(new UiWidgets.Label(catalogError == null ? Text.tr("stynastmc.ui.fonts.loading_catalog")
                    : Text.tr("stynastmc.ui.fonts.catalog_failed", catalogError), catalogError == null ? Ui.MUTED : Ui.ERROR), 14, 0);
            return;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        FontSettings applied = FontRuntime.appliedSettings();
        for (GoogleFonts.Family family : catalog) {
            if ((category != 0 && !family.category.equals(CATEGORIES.get(category)))
                    || (!q.isEmpty() && !family.family.toLowerCase(Locale.ROOT).contains(q))) {
                continue;
            }
            boolean isApplied = applied != null && applied.family.equals(family.family);
            String name = isApplied ? "§a✔ §f" + family.family : family.family;
            UiWidgets.Button row = list.place(new UiWidgets.Button(name, () -> select(family)).transparent(), 14, 0);
            row.selected = family == selected;
            row.tooltip(family.category + " · " + family.variants.size() + " styles");
        }
        list.setScroll(listScroll);
    }

    private void select(GoogleFonts.Family family) {
        selected = family;
        settings = settings.withFamily(family.family, family.defaultVariant());
        detailScroll = 0;
        requestPreview();
        rebuild();
    }

    private void fillDetail() {
        if (selected == null) {
            detail.place(UiWidgets.group(Text.tr("stynastmc.ui.fonts.pick")), 17, Ui.GAP);
            wrapped(Text.tr("stynastmc.ui.fonts.pick_hint"), Ui.MUTED);
            detail.place(new UiWidgets.Label(() -> status, Ui.WHITE), 12, Ui.GAP);
            return;
        }

        detail.place(new FontCard(), 40, Ui.GAP * 2);

        detail.place(UiWidgets.group(Text.tr("stynastmc.ui.fonts.style")), 17, Ui.GAP);
        int rowTop = detail.reserve(16, Ui.GAP);
        int bx = detail.innerX();
        int limit = detail.innerX() + detail.width - 26;
        for (String variant : selected.variants) {
            String name = GoogleFonts.variantName(variant);
            int w = Ui.width(name) + 10;
            if (bx + w > limit) {
                bx = detail.innerX();
                rowTop = detail.reserve(16, Ui.GAP);
            }
            UiWidgets.Button button = detail.add(new UiWidgets.Button(name, () -> {
                settings = settings.withVariant(variant);
                requestPreview();
                rebuild();
            }));
            button.selected = variant.equals(settings.variant);
            button.bounds(bx, rowTop, w, 16);
            bx += w + Ui.GAP;
        }
        detail.reserve(0, Ui.GAP * 2);

        detail.place(UiWidgets.group(Text.tr("stynastmc.ui.fonts.tuning")), 17, Ui.GAP);
        stepper("size", settings.size, 0.5F, 4, 32, v -> settings.withSize(v));
        stepper("oversample", settings.oversample, 1, 1, 16, v -> settings.withOversample(v));
        stepper("shift_x", settings.shiftX, 0.5F, -8, 8, v -> settings.withShift(v, settings.shiftY));
        stepper("shift_y", settings.shiftY, 0.5F, -8, 8, v -> settings.withShift(settings.shiftX, v));
        detail.reserve(0, Ui.GAP * 2);

        detail.place(UiWidgets.group(Text.tr("stynastmc.ui.fonts.preview")), 17, Ui.GAP);
        detail.place(new PreviewBox(), 138, Ui.GAP * 3);

        int actionsTop = detail.reserve(20, Ui.GAP);
        UiWidgets.Button apply = detail.add(new UiWidgets.Button(Text.tr("stynastmc.ui.fonts.apply"), this::apply));
        apply.bounds(detail.innerX(), actionsTop, 110, 20);
        apply.outline = Ui.SUCCESS;
        UiWidgets.Button vanilla = detail.add(new UiWidgets.Button(Text.tr("stynastmc.ui.fonts.use_vanilla"), this::useVanilla));
        vanilla.bounds(apply.endX() + Ui.GAP * 2, actionsTop, 110, 20);
        vanilla.enabled = FontRuntime.appliedSettings() != null;
        detail.place(new UiWidgets.Label(() -> status, Ui.WHITE), 12, Ui.GAP);
    }

    private void stepper(String key, float value, float step, float min, float max, Function<Float, FontSettings> update) {
        UiWidgets.Row row = detail.place(new UiWidgets.Row(ConfigTab.label(key)), 20, Ui.GAP);
        UiWidgets.Button plus = row.add(new UiWidgets.Button("+", () -> nudge(update, Math.min(max, value + step))));
        plus.bounds(row.endX() - 19, row.y + 2, 16, 16);
        UiWidgets.Label shown = row.add(new UiWidgets.Label(format(value), Ui.WHITE));
        shown.bounds(plus.x - 44, row.y, 40, row.height);
        shown.alignRight = true;
        UiWidgets.Button minus = row.add(new UiWidgets.Button("-", () -> nudge(update, Math.max(min, value - step))));
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
        if (!GoogleFonts.isCached(wanted.family, wanted.variant)) {
            setStatus(Text.tr("stynastmc.ui.fonts.downloading", wanted.family), Ui.MUTED);
        }
        GoogleFonts.download(wanted.family, wanted.variant).whenComplete((file, error) -> Minecraft.getMinecraft().addScheduledTask(() -> {
            if (token != previewToken) {
                return;
            }
            if (error != null) {
                setStatus(Text.tr("stynastmc.ui.fonts.download_failed", rootMessage(error)), Ui.ERROR);
                return;
            }
            FontRuntime.setPreview(wanted, file, e -> setStatus(Text.tr("stynastmc.ui.fonts.load_failed", rootMessage(e)), Ui.ERROR));
            if (statusColor != Ui.ERROR) {
                setStatus("", Ui.MUTED);
            }
        }));
    }

    private void apply() {
        FontSettings wanted = settings;
        setStatus(Text.tr("stynastmc.ui.fonts.downloading", wanted.family), Ui.MUTED);
        GoogleFonts.download(wanted.family, wanted.variant).whenComplete((file, error) -> Minecraft.getMinecraft().addScheduledTask(() -> {
            if (error != null) {
                setStatus(Text.tr("stynastmc.ui.fonts.download_failed", rootMessage(error)), Ui.ERROR);
                return;
            }
            FontRuntime.apply(wanted, file, e -> setStatus(Text.tr("stynastmc.ui.fonts.load_failed", rootMessage(e)), Ui.ERROR));
            if (wanted.equals(FontRuntime.appliedSettings())) {
                FontsConfig.save(true, wanted);
                setStatus(Text.tr("stynastmc.ui.fonts.applied", wanted.family), Ui.SUCCESS);
            }
            rebuild();
        }));
    }

    private void useVanilla() {
        FontRuntime.resetToVanilla();
        FontsConfig.save(false, settings);
        setStatus(Text.tr("stynastmc.ui.fonts.vanilla_restored"), Ui.MUTED);
        rebuild();
    }

    private void setStatus(String text, int color) {
        status = text.isEmpty() ? "" : Text.closest(color & 0xFFFFFF) + text;
        statusColor = color;
    }

    private void wrapped(String text, int color) {
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

    /** Preview text in the previewed font, or vanilla until it has loaded. */
    private static void preview(String text, float x, float y, int color, float scale) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, 1);
        TtfFont font = FontRuntime.preview();
        if (font != null) {
            font.drawString(text, 0, 0, color, true);
        } else {
            FontRuntime.drawVanilla(text, 0, 0, color);
        }
        GlStateManager.popMatrix();
    }

    private static float previewWidth(String text) {
        TtfFont font = FontRuntime.preview();
        return font != null ? font.width(text) : Ui.width(text);
    }

    private class FontCard extends UiElement {
        @Override
        protected void renderSelf(int mx, int my, float delta) {
            Ui.fill(x, y, width, height, Ui.CARD_BACKGROUND);
            Ui.outline(x, y, width, height, Ui.CARD_OUTLINE);
            preview(selected.family, x + 6, y + 6, Ui.GROUP_LABEL, 1.5F);
            String by = selected.designers.isEmpty() ? "" : " · " + String.join(", ", selected.designers);
            Ui.text(selected.category + by, x + 6, y + 26, Ui.MUTED, 0.75F);
        }
    }

    private class PreviewBox extends UiElement {
        @Override
        protected void renderSelf(int mx, int my, float delta) {
            Ui.fill(x, y, width, height, 0x80000000);
            Ui.outline(x, y, width, height, Ui.PANEL_OUTLINE);
            Ui.enableScissor(x + 1, y + 1, endX() - 1, endY() - 1);
            int tx = x + 6;
            int ty = y + 6;
            preview("The quick brown fox jumps over the lazy dog", tx, ty, Ui.WHITE, 1);
            preview("ABCDEFGHIJKLMNOPQRSTUVWXYZ", tx, ty += 12, Ui.WHITE, 1);
            preview("abcdefghijklmnopqrstuvwxyz", tx, ty += 11, Ui.WHITE, 1);
            preview("0123456789 !?.,:;'\"()[]{}<>@#$%&*+-=/", tx, ty += 11, Ui.WHITE, 1);
            ty += 14;
            preview("<Steve> ", tx, ty, Ui.WHITE, 1);
            preview("gg, see you tomorrow! :)", tx + previewWidth("<Steve> "), ty, 0xFFE0E0E0, 1);
            ty += 14;
            Minecraft.getMinecraft().getTextureManager().bindTexture(WIDGETS);
            GlStateManager.color(1, 1, 1, 1);
            Gui.drawModalRectWithCustomSizedTexture(tx, ty, 0, 66, 75, 20, 256, 256);
            Gui.drawModalRectWithCustomSizedTexture(tx + 75, ty, 125, 66, 75, 20, 256, 256);
            preview("Singleplayer", tx + (150 - previewWidth("Singleplayer")) / 2, ty + 6, Ui.WHITE, 1);
            preview("Aa Bb Cc 123", tx + 160, ty + 2, Ui.GROUP_LABEL, 2.0F);
            ty += 28;
            Ui.font().drawStringWithShadow(Text.tr("stynastmc.ui.fonts.vanilla_compare"), tx, ty, Ui.MUTED);
            ty += 12;
            FontRuntime.drawVanilla("The quick brown fox jumps over the lazy dog", tx, ty, Ui.MUTED);
            Ui.disableScissor();
        }
    }
}
