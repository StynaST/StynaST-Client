package net.styna.stynastmc.config.tabs;

import java.nio.file.Path;
import java.util.function.Supplier;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.clearspam.config.ClearSpamConfig;
import net.styna.stynastmc.config.StynaConfigScreen;
import net.styna.stynastmc.fonts.FontRuntime;
import net.styna.stynastmc.fonts.FontSettings;
import net.styna.stynastmc.fonts.GoogleFonts;
import net.styna.stynastmc.freecam.config.FreecamConfig;
import net.styna.stynastmc.muffler.Muffler;
import net.styna.stynastmc.skins.CustomSkins;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiPanel;
import net.styna.stynastmc.ui.UiWidgets;
import net.styna.stynastmc.util.Text;

/** Landing page modeled on Better Statistics Screen's home tab: mod card, quick access, overview, credits. */
public class HomeTab implements Tab {
    private final StynaConfigScreen screen;

    public HomeTab(StynaConfigScreen screen) {
        this.screen = screen;
    }

    @Override
    public String title() {
        return Text.tr("stynastmc.ui.home");
    }

    @Override
    public void build(UiElement area) {
        int leftWidth = (int) (area.width * 0.7) - 28;
        UiPanel home = area.add(new UiPanel()).bounds(area.x + 10, area.y + 10, leftWidth, area.height - 20);
        home.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 10);
        home.place(new ModCard(), 40, Ui.GAP);

        home.place(UiWidgets.group(Text.tr("stynastmc.ui.quick_access")), 17, Ui.GAP);
        UiPanel quick = home.place(new UiPanel(), 40, Ui.GAP * 3);
        quick.style(Ui.QUICK_BACKGROUND, 0, 5);
        int qx = quick.x + 5;
        qx = quickButton(quick, qx, new ItemStack(Items.ENDER_EYE), Text.tr("stynastmc.configuration.freecam"), StynaConfigScreen.FREECAM_TAB);
        qx = quickButton(quick, qx, new ItemStack(Items.WRITABLE_BOOK), Text.tr("stynastmc.ui.fonts"), StynaConfigScreen.FONTS_TAB);
        qx = quickButton(quick, qx, new ItemStack(Items.SKULL, 1, 3), Text.tr("stynastmc.ui.skin"), StynaConfigScreen.SKIN_TAB);
        qx = quickButton(quick, qx, new ItemStack(Items.COMPASS), Text.tr("stynastmc.configuration.zoom"), StynaConfigScreen.ZOOM_TAB);
        qx = quickButton(quick, qx, new ItemStack(Items.RECORD_CAT), Text.tr("stynastmc.ui.muffler"), StynaConfigScreen.MUFFLER_TAB);
        UiWidgets.Button folder = quick.add(new UiWidgets.Button("", () -> OpenGlHelper.openFile(StynaSTMC.modConfigDir().toFile()))
                .icon(Ui.FOLDER));
        folder.bounds(qx, quick.y + 5, 30, 30);
        folder.tooltip(Text.tr("stynastmc.ui.open_folder"));

        home.place(UiWidgets.group(Text.tr("stynastmc.ui.overview")), 17, Ui.GAP);
        info(home, "stynastmc.ui.game_font", () -> {
            FontSettings applied = FontRuntime.appliedSettings();
            return applied == null ? Text.tr("stynastmc.ui.vanilla_font") : applied.family + " " + GoogleFonts.variantName(applied.variant);
        });
        info(home, "stynastmc.ui.freecam_flight", () -> FreecamConfig.FLIGHT_MODE.get().name());
        info(home, "stynastmc.ui.clearspam", () -> onOff(ClearSpamConfig.SPEC.isLoaded() && ClearSpamConfig.ENABLED.get()));
        info(home, "stynastmc.ui.muffler", () -> Muffler.enabled
                ? Text.tr("stynastmc.ui.muffler.count", Muffler.global.size(), Muffler.anchors.size()) : onOff(false));
        info(home, "stynastmc.ui.skin", () -> {
            Path skin = CustomSkins.enabled() ? CustomSkins.selected() : null;
            return skin == null ? Text.tr("stynastmc.ui.skin.account") : CustomSkins.name(skin);
        });

        UiPanel credits = area.add(new UiPanel()).bounds(home.endX() + 18, area.y + 10, area.endX() - home.endX() - 28, area.height - 20);
        credits.style(Ui.PANEL_BACKGROUND, Ui.BLACK, 10);
        credits.place(UiWidgets.group(Text.tr("stynastmc.ui.credits")), 17, Ui.GAP);
        credit(credits, Text.tr("stynastmc.ui.credits.text"));
        credits.place(UiWidgets.group(Text.tr("stynastmc.ui.includes")), 17, Ui.GAP);
        for (String line : new String[] {"ClearSpam by Ruukas", "Freecam by MinecraftFreecam",
                "Zoom inspired by WI Zoom by Wurst-Imperium", "Sound muffler inspired by Extreme Sound Muffler by leobeliik",
                "UI style from Better Statistics Screen by TheCSDev", "Fonts from Google Fonts",
                "Container preview from ShulkerBoxTooltip by MisterPeModder", "Enchantment descriptions by Darkhax",
                "First person body after FirstPersonModel by tr7zw"}) {
            credit(credits, line);
        }
    }

    private int quickButton(UiPanel quick, int x, ItemStack icon, String tooltip, int tab) {
        UiWidgets.Button button = quick.add(new UiWidgets.Button("", () -> screen.openTab(tab)).item(icon));
        button.bounds(x, quick.y + 5, 30, 30);
        button.tooltip(tooltip);
        return button.endX() + Ui.GAP;
    }

    private static void info(UiPanel panel, String key, Supplier<String> value) {
        panel.place(new UiWidgets.InfoRow(Text.tr(key), value), 18, Ui.GAP);
    }

    private static void credit(UiPanel panel, String text) {
        UiWidgets.Label label = new UiWidgets.Label(text, Ui.MUTED);
        label.wrap = true;
        panel.place(label, label.wrappedHeight(panel.width - 26), 4);
    }

    private static String onOff(boolean on) {
        return Text.tr(on ? "options.on" : "options.off");
    }

    private static class ModCard extends UiElement {
        @Override
        protected void renderSelf(int mx, int my, float delta) {
            Ui.fill(x, y, width, height, Ui.CARD_BACKGROUND);
            Ui.outline(x, y, width, height, Ui.CARD_OUTLINE);
            int size = height - 10;
            Ui.sprite(Ui.SETTINGS, x + 5, y + 5, size, size);
            Ui.text("StynaST-MC", x + size + 10, y + 7, Ui.GROUP_LABEL, 0.9F);
            Ui.text("v" + StynaSTMC.version(), x + size + 10, y + 20, Ui.WHITE, 0.75F);
        }
    }
}
