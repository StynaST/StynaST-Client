package net.styna.stynastmc.classicbar.impl.overlays.templates;

import com.mojang.serialization.Codec;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.styna.stynastmc.classicbar.api.BarSettings;
import net.styna.stynastmc.classicbar.api.Color;
import net.styna.stynastmc.classicbar.impl.BarInfo;

public class StackingBarOverlay extends BarOverlayImpl {
    private final Codec<? extends StackingBarOverlay> codec;

    public StackingBarOverlay(BarInfo info, BarSettings barSettings, Codec<? extends StackingBarOverlay> codec) {
        super(info, barSettings);
        this.codec = codec;
    }

    @Override
    public Codec<? extends BarOverlayImpl> codec() {
        return codec;
    }

    @Override
    public void renderBar(GuiGraphicsExtractor graphics, Player player, int screenWidth, int screenHeight, int vOffset) {
        double value = barInfo.numerator().getValue(player);
        int barWidth = getBarWidth(player);
        int xStart = getXStartBar(screenWidth, barWidth);
        int yStart = screenHeight - vOffset;
        int index = getStackCount(player);
        Color primary = getBarColor(player, index);
        renderBarBackground(graphics, player, screenWidth, screenHeight, vOffset);
        if (index == 0) {
            renderPartialBar(primary, graphics, xStart + 2, yStart + 2, barWidth);
        } else if (value % 20.0 != 0.0) {
            renderFullBar(getBarColor(player, index - 1), graphics, xStart + 2, yStart + 2);
            int w = getWidth(value % 20.0, 20.0);
            renderPartialBar(primary, graphics, getXStartBar(screenWidth, w) + 2, yStart + 2, w);
        } else {
            renderFullBar(primary, graphics, xStart + 2, yStart + 2);
        }
    }

    public int getStackCount(Player player) {
        float ratio = barInfo.getUnclampedRatio(player);
        return ratio > 1.0F ? (int) (Math.ceil(ratio) - 1.0) : 0;
    }

    public Color getBarColor(Player player, int index) {
        return getBarSettings().colorProvider().getColor(player, barInfo.getUnclampedRatio(player), index);
    }

    @Override
    public void renderText(GuiGraphicsExtractor graphics, Player player, int width, int height, int vOffset) {
        int xStart = width / 2 + getIconOffset();
        int yStart = height - vOffset;
        textHelper(graphics, xStart, yStart, barInfo.numerator().getValue(player), getBarColor(player, getStackCount(player)).argb());
    }
}
