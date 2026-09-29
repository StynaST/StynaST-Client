package net.styna.stynastmc.config.tabs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerModelType;
import net.styna.stynastmc.config.StynaConfigScreen;
import net.styna.stynastmc.skins.CustomSkins;
import net.styna.stynastmc.skins.SkinImages;
import net.styna.stynastmc.skins.SkinService;
import net.styna.stynastmc.skins.SkinModel;
import net.styna.stynastmc.skins.SkinsConfig;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiPanel;
import net.styna.stynastmc.ui.UiWidgets;

public class SkinTab implements Tab {
    private static final int ENTRY_HEIGHT = 20;
    // File, resolution, model, buttons and status rows plus panel padding.
    private static final int INFO_HEIGHT = 110;

    private final StynaConfigScreen screen;
    private Path highlighted;
    private Component status = Component.empty();
    private double listScroll;
    private boolean dirty;
    private UiPanel list;
    private Model.Simple wideModel;
    private Model.Simple slimModel;
    private float rotationX = -5.0F;
    private float rotationY = 30.0F;

    public SkinTab(StynaConfigScreen screen) {
        this.screen = screen;
    }

    @Override
    public Component title() {
        return Component.translatable("stynastmc.ui.skin");
    }

    @Override
    public void build(UiElement area) {
        if (!SkinsConfig.loaded()) {
            area.add(new UiWidgets.Label(Component.translatable("stynastmc.ui.config_not_loaded"), Ui.ERROR))
                    .bounds(area.x + 10, area.y + 10, area.width - 20, 12);
            return;
        }
        if (highlighted == null) {
            highlighted = CustomSkins.selected();
        }

        int leftWidth = Math.max(160, (int) (area.width * 0.38));
        int left = area.x + 10;

        UiWidgets.Label folderLabel = area.add(UiWidgets.group(Component.translatable("stynastmc.ui.skin.folder")));
        folderLabel.bounds(left, area.y + 8, leftWidth, 12);

        UiWidgets.TextField folder = area.add(new UiWidgets.TextField(SkinsConfig.DIRECTORY.get()));
        folder.bounds(left, folderLabel.endY() + 2, leftWidth - 38, 16);
        folder.placeholder = Component.translatable("stynastmc.ui.skin.folder_hint");
        folder.onChange = value -> {
            SkinsConfig.DIRECTORY.set(value.trim());
            dirty = true;
            highlighted = CustomSkins.selected();
            CustomSkins.refresh();
            fillList();
        };

        UiWidgets.Button open = area.add(new UiWidgets.Button(Component.empty(), this::openFolder).icon(Ui.FOLDER));
        open.bounds(folder.endX() + Ui.GAP, folder.y, 16, 16);
        open.tooltip(Component.translatable("stynastmc.ui.skin.open_folder"));
        UiWidgets.Button rescan = area.add(new UiWidgets.Button(Component.literal("⟳"), () -> {
            CustomSkins.refresh();
            rebuild();
        }));
        rescan.bounds(open.endX() + Ui.GAP, folder.y, 16, 16);
        rescan.tooltip(Component.translatable("stynastmc.ui.skin.rescan"));

        list = area.add(new UiPanel()).bounds(left, folder.endY() + Ui.GAP, leftWidth, area.endY() - folder.endY() - Ui.GAP - 10);
        list.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 4);
        fillList();

        buildDetail(area, list.endX() + 10, area.endX() - list.endX() - 20);
    }

    private void fillList() {
        list.clearChildren();
        Path dir = CustomSkins.directory();
        if (dir == null) {
            list.place(wrapped(Component.translatable("stynastmc.ui.skin.no_folder"), Ui.MUTED), 24, 0);
            return;
        }
        if (!Files.isDirectory(dir)) {
            list.place(wrapped(Component.translatable("stynastmc.ui.skin.folder_missing", dir.toString()), Ui.ERROR), 24, 0);
            return;
        }
        List<Path> files = CustomSkins.list();
        if (files.isEmpty()) {
            list.place(wrapped(Component.translatable("stynastmc.ui.skin.folder_empty"), Ui.MUTED), 24, 0);
            return;
        }
        Path applied = CustomSkins.selected();
        for (Path file : files) {
            Entry entry = list.place(new Entry(file, file.equals(applied)), ENTRY_HEIGHT, 1);
            entry.tooltip(Component.literal(file.toString()));
        }
        list.setScroll(listScroll);
    }

    private UiWidgets.Label wrapped(Component text, int color) {
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
        info.place(new UiWidgets.InfoRow(Component.translatable("stynastmc.ui.skin.file"),
                () -> Component.literal(highlighted == null ? "-" : CustomSkins.name(highlighted))), 16, 2);
        info.place(new UiWidgets.InfoRow(Component.translatable("stynastmc.ui.skin.resolution"),
                () -> Component.literal(loaded == null ? "-" : loaded.width() + "x" + loaded.height())), 16, Ui.GAP);

        UiWidgets.Row model = info.place(new UiWidgets.Row(Component.translatable("stynastmc.ui.skin.model")), 20, Ui.GAP);
        int bx = model.endX() - 3;
        for (SkinModel option : new SkinModel[] {SkinModel.SLIM, SkinModel.WIDE, SkinModel.AUTO}) {
            UiWidgets.Button button = model.add(new UiWidgets.Button(Component.translatable("stynastmc.ui.skin.model." + option.name().toLowerCase(Locale.ROOT)),
                    () -> {
                        CustomSkins.setModel(option);
                        rebuild();
                    }));
            int w = Ui.font().width(button.label.get()) + 10;
            button.bounds(bx - w, model.y + 2, w, 16);
            button.selected = SkinsConfig.MODEL.get() == option;
            bx -= w + Ui.GAP;
        }

        int actions = info.reserve(20, Ui.GAP);
        UiWidgets.Button apply = info.add(new UiWidgets.Button(Component.translatable("stynastmc.ui.skin.apply"), this::apply));
        apply.bounds(info.innerX(), actions, 110, 20);
        apply.outline = 0xFF55FF55;
        apply.enabled = highlighted != null;
        UiWidgets.Button reset = info.add(new UiWidgets.Button(Component.translatable("stynastmc.ui.skin.use_account"), this::useAccount));
        reset.bounds(apply.endX() + Ui.GAP * 2, actions, 110, 20);
        reset.enabled = CustomSkins.enabled() && CustomSkins.selected() != null;

        info.place(new UiWidgets.Label(() -> status, Ui.WHITE), 12, Ui.GAP);
    }

    private void openFolder() {
        Path dir = CustomSkins.directory();
        if (dir != null && Files.isDirectory(dir)) {
            Util.getPlatform().openPath(dir);
        }
    }

    private void apply() {
        if (highlighted == null) {
            return;
        }
        if (CustomSkins.texture(highlighted) == null) {
            setStatus(Component.translatable("stynastmc.ui.skin.failed", String.valueOf(CustomSkins.error(highlighted))), Ui.ERROR);
            rebuild();
            return;
        }
        Path file = highlighted;
        CustomSkins.select(file);
        setStatus(Component.translatable("stynastmc.ui.skin.applied", CustomSkins.name(file)), 0xFF55FF55);
        share(file);
        rebuild();
    }

    private void useAccount() {
        CustomSkins.select(null);
        setStatus(Component.translatable("stynastmc.ui.skin.account_restored"), Ui.MUTED);
        if (SkinService.configured()) {
            SkinService.clear().whenComplete((unused, error) -> Minecraft.getInstance().execute(() -> {
                if (error != null) {
                    setStatus(Component.translatable("stynastmc.ui.skin.share_failed", SkinService.message(error)), Ui.ERROR);
                }
            }));
        }
        rebuild();
    }

    private void share(Path file) {
        if (!SkinService.configured()) {
            return;
        }
        setStatus(Component.translatable("stynastmc.ui.skin.sharing"), Ui.MUTED);
        SkinService.upload(file, SkinsConfig.MODEL.get()).whenComplete((unused, error) -> Minecraft.getInstance().execute(() -> {
            if (error != null) {
                setStatus(Component.translatable("stynastmc.ui.skin.share_failed", SkinService.message(error)), Ui.ERROR);
            } else {
                setStatus(Component.translatable("stynastmc.ui.skin.shared", CustomSkins.name(file)), 0xFF55FF55);
            }
        }));
    }

    private void setStatus(Component text, int color) {
        status = text.copy().withColor(color & 0xFFFFFF);
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

    private Model.Simple model(PlayerModelType type) {
        if (wideModel == null) {
            wideModel = new Model.Simple(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), RenderTypes::entityTranslucent);
            slimModel = new Model.Simple(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), RenderTypes::entityTranslucent);
        }
        return type == PlayerModelType.SLIM ? slimModel : wideModel;
    }

    /** Model type of the account skin, which AUTO follows. */
    private static PlayerModelType accountModel() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? PlayerModelType.WIDE : minecraft.player.getSkin().model();
    }

    private class Entry extends UiElement {
        private final Path file;
        private final boolean applied;

        Entry(Path file, boolean applied) {
            this.file = file;
            this.applied = applied;
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hot = contains(mx, my);
            if (file.equals(highlighted)) {
                Ui.fill(g, x, y, width, height, Ui.HOVER_FILL);
            }
            if (hot) {
                Ui.outline(g, x, y, width, height, Ui.FOCUS_OUTLINE);
            }
            int size = height - 4;
            SkinImages.Loaded loaded = CustomSkins.texture(file);
            if (loaded == null) {
                Ui.fill(g, x + 2, y + 2, size, size, Ui.CARD_BACKGROUND);
                Ui.outline(g, x + 2, y + 2, size, size, Ui.ERROR);
            } else {
                face(g, loaded, x + 2, y + 2, size);
            }
            Component name = Component.literal(CustomSkins.name(file));
            if (applied) {
                name = Component.literal("✔ ").withColor(0x55FF55).append(name.copy().withColor(0xFFFFFF));
            }
            Ui.textInBox(g, name, x + size + 6, y, width - size - 10, height, loaded == null ? Ui.ERROR : Ui.WHITE, false);
        }

        private void face(GuiGraphicsExtractor g, SkinImages.Loaded loaded, int x, int y, int size) {
            int unit = loaded.width() / 64;
            g.blit(RenderPipelines.GUI_TEXTURED, loaded.texture(), x, y, 8 * unit, 8 * unit, size, size, 8 * unit, 8 * unit,
                    loaded.width(), loaded.height());
            g.blit(RenderPipelines.GUI_TEXTURED, loaded.texture(), x, y, 40 * unit, 8 * unit, size, size, 8 * unit, 8 * unit,
                    loaded.width(), loaded.height());
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
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            Ui.fill(g, x, y, width, height, Ui.CARD_BACKGROUND);
            Ui.outline(g, x, y, width, height, Ui.PANEL_OUTLINE);
            SkinImages.Loaded loaded = highlighted == null ? null : CustomSkins.texture(highlighted);
            Identifier texture = loaded == null ? DefaultPlayerSkin.getDefaultTexture() : loaded.texture();
            PlayerModelType type = SkinsConfig.MODEL.get().resolve(accountModel());
            float scale = 0.97F * (height - 8) / 2.125F;
            g.skin(model(type), texture, scale, rotationX, rotationY, -1.0625F, x + 4, y + 4, endX() - 4, endY() - 4);
            if (loaded == null && highlighted != null) {
                Ui.centeredText(g, Component.translatable("stynastmc.ui.skin.failed", String.valueOf(CustomSkins.error(highlighted))),
                        x, endY() - 14, width, 12, Ui.ERROR);
            }
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
            rotationX = Mth.clamp(rotationX - (float) (my - lastY) * 2.5F, -50.0F, 50.0F);
            rotationY += (float) (mx - lastX) * 2.5F;
            lastX = mx;
            lastY = my;
            return true;
        }
    }
}
