package net.styna.stynastmc.classicbar.impl.overlays.templates;

import java.util.Optional;

import com.mojang.datafixers.Products;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.classicbar.api.BarOverlay;
import net.styna.stynastmc.classicbar.api.BarSettings;
import net.styna.stynastmc.classicbar.api.BarSide;
import net.styna.stynastmc.classicbar.api.Color;
import net.styna.stynastmc.classicbar.impl.BarInfo;
import net.styna.stynastmc.classicbar.util.HealthEffect;
import net.styna.stynastmc.classicbar.util.ModUtils;

public abstract class BarOverlayImpl implements BarOverlay {
    public static final int WIDTH = 77;
    public static final int HEIGHT = 5;
    public static final Identifier BAR = StynaSTMC.id("textures/gui/classicbar/health.png");

    private final BarSettings barSettings;
    protected final BarInfo barInfo;
    protected boolean errored;

    protected static <T extends BarOverlayImpl> Products.P1<RecordCodecBuilder.Mu<T>, BarSettings> codecStart(RecordCodecBuilder.Instance<T> instance) {
        return instance.group(BarSettings.CODEC.fieldOf("bar_settings").forGetter(BarOverlayImpl::getBarSettings));
    }

    protected BarOverlayImpl(BarInfo barInfo, BarSettings barSettings) {
        this.barInfo = barInfo;
        this.barSettings = barSettings;
    }

    public static int getWidth(double d1, double d2) {
        return (int) Math.ceil(WIDTH * d1 / d2);
    }

    public BarSettings getBarSettings() {
        return barSettings;
    }

    public final boolean shouldRender(Player player) {
        return !errored && barSettings.enabled() && barInfo.shouldRender().test(player);
    }

    @Override
    public final boolean enabled() {
        return barSettings.enabled();
    }

    @Override
    public final BarSide getSide() {
        return barSettings.side();
    }

    @Override
    public boolean render(GuiGraphicsExtractor graphics, Player player, int screenWidth, int screenHeight, int vOffset) {
        if (!shouldRender(player)) {
            return false;
        }

        renderBar(graphics, player, screenWidth, screenHeight, vOffset);
        renderBarDecorations(graphics, player, screenWidth, screenHeight, vOffset);
        if (barSettings.show_text()) {
            renderText(graphics, player, screenWidth, screenHeight, vOffset);
        }
        if (barSettings.show_icon()) {
            renderIcon(graphics, player, screenWidth, screenHeight, vOffset);
        }
        return true;
    }

    public void renderBar(GuiGraphicsExtractor graphics, Player player, int screenWidth, int screenHeight, int vOffset) {
        renderSimpleBar(barSettings.colorProvider().getColor(player, barInfo.getRatio(player), 0), graphics, player, screenWidth, screenHeight, vOffset);
    }

    public void renderBarDecorations(GuiGraphicsExtractor graphics, Player player, int screenWidth, int screenHeight, int vOffset) {
    }

    public void renderText(GuiGraphicsExtractor graphics, Player player, int width, int height, int vOffset) {
        int xStart = width / 2 + getIconOffset();
        int yStart = height - vOffset;
        textHelper(graphics, xStart, yStart, barInfo.numerator().getValue(player),
                barSettings.colorProvider().getColor(player, barInfo.getRatio(player), 0).argb());
    }

    public void renderIcon(GuiGraphicsExtractor graphics, Player player, int width, int height, int vOffset) {
        int xStart = width / 2 + getIconOffset();
        int yStart = height - vOffset;
        for (Identifier sprite : barInfo.icons()) {
            ModUtils.blitIcon(graphics, sprite, xStart, yStart);
        }
    }

    public int getHOffset() {
        return switch (getSide()) {
            case LEFT -> -91;
            case RIGHT -> 10;
        };
    }

    public int getIconOffset() {
        return switch (getSide()) {
            case LEFT -> -101;
            case RIGHT -> 92;
        };
    }

    public static HealthEffect getHealthEffect(Player player) {
        if (player.hasEffect(MobEffects.POISON)) {
            return HealthEffect.POISON;
        } else if (player.hasEffect(MobEffects.WITHER)) {
            return HealthEffect.WITHER;
        } else if (player.isFullyFrozen()) {
            return HealthEffect.FROZEN;
        }
        return HealthEffect.NONE;
    }

    protected static boolean isHardcore(Player player) {
        return player.level().getLevelData().isHardcore();
    }

    protected void renderBarBackground(GuiGraphicsExtractor graphics, Player player, int screenWidth, int screenHeight, int vOffset) {
        renderBarBackground(graphics, player, screenWidth, screenHeight, vOffset, false);
    }

    protected void renderBarBackground(GuiGraphicsExtractor graphics, Player player, int screenWidth, int screenHeight, int vOffset, boolean flash) {
        int barWidth = getBarWidth(player);
        int xStart = screenWidth / 2 + getHOffset();
        int yStart = screenHeight - vOffset;
        int v = flash ? 18 : 0;
        if (isFitted()) {
            if (getSide() == BarSide.RIGHT) {
                xStart += WIDTH - barWidth;
            }
            ModUtils.blit(graphics, BAR, xStart, yStart, 0, v, barWidth + 2, 9);
            ModUtils.blit(graphics, BAR, xStart + barWidth + 2, yStart, 79, v, 2, 9);
        } else {
            renderFullBarBackground(graphics, xStart, yStart, v);
        }
    }

    public void textHelper(GuiGraphicsExtractor graphics, int xStart, int yStart, double stat, int color) {
        String text = String.valueOf((int) Math.floor(stat));
        int iconWidth = barSettings.show_icon() ? 9 : 0;
        switch (getSide()) {
            case LEFT -> ModUtils.drawStringOnHUD(graphics, text, xStart - iconWidth - Minecraft.getInstance().font.width(text) + 5, yStart - 1, color);
            case RIGHT -> ModUtils.drawStringOnHUD(graphics, text, xStart + iconWidth, yStart - 1, color);
        }
    }

    protected int getXStartBar(int screenWidth, int barWidth) {
        int xStart = screenWidth / 2 + getHOffset();
        if (getSide() == BarSide.RIGHT) {
            xStart += WIDTH - barWidth;
        }
        return xStart;
    }

    public void renderFullBarBackground(GuiGraphicsExtractor graphics, int xStart, int yStart, int v) {
        ModUtils.blit(graphics, BAR, xStart, yStart, 0, v, 81, 9);
    }

    public void renderFullBar(Color color, GuiGraphicsExtractor graphics, int xStart, int yStart) {
        renderPartialBar(color, graphics, xStart, yStart, WIDTH);
    }

    protected void renderSimpleBar(Color color, GuiGraphicsExtractor graphics, Player player, int screenWidth, int screenHeight, int vOffset) {
        renderSimpleBar(color, graphics, player, screenWidth, screenHeight, vOffset, false);
    }

    protected void renderSimpleBar(Color color, GuiGraphicsExtractor graphics, Player player, int screenWidth, int screenHeight, int vOffset, boolean highlight) {
        int barWidth = getBarWidth(player);
        int xStart = getXStartBar(screenWidth, barWidth);
        int yStart = screenHeight - vOffset;
        renderBarBackground(graphics, player, screenWidth, screenHeight, vOffset, highlight);
        renderPartialBar(color, graphics, xStart + 2, yStart + 2, barWidth);
    }

    public void renderPartialBar(Color color, GuiGraphicsExtractor graphics, double xStart, int yStart, double barWidth) {
        ModUtils.blit(graphics, BAR, (int) xStart, yStart, 2, 11, (int) barWidth, HEIGHT, color.argb());
    }

    public final int getBarWidth(Player player) {
        return (int) Math.ceil(WIDTH * barInfo.getRatio(player));
    }

    protected static Denominator fixed(float value) {
        return p -> value;
    }

    public final boolean isFitted() {
        return barSettings.fitted();
    }

    @Override
    public final String name() {
        return barInfo.name();
    }

    @Override
    public void setErrored() {
        errored = true;
    }

    @Override
    public final Optional<Identifier> disablesOverlay() {
        return barSettings.disablesOverlay();
    }

    @FunctionalInterface
    public interface Denominator {
        float getValue(Player player);
    }

    @FunctionalInterface
    public interface Numerator {
        float getValue(Player player);
    }
}
