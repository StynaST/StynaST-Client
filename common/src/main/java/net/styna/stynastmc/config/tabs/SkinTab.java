package net.styna.stynastmc.config.tabs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.Blaze3D;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.styna.stynastmc.capes.CapePreview;
import net.styna.stynastmc.capes.CapeProvider;
import net.styna.stynastmc.capes.CapeTexture;
import net.styna.stynastmc.capes.Capes;
import net.styna.stynastmc.capes.CapesConfig;
import net.styna.stynastmc.capes.CustomCapes;
import net.styna.stynastmc.capes.MojangCapes;
import net.styna.stynastmc.capes.RemoteCapes;
import net.styna.stynastmc.config.StynaConfigScreen;
import net.styna.stynastmc.config.StynaConfigSpec;
import net.styna.stynastmc.skins.CustomSkins;
import net.styna.stynastmc.skins.SkinImages;
import net.styna.stynastmc.skins.SkinService;
import net.styna.stynastmc.skins.SkinModel;
import net.styna.stynastmc.skins.SkinsBridge;
import net.styna.stynastmc.skins.SkinsConfig;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiPanel;
import net.styna.stynastmc.ui.UiWidgets;

/** Skins pane on top, capes pane below, one preview for whichever was picked last. */
public class SkinTab implements Tab {
    private static final int ENTRY_HEIGHT = 20;
    // Info rows, buttons and status line plus panel padding.
    private static final int INFO_HEIGHT = 110;
    private static final int GREEN = 0xFF55FF55;
    private static final float FRONT = 30.0F;
    private static final float BACK = 210.0F;

    private enum CapeView {
        ROOT,
        PROVIDER,
        CUSTOM
    }

    /** A highlightable cape. Mojang with no account cape means hiding it. */
    private record CapeChoice(CapeProvider provider, MojangCapes.AccountCape account, Path file) {
        static CapeChoice custom(Path file) {
            return new CapeChoice(null, null, file);
        }
    }

    private final StynaConfigScreen screen;
    // Folders open in either pane survive closing the screen; the tab is rebuilt on every open.
    private static CapeView capeView = CapeView.ROOT;
    private boolean capesFocused;
    private static CapeProvider provider;
    private Path highlighted;
    private CapeChoice cape;
    private static Path browsing;
    private Component status = Component.empty();
    private double skinScroll;
    private double capeScroll;
    private boolean dirty;
    private boolean capesDirty;
    private String lastSignature;
    private UiPanel skinList;
    private UiPanel capeList;
    private Model.Simple wideModel;
    private Model.Simple slimModel;
    private Supplier<PlayerSkin> accountSkin;
    private float rotationX = -5.0F;
    private float rotationY = FRONT;

    public SkinTab(StynaConfigScreen screen) {
        this.screen = screen;
    }

    @Override
    public Component title() {
        return Component.translatable("stynastmc.ui.skin");
    }

    @Override
    public void build(UiElement area) {
        if (!SkinsConfig.loaded() || !CapesConfig.loaded()) {
            area.add(new UiWidgets.Label(Component.translatable("stynastmc.ui.config_not_loaded"), Ui.ERROR))
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
        int top = area.y + 8;
        int half = (area.endY() - 10 - top - 8) / 2;

        int skinTop = buildSkinHeader(area, left, top, leftWidth);
        skinList = pane(area, left, skinTop, leftWidth, top + half - skinTop);
        int capeHeaderTop = skinList.endY() + 8;
        int capeTop = buildCapeHeader(area, left, capeHeaderTop, leftWidth);
        capeList = pane(area, left, capeTop, leftWidth, area.endY() - 10 - capeTop);
        fillSkins();
        fillCapes();

        buildDetail(area, skinList.endX() + 10, area.endX() - skinList.endX() - 20);
        lastSignature = signature();
    }

    private UiPanel pane(UiElement area, int x, int y, int width, int height) {
        UiPanel pane = area.add(new UiPanel()).bounds(x, y, width, Math.max(ENTRY_HEIGHT * 2, height));
        pane.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 4);
        return pane;
    }

    private int buildSkinHeader(UiElement area, int left, int y, int width) {
        UiWidgets.Label label = area.add(UiWidgets.group(Component.translatable("stynastmc.ui.skin.folder")));
        label.bounds(left, y, width, 12);
        int row = label.endY() + 2;
        folderRow(area, left, row, width, SkinsConfig.DIRECTORY, CustomSkins.defaultDirectory(), CustomSkins::directory, () -> {
            dirty = true;
            highlighted = CustomSkins.selected();
            browsing = null;
            CustomSkins.refresh();
            fillSkins();
        }, CustomSkins::refresh);
        return row + 16 + Ui.GAP;
    }

    private int buildCapeHeader(UiElement area, int left, int y, int width) {
        Component title = switch (capeView) {
            case ROOT -> Component.translatable("stynastmc.ui.cape.capes");
            case PROVIDER -> Component.translatable("stynastmc.ui.cape.provider_title", provider.title());
            case CUSTOM -> Component.translatable("stynastmc.ui.cape.folder");
        };
        UiWidgets.Label label = area.add(UiWidgets.group(title));
        label.bounds(left, y, width, 12);
        int row = label.endY() + 2;
        switch (capeView) {
            case ROOT -> area.add(new UiWidgets.Label(Component.translatable("stynastmc.ui.cape.root_hint"), Ui.MUTED))
                    .bounds(left, row + 4, width, 12);
            case CUSTOM -> folderRow(area, left, row, width, CapesConfig.DIRECTORY, CustomCapes.defaultDirectory(), CustomCapes::directory, () -> {
                capesDirty = true;
                cape = null;
                CustomCapes.refresh();
                fillCapes();
            }, CustomCapes::refresh);
            case PROVIDER -> providerRow(area, left, row, width);
        }
        return row + 16 + Ui.GAP;
    }

    private void folderRow(UiElement area, int left, int y, int width, StynaConfigSpec.ConfigValue<String> value, Path fallback,
            Supplier<Path> directory, Runnable onChange, Runnable rescan) {
        UiWidgets.TextField folder = area.add(new UiWidgets.TextField(value.get()));
        folder.bounds(left, y, width - 38, 16);
        folder.placeholder = Component.literal(fallback.toString());
        folder.onChange = text -> {
            value.set(text.trim());
            onChange.run();
        };
        UiWidgets.Button open = area.add(new UiWidgets.Button(Component.empty(), () -> {
            Path dir = directory.get();
            if (dir != null && Files.isDirectory(dir)) {
                Blaze3D.openPath(dir);
            }
        }).icon(Ui.FOLDER));
        open.bounds(folder.endX() + Ui.GAP, y, 16, 16);
        open.tooltip(Component.translatable("stynastmc.ui.skin.open_folder"));
        UiWidgets.Button refresh = area.add(new UiWidgets.Button(Component.literal("⟳"), () -> {
            rescan.run();
            rebuild();
        }));
        refresh.bounds(open.endX() + Ui.GAP, y, 16, 16);
        refresh.tooltip(Component.translatable("stynastmc.ui.skin.rescan"));
    }

    private void providerRow(UiElement area, int left, int y, int width) {
        List<CapeProvider> enabled = Capes.enabled();
        int index = enabled.indexOf(provider);
        UiWidgets.Toggle toggle = area.add(new UiWidgets.Toggle(index >= 0, on -> {
            List<CapeProvider> next = new ArrayList<>(Capes.enabled());
            if (on) {
                next.add(provider);
            } else {
                next.remove(provider);
            }
            Capes.setEnabled(next);
            rebuild();
        }));
        toggle.bounds(left, y + 2, 24, 12);
        toggle.tooltip(Component.translatable(switch (provider) {
            case MOJANG -> "stynastmc.ui.cape.show.tooltip.mojang";
            default -> "stynastmc.ui.cape.show.tooltip";
        }));
        UiWidgets.Label show = area.add(new UiWidgets.Label(Component.translatable("stynastmc.ui.cape.show"), Ui.WHITE));
        show.bounds(toggle.endX() + 4, y + 4, 90, 12);

        int x = left + width;
        UiWidgets.Button site = area.add(new UiWidgets.Button(Component.translatable("stynastmc.ui.cape.website"), () -> Blaze3D.openUri(provider.homepage())));
        int siteWidth = Ui.font().width(site.label.get()) + 10;
        x -= siteWidth;
        site.bounds(x, y, siteWidth, 16);
        if (index >= 0) {
            UiWidgets.Button down = area.add(new UiWidgets.Button(Component.literal("↓"), () -> move(1)));
            x -= 16 + Ui.GAP;
            down.bounds(x, y, 16, 16);
            down.enabled = index < enabled.size() - 1;
            down.tooltip(Component.translatable("stynastmc.ui.cape.later"));
            UiWidgets.Button up = area.add(new UiWidgets.Button(Component.literal("↑"), () -> move(-1)));
            x -= 16 + Ui.GAP;
            up.bounds(x, y, 16, 16);
            up.enabled = index > 0;
            up.tooltip(Component.translatable("stynastmc.ui.cape.earlier"));
            UiWidgets.Label order = area.add(new UiWidgets.Label(Component.literal("#" + (index + 1)), Ui.MUTED));
            int orderWidth = Ui.font().width("#" + (index + 1));
            order.bounds(x - orderWidth - Ui.GAP, y + 4, orderWidth, 12);
        }
    }

    private void move(int offset) {
        List<CapeProvider> next = new ArrayList<>(Capes.enabled());
        int index = next.indexOf(provider);
        int target = index + offset;
        if (index < 0 || target < 0 || target >= next.size()) {
            return;
        }
        next.set(index, next.get(target));
        next.set(target, provider);
        Capes.setEnabled(next);
        rebuild();
    }

    private void open(CapeView next, CapeProvider nextProvider) {
        focus(true);
        capeView = next;
        provider = nextProvider;
        cape = null;
        capeScroll = 0;
        if (next == CapeView.PROVIDER && nextProvider == CapeProvider.MOJANG && MojangCapes.error() != null) {
            MojangCapes.refresh();
        }
        rebuild();
    }

    /** Turns the preview to face whichever pane was used last. */
    private void focus(boolean capes) {
        if (capes != capesFocused) {
            capesFocused = capes;
            rotationY = capes ? BACK : FRONT;
        }
    }

    private void fillCapes() {
        capeList.clearChildren();
        switch (capeView) {
            case ROOT -> fillCapeRoot();
            case PROVIDER -> fillProvider();
            case CUSTOM -> fillCustom();
        }
        capeList.setScroll(capeScroll);
    }

    private void fillCapeRoot() {
        List<CapeProvider> enabled = Capes.enabled();
        CapeProvider own = CapeProvider.byId(Capes.own());
        for (CapeProvider option : CapeProvider.values()) {
            int index = enabled.indexOf(option);
            Component tag = index >= 0 ? Component.literal("#" + (index + 1)).withColor(GREEN & 0xFFFFFF)
                    : Component.translatable("stynastmc.ui.cape.off").withColor(Ui.MUTED & 0xFFFFFF);
            Component name = Component.literal(option.title());
            if (option == own) {
                name = Component.literal("✔ ").withColor(GREEN & 0xFFFFFF).append(name.copy().withColor(0xFFFFFF));
            }
            capeList.place(new FolderEntry(name, tag, () -> open(CapeView.PROVIDER, option)), ENTRY_HEIGHT, 1)
                    .tooltip(Component.translatable(switch (option) {
                        case MOJANG -> "stynastmc.ui.cape.mojang.tooltip";
                        default -> "stynastmc.ui.cape.provider.tooltip";
                    }, option.title()));
        }
        Component custom = Component.translatable("stynastmc.ui.cape.custom");
        if (Capes.ownCustomFile() != null) {
            custom = Component.literal("✔ ").withColor(GREEN & 0xFFFFFF).append(custom.copy().withColor(0xFFFFFF));
        }
        capeList.place(new FolderEntry(custom, Component.empty(), () -> open(CapeView.CUSTOM, null)), ENTRY_HEIGHT, 1)
                .tooltip(Component.translatable("stynastmc.ui.cape.custom.tooltip"));
    }

    private void fillSkins() {
        skinList.clearChildren();
        Path dir = CustomSkins.directory();
        if (dir == null) {
            skinList.place(wrapped(Component.translatable("stynastmc.ui.skin.no_folder"), Ui.MUTED), 24, 0);
            return;
        }
        if (!Files.isDirectory(dir)) {
            skinList.place(wrapped(Component.translatable("stynastmc.ui.skin.folder_missing", dir.toString()), Ui.ERROR), 24, 0);
            return;
        }
        if (browsing == null || !browsing.startsWith(dir) || !Files.isDirectory(browsing)) {
            browsing = dir;
        }
        if (!browsing.equals(dir)) {
            Path parent = browsing.getParent();
            up(skinList, () -> browse(parent), CustomSkins.name(browsing));
        }
        List<Path> files = CustomSkins.list(browsing);
        if (files.isEmpty()) {
            skinList.place(wrapped(Component.translatable("stynastmc.ui.skin.folder_empty"), Ui.MUTED), 24, 0);
            return;
        }
        Path applied = CustomSkins.selected();
        for (Path file : files) {
            UiElement entry = Files.isDirectory(file)
                    ? new FolderEntry(Component.literal(file.getFileName() + "/"), Component.empty(), () -> browse(file))
                    : new SkinEntry(file, file.equals(applied));
            skinList.place(entry, ENTRY_HEIGHT, 1);
            entry.tooltip(Component.literal(file.toString()));
        }
        skinList.setScroll(skinScroll);
    }

    private void fillProvider() {
        up(capeList, () -> open(CapeView.ROOT, null), null);
        if (provider == CapeProvider.MOJANG) {
            List<MojangCapes.AccountCape> owned = MojangCapes.capes();
            if (MojangCapes.error() != null) {
                capeList.place(wrapped(Component.translatable("stynastmc.ui.cape.mojang.failed", MojangCapes.error()), Ui.ERROR), 24, 0);
            } else if (owned == null) {
                capeList.place(wrapped(Component.translatable("stynastmc.ui.cape.loading"), Ui.MUTED), 12, 0);
            } else {
                capeList.place(new CapeEntry(new CapeChoice(CapeProvider.MOJANG, null, null)), ENTRY_HEIGHT, 1);
                owned.forEach(account -> capeList.place(new CapeEntry(new CapeChoice(CapeProvider.MOJANG, account, null)), ENTRY_HEIGHT, 1));
                if (owned.isEmpty()) {
                    capeList.place(wrapped(Component.translatable("stynastmc.ui.cape.mojang.none"), Ui.MUTED), 24, 0);
                }
            }
            return;
        }
        GameProfile self = Minecraft.getInstance().getGameProfile();
        switch (Capes.state(self, provider)) {
            case LOADING -> capeList.place(wrapped(Component.translatable("stynastmc.ui.cape.loading"), Ui.MUTED), 12, 0);
            case NONE -> capeList.place(wrapped(Component.translatable("stynastmc.ui.cape.provider.none", provider.title()), Ui.MUTED), 24, 0);
            case FAILED -> capeList.place(wrapped(Component.translatable("stynastmc.ui.cape.provider.failed", Capes.error(self, provider)), Ui.ERROR), 24, 0);
            case LOADED -> capeList.place(new CapeEntry(new CapeChoice(provider, null, null)), ENTRY_HEIGHT, 1);
        }
    }

    private void fillCustom() {
        up(capeList, () -> open(CapeView.ROOT, null), null);
        Path dir = CustomCapes.directory();
        if (dir == null || !Files.isDirectory(dir)) {
            capeList.place(wrapped(Component.translatable("stynastmc.ui.skin.folder_missing", String.valueOf(dir)), Ui.ERROR), 24, 0);
            return;
        }
        List<Path> files = CustomCapes.list();
        if (files.isEmpty()) {
            capeList.place(wrapped(Component.translatable("stynastmc.ui.cape.folder_empty"), Ui.MUTED), 36, 0);
            return;
        }
        for (Path file : files) {
            capeList.place(new CapeEntry(CapeChoice.custom(file)), ENTRY_HEIGHT, 1).tooltip(Component.literal(file.toString()));
        }
    }

    private void up(UiPanel list, Runnable action, String tooltip) {
        FolderEntry up = list.place(new FolderEntry(Component.literal(".."), Component.empty(), action), ENTRY_HEIGHT, 1);
        if (tooltip != null) {
            up.tooltip(Component.literal(tooltip));
        }
    }

    private void browse(Path folder) {
        focus(false);
        browsing = folder;
        skinScroll = 0;
        fillSkins();
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
        if (capesFocused) {
            capeInfo(info);
        } else {
            skinInfo(info);
        }
        info.place(new UiWidgets.Label(() -> status, Ui.WHITE), 12, Ui.GAP);
    }

    private void skinInfo(UiPanel info) {
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

        actions(info, Component.translatable("stynastmc.ui.skin.apply"), this::apply, highlighted != null,
                Component.translatable("stynastmc.ui.skin.use_account"), this::useAccount, CustomSkins.enabled());
    }

    private void capeInfo(UiPanel info) {
        info.place(new UiWidgets.InfoRow(Component.translatable("stynastmc.ui.cape.name"),
                () -> cape == null ? Component.literal("-") : capeName(cape)), 16, 2);
        info.place(new UiWidgets.InfoRow(Component.translatable("stynastmc.ui.cape.source"), () -> cape == null ? Component.literal("-")
                : cape.file() != null ? Component.translatable("stynastmc.ui.cape.custom.source")
                : Component.literal(cape.provider().title())), 16, Ui.GAP);
        info.place(new UiWidgets.InfoRow(Component.translatable("stynastmc.ui.skin.resolution"), () -> {
            CapeTexture texture = cape == null ? null : capeTexture(cape);
            if (texture == null) {
                return Component.literal("-");
            }
            String size = texture.width() + "x" + texture.height();
            return Component.literal(texture.frames().size() > 1 ? size + ", " + texture.frames().size() + " frames" : size);
        }), 20, Ui.GAP);

        actions(info, Component.translatable("stynastmc.ui.cape.apply"), this::applyCape, cape != null,
                Component.translatable("stynastmc.ui.cape.use_default"), this::useDefaultCape, !Capes.own().isEmpty());
    }

    private void actions(UiPanel info, Component applyLabel, Runnable apply, boolean canApply, Component resetLabel, Runnable reset, boolean canReset) {
        int y = info.reserve(20, Ui.GAP);
        UiWidgets.Button applyButton = info.add(new UiWidgets.Button(applyLabel, apply));
        applyButton.bounds(info.innerX(), y, 110, 20);
        applyButton.outline = GREEN;
        applyButton.enabled = canApply;
        UiWidgets.Button resetButton = info.add(new UiWidgets.Button(resetLabel, reset));
        resetButton.bounds(applyButton.endX() + Ui.GAP * 2, y, 110, 20);
        resetButton.enabled = canReset;
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
        setStatus(Component.translatable("stynastmc.ui.skin.applied", CustomSkins.name(file)), GREEN);
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
        CustomSkins.shareSelected().whenComplete((unused, error) -> Minecraft.getInstance().execute(() -> {
            if (error != null) {
                setStatus(Component.translatable("stynastmc.ui.skin.share_failed", SkinService.message(error)), Ui.ERROR);
            } else {
                setStatus(Component.translatable("stynastmc.ui.skin.shared", CustomSkins.name(file)), GREEN);
            }
        }));
    }

    private void applyCape() {
        CapeChoice choice = cape;
        if (choice == null) {
            return;
        }
        if (choice.file() != null) {
            if (CustomCapes.texture(choice.file()) == null) {
                setStatus(Component.translatable("stynastmc.ui.cape.failed", String.valueOf(CustomCapes.error(choice.file()))), Ui.ERROR);
            } else {
                Capes.setOwn(Capes.customValue(choice.file()));
                String name = choice.file().getFileName().toString();
                setStatus(Component.translatable("stynastmc.ui.cape.applied.custom", name), GREEN);
                if (SkinService.configured()) {
                    setStatus(Component.translatable("stynastmc.ui.cape.sharing"), Ui.MUTED);
                    RemoteCapes.share().whenComplete((unused, error) -> Minecraft.getInstance().execute(() -> setStatus(error == null
                            ? Component.translatable("stynastmc.ui.cape.shared", name)
                            : Component.translatable("stynastmc.ui.skin.share_failed", SkinService.message(error)), error == null ? GREEN : Ui.ERROR)));
                }
            }
            rebuild();
            return;
        }
        if (choice.provider() != CapeProvider.MOJANG) {
            Capes.setOwn(choice.provider().id());
            setStatus(Component.translatable("stynastmc.ui.cape.applied.provider", choice.provider().title()), GREEN);
            rebuild();
            return;
        }
        setStatus(Component.translatable("stynastmc.ui.cape.equipping"), Ui.MUTED);
        MojangCapes.equip(choice.account()).whenComplete((unused, error) -> {
            if (error != null) {
                setStatus(Component.translatable("stynastmc.ui.cape.equip_failed", SkinService.message(error)), Ui.ERROR);
            } else {
                Capes.setOwn(CapeProvider.MOJANG.id());
                setStatus(choice.account() == null ? Component.translatable("stynastmc.ui.cape.hidden")
                        : Component.translatable("stynastmc.ui.cape.equipped", choice.account().alias()), GREEN);
            }
            rebuild();
        });
    }

    private void useDefaultCape() {
        Capes.setOwn("");
        setStatus(Component.translatable("stynastmc.ui.cape.default_restored"), Ui.MUTED);
        rebuild();
    }

    private static Component capeName(CapeChoice choice) {
        if (choice.file() != null) {
            return Component.literal(choice.file().getFileName().toString());
        }
        if (choice.provider() != CapeProvider.MOJANG) {
            return Component.translatable("stynastmc.ui.cape.provider.yours", choice.provider().title());
        }
        return choice.account() == null ? Component.translatable("stynastmc.ui.cape.mojang.hide") : Component.literal(choice.account().alias());
    }

    private static CapeTexture capeTexture(CapeChoice choice) {
        if (choice.file() != null) {
            return CustomCapes.texture(choice.file());
        }
        if (choice.provider() != CapeProvider.MOJANG) {
            return Capes.texture(Minecraft.getInstance().getGameProfile(), choice.provider());
        }
        return choice.account() == null ? null : MojangCapes.texture(choice.account());
    }

    private static boolean applied(CapeChoice choice) {
        if (choice.file() != null) {
            return Capes.own().equals(Capes.customValue(choice.file()));
        }
        if (choice.provider() != CapeProvider.MOJANG) {
            return Capes.own().equals(choice.provider().id());
        }
        MojangCapes.AccountCape active = MojangCapes.active();
        return choice.account() == null ? active == null : choice.account().equals(active);
    }

    /** Changes when an async load the current list depends on finishes. */
    private String signature() {
        if (capeView != CapeView.PROVIDER) {
            return capeView.name();
        }
        if (provider == CapeProvider.MOJANG) {
            return MojangCapes.loading() + "|" + MojangCapes.error() + "|" + System.identityHashCode(MojangCapes.capes());
        }
        return provider.id() + "|" + Capes.state(Minecraft.getInstance().getGameProfile(), provider);
    }

    @Override
    public void tick() {
        if (capeList != null && lastSignature != null && !lastSignature.equals(signature())) {
            rebuild();
        }
    }

    private void setStatus(Component text, int color) {
        status = text.copy().withColor(color & 0xFFFFFF);
    }

    private void rebuild() {
        if (skinList != null) {
            skinScroll = skinList.scroll();
            capeScroll = capeList.scroll();
        }
        screen.rebuild();
    }

    @Override
    public void onLeave() {
        if (dirty) {
            SkinsConfig.save();
            dirty = false;
        }
        if (capesDirty) {
            CapesConfig.save();
            capesDirty = false;
        }
        if (skinList != null) {
            skinScroll = skinList.scroll();
            capeScroll = capeList.scroll();
        }
    }

    private Model.Simple model(PlayerModelType type) {
        if (wideModel == null) {
            wideModel = new Model.Simple(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), RenderTypes::entityTranslucent);
            slimModel = new Model.Simple(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), RenderTypes::entityTranslucent);
        }
        return type == PlayerModelType.SLIM ? slimModel : wideModel;
    }

    /** What others see on us right now, skin and cape. */
    private PlayerSkin ownSkin() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            return minecraft.player.getSkin();
        }
        GameProfile profile = minecraft.getGameProfile();
        if (accountSkin == null) {
            accountSkin = minecraft.getSkinManager().createLookup(profile, false);
        }
        return Capes.apply(profile, SkinsBridge.forPlayer(profile.id(), accountSkin.get()));
    }

    private class FolderEntry extends UiElement {
        private final Component name;
        private final Component tag;
        private final Runnable action;

        FolderEntry(Component name, Component tag, Runnable action) {
            this.name = name;
            this.tag = tag;
            this.action = action;
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            if (contains(mx, my)) {
                Ui.outline(g, x, y, width, height, Ui.FOCUS_OUTLINE);
            }
            int size = height - 4;
            Ui.sprite(g, Ui.FOLDER, x + 2, y + 2, size, size);
            Ui.textInBox(g, name, x + size + 6, y, width - size - 10, height, Ui.WHITE, false);
            Ui.textInBox(g, tag, x, y, width - 4, height, Ui.WHITE, true);
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            UiWidgets.playClick();
            action.run();
            return true;
        }
    }

    private abstract class Entry extends UiElement {
        abstract boolean highlighted();

        abstract void icon(GuiGraphicsExtractor g, int x, int y, int size);

        abstract Component name();

        abstract boolean applied();

        abstract boolean failed();

        abstract void select();

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            if (highlighted()) {
                Ui.fill(g, x, y, width, height, Ui.HOVER_FILL);
            }
            if (contains(mx, my)) {
                Ui.outline(g, x, y, width, height, Ui.FOCUS_OUTLINE);
            }
            int size = height - 4;
            icon(g, x + 2, y + 2, size);
            Component name = name();
            if (applied()) {
                name = Component.literal("✔ ").withColor(GREEN & 0xFFFFFF).append(name.copy().withColor(0xFFFFFF));
            }
            Ui.textInBox(g, name, x + size + 6, y, width - size - 10, height, failed() ? Ui.ERROR : Ui.WHITE, false);
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            UiWidgets.playClick();
            select();
            rebuild();
            return true;
        }
    }

    private class SkinEntry extends Entry {
        private final Path file;
        private final boolean applied;

        SkinEntry(Path file, boolean applied) {
            this.file = file;
            this.applied = applied;
        }

        @Override
        boolean highlighted() {
            return file.equals(highlighted);
        }

        @Override
        void icon(GuiGraphicsExtractor g, int x, int y, int size) {
            SkinImages.Loaded loaded = CustomSkins.texture(file);
            if (loaded == null) {
                emptyIcon(g, x, y, size, Ui.ERROR);
                return;
            }
            int unit = loaded.width() / 64;
            g.blit(RenderPipelines.GUI_TEXTURED, loaded.texture(), x, y, 8 * unit, 8 * unit, size, size, 8 * unit, 8 * unit,
                    loaded.width(), loaded.height());
            g.blit(RenderPipelines.GUI_TEXTURED, loaded.texture(), x, y, 40 * unit, 8 * unit, size, size, 8 * unit, 8 * unit,
                    loaded.width(), loaded.height());
        }

        @Override
        Component name() {
            return Component.literal(file.getFileName().toString());
        }

        @Override
        boolean applied() {
            return applied;
        }

        @Override
        boolean failed() {
            return CustomSkins.texture(file) == null;
        }

        @Override
        void select() {
            focus(false);
            highlighted = file;
        }
    }

    private class CapeEntry extends Entry {
        private final CapeChoice choice;

        CapeEntry(CapeChoice choice) {
            this.choice = choice;
        }

        @Override
        boolean highlighted() {
            return choice.equals(cape);
        }

        @Override
        void icon(GuiGraphicsExtractor g, int x, int y, int size) {
            CapeTexture texture = capeTexture(choice);
            if (texture == null) {
                emptyIcon(g, x, y, size, failed() ? Ui.ERROR : Ui.PANEL_OUTLINE);
                return;
            }
            // Front face of the cape, 10x16 at u=1 v=1 on the 64x32 layout.
            int unit = texture.width() / 64;
            int w = size * 10 / 16;
            g.blit(RenderPipelines.GUI_TEXTURED, texture.current(), x + (size - w) / 2, y, unit, unit, w, size, 10 * unit, 16 * unit,
                    texture.width(), texture.height());
        }

        @Override
        Component name() {
            return capeName(choice);
        }

        @Override
        boolean applied() {
            return SkinTab.applied(choice);
        }

        @Override
        boolean failed() {
            return choice.file() != null && CustomCapes.error(choice.file()) != null;
        }

        @Override
        void select() {
            focus(true);
            cape = choice;
        }
    }

    private static void emptyIcon(GuiGraphicsExtractor g, int x, int y, int size, int outline) {
        Ui.fill(g, x, y, size, size, Ui.CARD_BACKGROUND);
        Ui.outline(g, x, y, size, size, outline);
    }

    private class Preview extends UiElement {
        private double lastX;
        private double lastY;

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            Ui.fill(g, x, y, width, height, Ui.CARD_BACKGROUND);
            Ui.outline(g, x, y, width, height, Ui.PANEL_OUTLINE);
            PlayerSkin own = ownSkin();
            boolean capes = capesFocused;
            Identifier texture = own.body().texturePath();
            PlayerModelType type = own.model();
            SkinImages.Loaded loaded = null;
            if (!capes && highlighted != null) {
                loaded = CustomSkins.texture(highlighted);
                texture = loaded == null ? texture : loaded.texture();
                type = SkinsConfig.MODEL.get().resolve(accountModel());
            }
            Identifier capeTexture = own.cape() == null ? null : own.cape().texturePath();
            if (capes && cape != null) {
                CapeTexture highlightedCape = capeTexture(cape);
                capeTexture = highlightedCape == null ? null : highlightedCape.current();
            }
            Model.Simple model = model(type);
            CapePreview.attach(model, capeTexture);
            float scale = 0.97F * (height - 8) / 2.125F;
            g.skin(model, texture, scale, rotationX, rotationY, -1.0625F, x + 4, y + 4, endX() - 4, endY() - 4);
            if (!capes && loaded == null && highlighted != null) {
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

    /** Model type of the account skin, which AUTO follows. */
    private static PlayerModelType accountModel() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? PlayerModelType.WIDE : minecraft.player.getSkin().model();
    }
}
