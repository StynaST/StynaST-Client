package net.styna.stynastmc.config.tabs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.lwjgl.Sys;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.styna.stynastmc.capes.CapeProvider;
import net.styna.stynastmc.capes.CapeTexture;
import net.styna.stynastmc.capes.Capes;
import net.styna.stynastmc.capes.CapesConfig;
import net.styna.stynastmc.capes.CustomCapes;
import net.styna.stynastmc.capes.MojangCapes;
import net.styna.stynastmc.config.StynaConfigScreen;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiPanel;
import net.styna.stynastmc.ui.UiWidgets;
import net.styna.stynastmc.util.Text;

/** Picks the cape on your own player. Only you see it. */
public class CapeTab implements Tab {
    private static final int ENTRY_HEIGHT = 20;
    private static final int INFO_HEIGHT = 92;

    private enum View {
        ROOT, PROVIDER, CUSTOM
    }

    /** Something that can be worn: value is what {@link Capes#setOwn} stores. */
    private static final class Choice {
        final String value;
        final String name;
        final String source;
        final Supplier<CapeTexture> texture;
        final Supplier<String> error;

        Choice(String value, String name, String source, Supplier<CapeTexture> texture, Supplier<String> error) {
            this.value = value;
            this.name = name;
            this.source = source;
            this.texture = texture;
            this.error = error;
        }
    }

    private final StynaConfigScreen screen;
    private View view = View.ROOT;
    private CapeProvider provider;
    private Choice highlighted;
    private String status = "";
    private String listState = "";
    private double listScroll;
    private boolean dirty;
    private UiPanel list;
    private ModelPlayer wideModel;
    private ModelPlayer slimModel;
    private float rotationX = -5.0F;
    private float rotationY = 210.0F;

    public CapeTab(StynaConfigScreen screen) {
        this.screen = screen;
    }

    @Override
    public String title() {
        return Text.tr("stynastmc.ui.cape");
    }

    @Override
    public void build(UiElement area) {
        if (!CapesConfig.loaded()) {
            area.add(new UiWidgets.Label(Text.tr("stynastmc.ui.config_not_loaded"), Ui.ERROR))
                    .bounds(area.x + 10, area.y + 10, area.width - 20, 12);
            return;
        }
        int leftWidth = Math.max(160, (int) (area.width * 0.38));
        int left = area.x + 10;
        UiWidgets.Label header = area.add(UiWidgets.group(headerTitle()));
        header.bounds(left, area.y + 8, leftWidth, 12);
        int top = header.endY() + 2;
        if (view == View.CUSTOM) {
            top = folderRow(area, left, top, leftWidth);
        } else if (view == View.PROVIDER && provider != CapeProvider.MOJANG) {
            UiWidgets.Button site = area.add(new UiWidgets.Button(Text.tr("stynastmc.ui.cape.website"), () -> Sys.openURL(provider.homepage())));
            site.bounds(left, top, 70, 16);
            top = site.endY() + Ui.GAP;
        }
        list = area.add(new UiPanel()).bounds(left, top, leftWidth, area.endY() - top - 10);
        list.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 4);
        fillList();
        buildDetail(area, list.endX() + 10, area.endX() - list.endX() - 20);
    }

    private String headerTitle() {
        switch (view) {
            case PROVIDER:
                return Text.tr("stynastmc.ui.cape.provider_title", provider.title());
            case CUSTOM:
                return Text.tr("stynastmc.ui.cape.folder");
            default:
                return Text.tr("stynastmc.ui.cape.capes");
        }
    }

    private int folderRow(UiElement area, int left, int y, int width) {
        UiWidgets.TextField folder = area.add(new UiWidgets.TextField(CapesConfig.DIRECTORY.get()));
        folder.bounds(left, y, width - 38, 16);
        folder.placeholder = CustomCapes.defaultDirectory().toString();
        folder.onChange = value -> {
            CapesConfig.DIRECTORY.set(value.trim());
            dirty = true;
            highlighted = null;
            CustomCapes.refresh();
            fillList();
        };
        UiWidgets.Button open = area.add(new UiWidgets.Button("", () -> {
            Path dir = CustomCapes.directory();
            if (dir != null && Files.isDirectory(dir)) {
                OpenGlHelper.openFile(dir.toFile());
            }
        }).icon(Ui.FOLDER));
        open.bounds(folder.endX() + Ui.GAP, y, 16, 16);
        open.tooltip(Text.tr("stynastmc.ui.skin.open_folder"));
        UiWidgets.Button rescan = area.add(new UiWidgets.Button("⟳", () -> {
            CustomCapes.refresh();
            rebuild();
        }));
        rescan.bounds(open.endX() + Ui.GAP, y, 16, 16);
        rescan.tooltip(Text.tr("stynastmc.ui.skin.rescan"));
        return folder.endY() + Ui.GAP;
    }

    private void fillList() {
        list.clearChildren();
        listState = state();
        switch (view) {
            case ROOT -> fillRoot();
            case PROVIDER -> fillProvider();
            case CUSTOM -> fillCustom();
        }
        list.setScroll(listScroll);
    }

    private void fillRoot() {
        list.place(new CapeEntry(new Choice("", Text.tr("stynastmc.ui.cape.server"), Text.tr("stynastmc.ui.cape.server.source"),
                () -> null, () -> null)), ENTRY_HEIGHT, 1).tooltip(Text.tr("stynastmc.ui.cape.server.tooltip"));
        list.place(new CapeEntry(new Choice(Capes.NONE, Text.tr("stynastmc.ui.cape.none"), "-", () -> null, () -> null)), ENTRY_HEIGHT, 1);
        String own = Capes.own();
        for (CapeProvider option : CapeProvider.values()) {
            boolean wearing = option == CapeProvider.MOJANG ? Capes.ownMojangId() != null || own.equals(option.id()) : own.equals(option.id());
            list.place(new FolderEntry(option.title(), wearing, () -> open(View.PROVIDER, option)), ENTRY_HEIGHT, 1)
                    .tooltip(Text.tr(option == CapeProvider.MOJANG ? "stynastmc.ui.cape.mojang.tooltip" : "stynastmc.ui.cape.provider.tooltip", option.title()));
        }
        list.place(new FolderEntry(Text.tr("stynastmc.ui.cape.custom"), Capes.ownCustomFile() != null, () -> open(View.CUSTOM, null)), ENTRY_HEIGHT, 1)
                .tooltip(Text.tr("stynastmc.ui.cape.custom.tooltip"));
    }

    private void fillProvider() {
        list.place(new FolderEntry("..", false, () -> open(View.ROOT, null)), ENTRY_HEIGHT, 1);
        if (provider == CapeProvider.MOJANG) {
            List<MojangCapes.AccountCape> owned = MojangCapes.capes();
            if (MojangCapes.error() != null) {
                list.place(wrapped(Text.tr("stynastmc.ui.cape.mojang.failed", MojangCapes.error()), Ui.ERROR), 24, 0);
            } else if (owned == null) {
                list.place(wrapped(Text.tr("stynastmc.ui.cape.loading"), Ui.MUTED), 12, 0);
            } else if (owned.isEmpty()) {
                list.place(wrapped(Text.tr("stynastmc.ui.cape.mojang.none"), Ui.MUTED), 24, 0);
            } else {
                for (MojangCapes.AccountCape cape : owned) {
                    list.place(new CapeEntry(new Choice(Capes.mojangValue(cape), cape.alias, provider.title(),
                            () -> MojangCapes.texture(cape), () -> MojangCapes.textureError(cape))), ENTRY_HEIGHT, 1);
                }
            }
            return;
        }
        CapeProvider current = provider;
        switch (Capes.state(current)) {
            case LOADING -> list.place(wrapped(Text.tr("stynastmc.ui.cape.loading"), Ui.MUTED), 12, 0);
            case NONE -> list.place(wrapped(Text.tr("stynastmc.ui.cape.provider.none", current.title()), Ui.MUTED), 24, 0);
            case FAILED -> list.place(wrapped(Text.tr("stynastmc.ui.cape.provider.failed", Capes.error(current)), Ui.ERROR), 24, 0);
            case LOADED -> list.place(new CapeEntry(new Choice(current.id(), Text.tr("stynastmc.ui.cape.provider.yours", current.title()),
                    current.title(), () -> Capes.texture(current), () -> Capes.error(current))), ENTRY_HEIGHT, 1);
        }
    }

    private void fillCustom() {
        list.place(new FolderEntry("..", false, () -> open(View.ROOT, null)), ENTRY_HEIGHT, 1);
        List<Path> files = CustomCapes.list();
        if (files.isEmpty()) {
            list.place(wrapped(Text.tr("stynastmc.ui.cape.folder_empty"), Ui.MUTED), 36, 0);
            return;
        }
        for (Path file : files) {
            list.place(new CapeEntry(new Choice(Capes.customValue(file), file.getFileName().toString(), Text.tr("stynastmc.ui.cape.custom"),
                    () -> CustomCapes.texture(file), () -> CustomCapes.error(file))), ENTRY_HEIGHT, 1).tooltip(file.toString());
        }
    }

    /** Changes when an async lookup finishes, so the list redraws with the result. */
    private String state() {
        if (view == View.PROVIDER && provider == CapeProvider.MOJANG) {
            List<MojangCapes.AccountCape> owned = MojangCapes.capes();
            return MojangCapes.error() + "/" + (owned == null ? -1 : owned.size());
        }
        return view == View.PROVIDER ? Capes.state(provider).name() : "";
    }

    private void open(View next, CapeProvider nextProvider) {
        if (next == View.PROVIDER && nextProvider == CapeProvider.MOJANG && MojangCapes.error() != null) {
            MojangCapes.refresh();
        }
        view = next;
        provider = nextProvider;
        listScroll = 0;
        rebuild();
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
        info.place(new UiWidgets.InfoRow(Text.tr("stynastmc.ui.cape.name"), () -> highlighted == null ? "-" : highlighted.name), 16, 2);
        info.place(new UiWidgets.InfoRow(Text.tr("stynastmc.ui.cape.source"), () -> highlighted == null ? "-" : highlighted.source), 16, Ui.GAP);

        int actions = info.reserve(20, Ui.GAP);
        UiWidgets.Button apply = info.add(new UiWidgets.Button(Text.tr("stynastmc.ui.cape.apply"), this::apply));
        apply.bounds(info.innerX(), actions, 110, 20);
        apply.outline = Ui.SUCCESS;
        apply.enabled = highlighted != null;
        UiWidgets.Button reset = info.add(new UiWidgets.Button(Text.tr("stynastmc.ui.cape.use_default"), () -> {
            Capes.setOwn("");
            setStatus(Text.tr("stynastmc.ui.cape.default_restored"), Ui.MUTED);
            rebuild();
        }));
        reset.bounds(apply.endX() + Ui.GAP * 2, actions, 110, 20);
        reset.enabled = !Capes.own().isEmpty();

        info.place(new UiWidgets.Label(() -> status, Ui.WHITE), 12, Ui.GAP);
    }

    private void apply() {
        if (highlighted == null) {
            return;
        }
        boolean wearable = highlighted.value.isEmpty() || highlighted.value.equals(Capes.NONE);
        if (!wearable && highlighted.texture.get() == null) {
            String error = highlighted.error.get();
            setStatus(error == null ? Text.tr("stynastmc.ui.cape.loading") : Text.tr("stynastmc.ui.cape.failed", error), Ui.ERROR);
            return;
        }
        Capes.setOwn(highlighted.value);
        setStatus(Text.tr("stynastmc.ui.cape.applied", highlighted.name), Ui.SUCCESS);
        rebuild();
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
    public void tick() {
        if (list != null && !state().equals(listState)) {
            listScroll = list.scroll();
            fillList();
        }
    }

    @Override
    public void onLeave() {
        if (dirty) {
            CapesConfig.save();
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

    private static final class FolderEntry extends UiElement {
        private final String name;
        private final boolean wearing;
        private final Runnable onOpen;

        FolderEntry(String name, boolean wearing, Runnable onOpen) {
            this.name = name;
            this.wearing = wearing;
            this.onOpen = onOpen;
        }

        @Override
        protected void renderSelf(int mx, int my, float delta) {
            if (contains(mx, my)) {
                Ui.outline(x, y, width, height, Ui.FOCUS_OUTLINE);
            }
            int size = height - 4;
            Ui.sprite(Ui.FOLDER, x + 2, y + 2, size, size);
            Ui.textInBox((wearing ? "§a✔ §f" : "") + name, x + size + 6, y, width - size - 10, height, Ui.WHITE, false);
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            UiWidgets.playClick();
            onOpen.run();
            return true;
        }
    }

    private class CapeEntry extends UiElement {
        private final Choice choice;

        CapeEntry(Choice choice) {
            this.choice = choice;
        }

        @Override
        protected void renderSelf(int mx, int my, float delta) {
            if (highlighted != null && highlighted.value.equals(choice.value)) {
                Ui.fill(x, y, width, height, Ui.HOVER_FILL);
            }
            if (contains(mx, my)) {
                Ui.outline(x, y, width, height, Ui.FOCUS_OUTLINE);
            }
            int size = height - 4;
            CapeTexture texture = choice.texture.get();
            boolean broken = texture == null && choice.error.get() != null;
            if (texture != null) {
                front(texture.current(), texture.width, texture.height, x + 2 + size / 4, y + 2, size / 2, size);
            } else {
                Ui.fill(x + 2 + size / 4, y + 2, size / 2, size, Ui.CARD_BACKGROUND);
                Ui.outline(x + 2 + size / 4, y + 2, size / 2, size, broken ? Ui.ERROR : Ui.ROW_OUTLINE);
            }
            String name = choice.value.equals(Capes.own()) ? "§a✔ §f" + choice.name : choice.name;
            Ui.textInBox(name, x + size + 6, y, width - size - 10, height, broken ? Ui.ERROR : Ui.WHITE, false);
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            UiWidgets.playClick();
            highlighted = choice;
            rebuild();
            return true;
        }
    }

    /** The cape's outer face, 10x16 units at (1, 1). */
    private static void front(ResourceLocation texture, int width, int height, int x, int y, int w, int h) {
        int unit = width / 64;
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, 1);
        Gui.drawScaledCustomSizeModalRect(x, y, unit, unit, 10 * unit, 16 * unit, w, h, width, height);
    }

    private class Preview extends UiElement {
        private double lastX;
        private double lastY;

        @Override
        protected void renderSelf(int mx, int my, float delta) {
            Ui.fill(x, y, width, height, Ui.CARD_BACKGROUND);
            Ui.outline(x, y, width, height, Ui.PANEL_OUTLINE);
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            ResourceLocation skin = player == null ? DefaultPlayerSkin.getDefaultSkinLegacy() : player.getLocationSkin();
            boolean slim = player != null && "slim".equals(player.getSkinType());
            ResourceLocation cape;
            if (highlighted != null) {
                CapeTexture texture = highlighted.texture.get();
                cape = texture != null ? texture.current() : highlighted.value.isEmpty() && Capes.own().isEmpty() && player != null ? player.getLocationCape() : null;
            } else {
                cape = player == null ? null : player.getLocationCape();
            }
            Ui.enableScissor(x + 1, y + 1, endX() - 1, endY() - 1);
            drawPlayer(model(slim), skin, cape, x + width / 2.0F, y + height / 2.0F, 0.97F * (height - 8) / 2.125F);
            Ui.disableScissor();
        }

        private void drawPlayer(ModelPlayer model, ResourceLocation skin, ResourceLocation cape, float cx, float cy, float scale) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(skin);
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
            if (cape != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(cape);
                GlStateManager.pushMatrix();
                // Vanilla's resting cape lean from LayerCape with no movement.
                GlStateManager.translate(0.0F, 0.0F, 0.125F);
                GlStateManager.rotate(6.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                model.renderCape(unit);
                GlStateManager.popMatrix();
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
