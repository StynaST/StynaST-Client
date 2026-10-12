package net.styna.stynastmc.config.tabs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.styna.stynastmc.config.StynaConfigScreen;
import net.styna.stynastmc.skins.CustomSkins;
import net.styna.stynastmc.skins.SkinImages;
import net.styna.stynastmc.skins.SkinModel;
import net.styna.stynastmc.skins.SkinService;
import net.styna.stynastmc.skins.SkinsConfig;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiPanel;
import net.styna.stynastmc.ui.UiWidgets;
import net.styna.stynastmc.util.Text;

public class SkinTab implements Tab {
    private static final int ENTRY_HEIGHT = 20;
    // File, resolution, model, buttons and status rows plus panel padding.
    private static final int INFO_HEIGHT = 110;

    private final StynaConfigScreen screen;
    private Path highlighted;
    private Path browsing;
    private String status = "";
    private double listScroll;
    private boolean dirty;
    private UiPanel list;
    private ModelPlayer wideModel;
    private ModelPlayer slimModel;
    private float rotationX = -5.0F;
    private float rotationY = 30.0F;

    public SkinTab(StynaConfigScreen screen) {
        this.screen = screen;
    }

    @Override
    public String title() {
        return Text.tr("stynastmc.ui.skin");
    }

    @Override
    public void build(UiElement area) {
        if (!SkinsConfig.loaded()) {
            area.add(new UiWidgets.Label(Text.tr("stynastmc.ui.config_not_loaded"), Ui.ERROR))
                    .bounds(area.x + 10, area.y + 10, area.width - 20, 12);
            return;
        }
        if (highlighted == null) {
            highlighted = CustomSkins.selected();
            if (highlighted != null && browsing == null) {
                browsing = highlighted.getParent();
            }
        }

        int leftWidth = Math.max(160, (int) (area.width * 0.38));
        int left = area.x + 10;

        UiWidgets.Label folderLabel = area.add(UiWidgets.group(Text.tr("stynastmc.ui.skin.folder")));
        folderLabel.bounds(left, area.y + 8, leftWidth, 12);

        UiWidgets.TextField folder = area.add(new UiWidgets.TextField(SkinsConfig.DIRECTORY.get()));
        folder.bounds(left, folderLabel.endY() + 2, leftWidth - 38, 16);
        folder.placeholder = CustomSkins.defaultDirectory().toString();
        folder.onChange = value -> {
            SkinsConfig.DIRECTORY.set(value.trim());
            dirty = true;
            highlighted = CustomSkins.selected();
            browsing = null;
            CustomSkins.refresh();
            fillList();
        };

        UiWidgets.Button open = area.add(new UiWidgets.Button("", this::openFolder).icon(Ui.FOLDER));
        open.bounds(folder.endX() + Ui.GAP, folder.y, 16, 16);
        open.tooltip(Text.tr("stynastmc.ui.skin.open_folder"));
        UiWidgets.Button rescan = area.add(new UiWidgets.Button("⟳", () -> {
            CustomSkins.refresh();
            rebuild();
        }));
        rescan.bounds(open.endX() + Ui.GAP, folder.y, 16, 16);
        rescan.tooltip(Text.tr("stynastmc.ui.skin.rescan"));

        list = area.add(new UiPanel()).bounds(left, folder.endY() + Ui.GAP, leftWidth, area.endY() - folder.endY() - Ui.GAP - 10);
        list.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 4);
        fillList();

        buildDetail(area, list.endX() + 10, area.endX() - list.endX() - 20);
    }

    private void fillList() {
        list.clearChildren();
        Path dir = CustomSkins.directory();
        if (dir == null) {
            list.place(wrapped(Text.tr("stynastmc.ui.skin.no_folder"), Ui.MUTED), 24, 0);
            return;
        }
        if (!Files.isDirectory(dir)) {
            list.place(wrapped(Text.tr("stynastmc.ui.skin.folder_missing", dir.toString()), Ui.ERROR), 24, 0);
            return;
        }
        if (browsing == null || !browsing.startsWith(dir) || !Files.isDirectory(browsing)) {
            browsing = dir;
        }
        if (!browsing.equals(dir)) {
            FolderEntry up = list.place(new FolderEntry(browsing.getParent(), ".."), ENTRY_HEIGHT, 1);
            up.tooltip(CustomSkins.name(browsing));
        }
        List<Path> files = CustomSkins.list(browsing);
        if (files.isEmpty()) {
            list.place(wrapped(Text.tr("stynastmc.ui.skin.folder_empty"), Ui.MUTED), 24, 0);
            return;
        }
        Path applied = CustomSkins.selected();
        for (Path file : files) {
            UiElement entry = Files.isDirectory(file)
                    ? new FolderEntry(file, file.getFileName() + "/")
                    : new Entry(file, file.equals(applied));
            list.place(entry, ENTRY_HEIGHT, 1);
            entry.tooltip(file.toString());
        }
        list.setScroll(listScroll);
    }

    private UiWidgets.Label wrapped(String text, int color) {
        UiWidgets.Label label = new UiWidgets.Label(text, color);
        label.wrap = true;
        return label;
    }

    private void buildDetail(UiElement area, int x, int width) {
        Preview preview = area.add(new Preview());
        int previewHeight = Math.min(area.height - 20 - Ui.GAP - INFO_HEIGHT, (int) (width * 1.1));
        preview.bounds(x, area.y + 10, width, Math.max(60, previewHeight));

        UiPanel info = area.add(new UiPanel()).bounds(x, preview.endY() + Ui.GAP, width, area.endY() - preview.endY() - Ui.GAP - 10);
        info.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 6);

        SkinImages.Loaded loaded = highlighted == null ? null : CustomSkins.texture(highlighted);
        info.place(new UiWidgets.InfoRow(Text.tr("stynastmc.ui.skin.file"),
                () -> highlighted == null ? "-" : CustomSkins.name(highlighted)), 16, 2);
        info.place(new UiWidgets.InfoRow(Text.tr("stynastmc.ui.skin.resolution"),
                () -> loaded == null ? "-" : loaded.width + "x" + loaded.height), 16, Ui.GAP);

        UiWidgets.Row model = info.place(new UiWidgets.Row(Text.tr("stynastmc.ui.skin.model")), 20, Ui.GAP);
        int bx = model.endX() - 3;
        for (SkinModel option : new SkinModel[] {SkinModel.SLIM, SkinModel.WIDE, SkinModel.AUTO}) {
            UiWidgets.Button button = model.add(new UiWidgets.Button(Text.tr("stynastmc.ui.skin.model." + option.name().toLowerCase(Locale.ROOT)),
                    () -> {
                        CustomSkins.setModel(option);
                        rebuild();
                    }));
            int w = Ui.width(button.label.get()) + 10;
            button.bounds(bx - w, model.y + 2, w, 16);
            button.selected = SkinsConfig.MODEL.get() == option;
            bx -= w + Ui.GAP;
        }

        int actions = info.reserve(20, Ui.GAP);
        UiWidgets.Button apply = info.add(new UiWidgets.Button(Text.tr("stynastmc.ui.skin.apply"), this::apply));
        apply.bounds(info.innerX(), actions, 110, 20);
        apply.outline = Ui.SUCCESS;
        apply.enabled = highlighted != null;
        UiWidgets.Button reset = info.add(new UiWidgets.Button(Text.tr("stynastmc.ui.skin.use_account"), this::useAccount));
        reset.bounds(apply.endX() + Ui.GAP * 2, actions, 110, 20);
        reset.enabled = CustomSkins.enabled();

        info.place(new UiWidgets.Label(() -> status, Ui.WHITE), 12, Ui.GAP);
    }

    private void openFolder() {
        Path dir = CustomSkins.directory();
        if (dir != null && Files.isDirectory(dir)) {
            OpenGlHelper.openFile(dir.toFile());
        }
    }

    private void apply() {
        if (highlighted == null) {
            return;
        }
        if (CustomSkins.texture(highlighted) == null) {
            setStatus(Text.tr("stynastmc.ui.skin.failed", String.valueOf(CustomSkins.error(highlighted))), Ui.ERROR);
            rebuild();
            return;
        }
        Path file = highlighted;
        CustomSkins.select(file);
        setStatus(Text.tr("stynastmc.ui.skin.applied", CustomSkins.name(file)), Ui.SUCCESS);
        share(file);
        rebuild();
    }

    private void useAccount() {
        CustomSkins.select(null);
        setStatus(Text.tr("stynastmc.ui.skin.account_restored"), Ui.MUTED);
        if (SkinService.configured()) {
            SkinService.clear().whenComplete((unused, error) -> Minecraft.getMinecraft().addScheduledTask(() -> {
                if (error != null) {
                    setStatus(Text.tr("stynastmc.ui.skin.share_failed", SkinService.message(error)), Ui.ERROR);
                }
            }));
        }
        rebuild();
    }

    private void share(Path file) {
        if (!SkinService.configured()) {
            return;
        }
        setStatus(Text.tr("stynastmc.ui.skin.sharing"), Ui.MUTED);
        CustomSkins.shareSelected().whenComplete((unused, error) -> Minecraft.getMinecraft().addScheduledTask(() -> {
            if (error != null) {
                setStatus(Text.tr("stynastmc.ui.skin.share_failed", SkinService.message(error)), Ui.ERROR);
            } else {
                setStatus(Text.tr("stynastmc.ui.skin.shared", CustomSkins.name(file)), Ui.SUCCESS);
            }
        }));
    }

    private void setStatus(String text, int color) {
        status = Text.closest(color & 0xFFFFFF) + text;
    }

    private void rebuild() {
        if (list != null) {
            listScroll = list.scroll();
        }
        screen.rebuild();
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

    private ModelPlayer model(boolean slim) {
        if (wideModel == null) {
            wideModel = new ModelPlayer(0.0F, false);
            slimModel = new ModelPlayer(0.0F, true);
        }
        return slim ? slimModel : wideModel;
    }

    /** Skin type of the account skin, which AUTO follows. */
    private static String accountModel() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.player == null ? "default" : minecraft.player.getSkinType();
    }

    private class FolderEntry extends UiElement {
        private final Path folder;
        private final String name;

        FolderEntry(Path folder, String name) {
            this.folder = folder;
            this.name = name;
        }

        @Override
        protected void renderSelf(int mx, int my, float delta) {
            if (contains(mx, my)) {
                Ui.outline(x, y, width, height, Ui.FOCUS_OUTLINE);
            }
            int size = height - 4;
            Ui.sprite(Ui.FOLDER, x + 2, y + 2, size, size);
            Ui.textInBox(name, x + size + 6, y, width - size - 10, height, Ui.WHITE, false);
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            UiWidgets.playClick();
            browsing = folder;
            listScroll = 0;
            fillList();
            return true;
        }
    }

    private class Entry extends UiElement {
        private final Path file;
        private final boolean applied;

        Entry(Path file, boolean applied) {
            this.file = file;
            this.applied = applied;
        }

        @Override
        protected void renderSelf(int mx, int my, float delta) {
            boolean hot = contains(mx, my);
            if (file.equals(highlighted)) {
                Ui.fill(x, y, width, height, Ui.HOVER_FILL);
            }
            if (hot) {
                Ui.outline(x, y, width, height, Ui.FOCUS_OUTLINE);
            }
            int size = height - 4;
            SkinImages.Loaded loaded = CustomSkins.texture(file);
            if (loaded == null) {
                Ui.fill(x + 2, y + 2, size, size, Ui.CARD_BACKGROUND);
                Ui.outline(x + 2, y + 2, size, size, Ui.ERROR);
            } else {
                face(loaded, x + 2, y + 2, size);
            }
            String name = file.getFileName().toString();
            if (applied) {
                name = "§a✔ §f" + name;
            }
            Ui.textInBox(name, x + size + 6, y, width - size - 10, height, loaded == null ? Ui.ERROR : Ui.WHITE, false);
        }

        private void face(SkinImages.Loaded loaded, int x, int y, int size) {
            int unit = loaded.width / 64;
            Minecraft.getMinecraft().getTextureManager().bindTexture(loaded.texture);
            GlStateManager.enableBlend();
            GlStateManager.color(1, 1, 1, 1);
            Gui.drawScaledCustomSizeModalRect(x, y, 8 * unit, 8 * unit, 8 * unit, 8 * unit, size, size, loaded.width, loaded.height);
            Gui.drawScaledCustomSizeModalRect(x, y, 40 * unit, 8 * unit, 8 * unit, 8 * unit, size, size, loaded.width, loaded.height);
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            UiWidgets.playClick();
            highlighted = file;
            rebuild();
            return true;
        }
    }

    private class Preview extends UiElement {
        private double lastX;
        private double lastY;

        @Override
        protected void renderSelf(int mx, int my, float delta) {
            Ui.fill(x, y, width, height, Ui.CARD_BACKGROUND);
            Ui.outline(x, y, width, height, Ui.PANEL_OUTLINE);
            SkinImages.Loaded loaded = highlighted == null ? null : CustomSkins.texture(highlighted);
            ResourceLocation texture = loaded == null ? DefaultPlayerSkin.getDefaultSkinLegacy() : loaded.texture;
            boolean slim = "slim".equals(SkinsConfig.MODEL.get().resolve(accountModel()));
            Ui.enableScissor(x + 1, y + 1, endX() - 1, endY() - 1);
            drawPlayer(model(slim), texture, x + width / 2.0F, y + height / 2.0F, 0.97F * (height - 8) / 2.125F);
            Ui.disableScissor();
            if (loaded == null && highlighted != null) {
                Ui.centeredText(Text.tr("stynastmc.ui.skin.failed", String.valueOf(CustomSkins.error(highlighted))),
                        x, endY() - 14, width, 12, Ui.ERROR);
            }
        }

        private void drawPlayer(ModelPlayer model, ResourceLocation texture, float cx, float cy, float scale) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            GlStateManager.pushMatrix();
            GlStateManager.translate(cx, cy, 150);
            // Mirrored on X like vanilla's entity rendering, so face winding survives back face culling.
            GlStateManager.scale(-scale, scale, scale);
            GlStateManager.rotate(-rotationX, 1, 0, 0);
            GlStateManager.rotate(180.0F - rotationY, 0, 1, 0);
            GlStateManager.translate(0, -0.5F, 0);
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.enableDepth();
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            RenderHelper.enableStandardItemLighting();
            float unit = 0.0625F;
            for (ModelRenderer part : new ModelRenderer[] {model.bipedHead, model.bipedHeadwear, model.bipedBody, model.bipedBodyWear,
                    model.bipedRightArm, model.bipedRightArmwear, model.bipedLeftArm, model.bipedLeftArmwear,
                    model.bipedRightLeg, model.bipedRightLegwear, model.bipedLeftLeg, model.bipedLeftLegwear}) {
                part.render(unit);
            }
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableDepth();
            GlStateManager.popMatrix();
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            lastX = mx;
            lastY = my;
            if (screen() != null) {
                screen().startDrag(this);
            }
            return true;
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button) {
            rotationX = MathHelper.clamp(rotationX - (float) (my - lastY) * 2.5F, -50.0F, 50.0F);
            rotationY += (float) (mx - lastX) * 2.5F;
            lastX = mx;
            lastY = my;
            return true;
        }
    }
}
