package net.styna.stynastmc.tweaks;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.ui.Ui;
import net.styna.stynastmc.ui.UiElement;
import net.styna.stynastmc.ui.UiPanel;
import net.styna.stynastmc.ui.UiScreen;
import net.styna.stynastmc.ui.UiWidgets;

/**
 * Picks which librarian books stop the auto cycle, and the lowest level that counts.
 * Drawn from sprites only, so resource packs can restyle it.
 */
public class BookTargetScreen extends UiScreen {
    private static final Identifier WINDOW = StynaSTMC.id("trade/window");
    private static final Identifier LIST = StynaSTMC.id("trade/list");
    private static final Identifier ROW_SELECTED = StynaSTMC.id("trade/row_selected");
    private static final Identifier ROW = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier ROW_HOVER = Identifier.withDefaultNamespace("widget/button_highlighted");
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFD0D0D0;

    private final Map<String, Integer> targets = TradeCycler.targets();
    private String query = "";
    private UiPanel panel;
    private int wx;
    private int wy;
    private int ww;
    private int wh;

    public BookTargetScreen(Screen parent) {
        super(Component.translatable("stynastmc.trade.targets"), parent);
    }

    @Override
    protected void build() {
        ww = Math.min(300, width - 20);
        wh = Math.min(260, height - 20);
        wx = (width - ww) / 2;
        wy = (height - wh) / 2;
        root.add(new UiWidgets.Label(title, TEXT)).bounds(wx + 8, wy + 6, ww - 16, 12);

        EditBox search = addRenderableWidget(new EditBox(font, wx + 8, wy + 22, ww - 80, 20, Component.translatable("stynastmc.stats.search")));
        search.setHint(Component.translatable("stynastmc.stats.search"));
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            fill();
        });
        addRenderableWidget(Button.builder(Component.translatable("stynastmc.trade.clear"), b -> {
            targets.clear();
            TradeCycler.setTargets(targets);
        }).bounds(wx + ww - 68, wy + 22, 60, 20).build());

        int listY = wy + 48;
        int listH = wh - 48 - 58;
        panel = root.add(new UiPanel()).bounds(wx + 8, listY, ww - 16, listH);
        panel.style(0, 0, 4);
        fill();

        UiWidgets.Label hint = root.add(new UiWidgets.Label(Component.translatable("stynastmc.trade.hint"), TEXT_DIM));
        hint.bounds(wx + 8, listY + listH + 4, ww - 16, 22);
        hint.wrap = true;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(wx + ww / 2 - 50, wy + wh - 26, 100, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        Ui.sprite(g, WINDOW, wx, wy, ww, wh);
        Ui.sprite(g, LIST, panel.x, panel.y, panel.width, panel.height);
    }

    private void fill() {
        panel.clearChildren();
        for (Holder<Enchantment> enchantment : enchantments()) {
            if (query.isBlank() || enchantment.value().description().getString().toLowerCase(Locale.ROOT)
                    .contains(query.trim().toLowerCase(Locale.ROOT))) {
                panel.place(new TargetRow(enchantment), 20, 1);
            }
        }
    }

    private List<Holder<Enchantment>> enchantments() {
        if (minecraft.getConnection() == null) {
            return List.of();
        }
        Registry<Enchantment> registry = minecraft.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<Holder<Enchantment>> list = registry.get(EnchantmentTags.TRADEABLE)
                .<List<Holder<Enchantment>>>map(set -> set.stream().toList())
                .filter(set -> !set.isEmpty())
                .orElseGet(() -> registry.listElements().<Holder<Enchantment>>map(h -> h).toList());
        return list.stream().sorted(Comparator.comparing(h -> h.value().description().getString())).toList();
    }

    private class TargetRow extends UiElement {
        private final Holder<Enchantment> enchantment;
        private final String id;

        TargetRow(Holder<Enchantment> enchantment) {
            this.enchantment = enchantment;
            this.id = enchantment.unwrapKey().map(key -> key.identifier().toString()).orElse("");
        }

        @Override
        protected void renderSelf(GuiGraphicsExtractor g, int mx, int my, float delta) {
            Integer level = targets.get(id);
            boolean hot = contains(mx, my);
            Ui.sprite(g, hot ? ROW_HOVER : level != null ? ROW_SELECTED : ROW, x, y, width, height);
            Ui.textInBox(g, enchantment.value().description(), x + 6, y, width - 12, height, TEXT, false);
            Component value = level == null
                    ? Component.translatable("options.off")
                    : Component.translatable("stynastmc.trade.min_level", Enchantment.getFullname(enchantment, level).copy().withColor(TEXT));
            Ui.textInBox(g, value, x + 6, y, width - 12, height, level == null ? TEXT_DIM : TEXT, true);
        }

        @Override
        protected boolean onClick(double mx, double my, int button) {
            int max = enchantment.value().getMaxLevel();
            int level = targets.getOrDefault(id, 0);
            level = button == InputConstants.MOUSE_BUTTON_RIGHT ? (level + 1) % (max + 1) : level == 0 ? max : level - 1;
            if (level == 0) {
                targets.remove(id);
            } else {
                targets.put(id, level);
            }
            TradeCycler.setTargets(targets);
            UiWidgets.playClick();
            return true;
        }
    }
}
