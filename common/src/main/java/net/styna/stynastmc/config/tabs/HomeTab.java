package net.styna.stynastmc.config.tabs;

import com.mojang.blaze3d.Blaze3D;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.classicbar.config.ClassicBarConfig;
import net.styna.stynastmc.clearspam.config.ClearSpamConfig;
import net.styna.stynastmc.config.StynaConfigScreen;
import net.styna.stynastmc.fonts.FontRuntime;
import net.styna.stynastmc.fonts.FontSettings;
import net.styna.stynastmc.fonts.GoogleFonts;
import net.styna.stynastmc.muffler.Muffler;
import net.styna.stynastmc.platform.Platform;
import net.styna.stynastmc.skins.CustomSkins;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiPanel;
import net.styna.stynastmc.ui.UiWidgets;
import net.styna.stynastmc.freecam.config.ModConfig;

/**
 * Landing page modeled on Better Statistics Screen's home tab: mod card, quick access, overview, credits.
 * Icons are GUI sprites because item stacks cannot be built on the title screen, before item components are bound.
 */
public class HomeTab implements Tab {
    private final StynaConfigScreen screen;

    public HomeTab(StynaConfigScreen screen) {
        this.screen = screen;
    }

    @Override
    public Component title() {
        return Component.translatable("stynastmc.ui.home");
    }

    @Override
    public void build(UiElement area) {
        int leftWidth = (int) (area.width * 0.7) - 28;
        UiPanel home = area.add(new UiPanel()).bounds(area.x + 10, area.y + 10, leftWidth, area.height - 20);
        home.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 10);
        home.place(new ModCard(), 40, Ui.GAP);

        home.place(UiWidgets.group(Component.translatable("stynastmc.ui.quick_access")), 17, Ui.GAP);
        UiPanel quick = home.place(new UiPanel(), 40, Ui.GAP * 3);
        quick.style(Ui.QUICK_BACKGROUND, 0, 5);
        int qx = quick.x + 5;
        qx = quickButton(quick, qx, Identifier.withDefaultNamespace("hud/heart/full"), Component.translatable("stynastmc.configuration.classicbar"), 1);
        qx = quickButton(quick, qx, Identifier.withDefaultNamespace("icon/video_link"), Component.translatable("stynastmc.configuration.freecam"), 2);
        qx = quickButton(quick, qx, Identifier.withDefaultNamespace("icon/language"), Component.translatable("stynastmc.ui.fonts"), 3);
        qx = quickButton(quick, qx, Identifier.withDefaultNamespace("spectator/teleport_to_player"), Component.translatable("stynastmc.ui.skin"), 4);
        qx = quickButton(quick, qx, Identifier.withDefaultNamespace("icon/search"), Component.translatable("stynastmc.configuration.zoom"), 5);
        qx = quickButton(quick, qx, Identifier.withDefaultNamespace("icon/music_notes"), Component.translatable("stynastmc.ui.muffler"), StynaConfigScreen.MUFFLER_TAB);
        UiWidgets.Button folder = quick.add(new UiWidgets.Button(Component.empty(), () -> Blaze3D.openPath(GoogleFonts.DIR.getParent()))
                .icon(Ui.FOLDER));
        folder.bounds(qx, quick.y + 5, 30, 30);
        folder.tooltip(Component.translatable("stynastmc.ui.open_folder"));

        home.place(UiWidgets.group(Component.translatable("stynastmc.ui.overview")), 17, Ui.GAP);
        info(home, "stynastmc.configuration.classicbar", () -> onOff(ClassicBarConfig.SPEC.isLoaded() && ClassicBarConfig.ENABLED.get()));
        info(home, "stynastmc.ui.game_font", () -> {
            FontSettings applied = FontRuntime.appliedSettings();
            return applied == null ? Component.translatable("stynastmc.ui.vanilla_font")
                    : Component.literal(applied.family() + " " + GoogleFonts.variantName(applied.variant()));
        });
        info(home, "stynastmc.ui.freecam_flight", () -> Component.literal(ModConfig.get().getFlightMode().name()));
        info(home, "stynastmc.ui.clearspam", () -> onOff(ClearSpamConfig.SPEC.isLoaded() && ClearSpamConfig.ENABLED.get()));
        info(home, "stynastmc.ui.muffler", () -> Muffler.enabled
                ? Component.translatable("stynastmc.ui.muffler.count", Muffler.global.size(), Muffler.anchors.size()) : onOff(false));
        info(home, "stynastmc.ui.skin", () -> {
            java.nio.file.Path skin = CustomSkins.enabled() ? CustomSkins.selected() : null;
            return skin == null ? Component.translatable("stynastmc.ui.skin.account") : Component.literal(CustomSkins.name(skin));
        });

        UiPanel credits = area.add(new UiPanel()).bounds(home.endX() + 18, area.y + 10, area.endX() - home.endX() - 28, area.height - 20);
        credits.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 10);
        credits.place(UiWidgets.group(Component.translatable("stynastmc.ui.credits")), 17, Ui.GAP);
        credit(credits, Component.translatable("stynastmc.ui.credits.text"));
        credits.place(UiWidgets.group(Component.translatable("stynastmc.ui.includes")), 17, Ui.GAP);
        for (String line : new String[] {"Classic Bar by Tfarcenim", "ClearSpam by Ruukas", "Freecam by MinecraftFreecam",
                "Zoom inspired by WI Zoom by Wurst-Imperium", "Sound muffler inspired by Extreme Sound Muffler by leobeliik",
                "UI style from Better Statistics Screen by TheCSDev", "Fonts from Google Fonts"}) {
            credit(credits, Component.literal(line));
        }
    }

    private int quickButton(UiPanel quick, int x, Identifier icon, Component tooltip, int tab) {
        UiWidgets.Button button = quick.add(new UiWidgets.Button(Component.empty(), () -> screen.openTab(tab)).icon(icon));
        button.bounds(x, quick.y + 5, 30, 30);
        button.tooltip(tooltip);
        return button.endX() + Ui.GAP;
    }

    private static void info(UiPanel panel, String key, java.util.function.Supplier<Component> value) {
        panel.place(new UiWidgets.InfoRow(Component.translatable(key), value), 18, Ui.GAP);
    }

    private static void credit(UiPanel panel, Component text) {
        UiWidgets.Label label = new UiWidgets.Label(text, Ui.MUTED);
        label.wrap = true;
        panel.place(label, label.wrappedHeight(panel.width - 26), 4);
    }

    private static Component onOff(boolean on) {
        return Component.translatable(on ? "options.on" : "options.off");
    }

    private static class ModCard extends UiElement {
        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            Ui.fill(g, x, y, width, height, Ui.CARD_BACKGROUND);
            Ui.outline(g, x, y, width, height, Ui.CARD_OUTLINE);
            int size = height - 10;
            Ui.sprite(g, Ui.SETTINGS, x + 5, y + 5, size, size);
            String version = Platform.INSTANCE.modVersion();
            Ui.text(g, Component.literal("StynaST-MC"), x + size + 10, y + 7, Ui.GROUP_LABEL, 0.9F);
            Ui.text(g, Component.literal("v" + version), x + size + 10, y + 20, Ui.WHITE, 0.75F);
        }
    }
}
