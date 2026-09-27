package net.styna.stynastmc.classicbar.impl.overlays.templates;

import java.util.function.DoubleSupplier;
import java.util.function.ToDoubleFunction;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.styna.stynastmc.classicbar.api.BarOverlay;
import net.styna.stynastmc.classicbar.api.BarSettings;
import net.styna.stynastmc.classicbar.api.BarSide;
import net.styna.stynastmc.classicbar.api.Color;
import net.styna.stynastmc.classicbar.config.ClassicBarConfig;
import net.styna.stynastmc.classicbar.impl.BarInfo;
import net.styna.stynastmc.classicbar.network.ClassicBarNetwork;
import net.styna.stynastmc.classicbar.util.ModUtils;

public abstract class FoodLikeBarOverlay extends BarOverlayImpl {
    private final Codec<? extends FoodLikeBarOverlay> codec;
    private final DoubleSupplier maxExhaustionGetter;
    private final ToDoubleFunction<Player> exhaustionGetter;
    protected final boolean showSaturation;
    protected final boolean showExhaustion;
    protected final boolean showPredictedHealing;

    protected static <T extends FoodLikeBarOverlay> Products.P4<RecordCodecBuilder.Mu<T>, BarSettings, Boolean, Boolean, Boolean> codecStartFoodLike(
            RecordCodecBuilder.Instance<T> instance) {
        return codecStart(instance)
                .and(Codec.BOOL.fieldOf("show_saturation").forGetter(t -> t.showSaturation))
                .and(Codec.BOOL.fieldOf("show_exhaustion").forGetter(t -> t.showExhaustion))
                .and(Codec.BOOL.fieldOf("show_predicted_healing").forGetter(t -> t.showPredictedHealing));
    }

    protected FoodLikeBarOverlay(BarInfo barInfo, Codec<? extends FoodLikeBarOverlay> codec, DoubleSupplier maxExhaustionGetter,
            ToDoubleFunction<Player> exhaustionGetter, BarSettings barSettings, boolean showSaturation, boolean showExhaustion,
            boolean showPredictedHealing) {
        super(barInfo, barSettings);
        this.codec = codec;
        this.maxExhaustionGetter = maxExhaustionGetter;
        this.exhaustionGetter = exhaustionGetter;
        this.showSaturation = showSaturation;
        this.showExhaustion = showExhaustion;
        this.showPredictedHealing = showPredictedHealing;
    }

    @Override
    public void renderBar(GuiGraphicsExtractor graphics, Player player, int screenWidth, int screenHeight, int vOffset) {
        float level = barInfo.numerator().getValue(player);
        double saturation = getSaturationValue(player);
        int xStart = screenWidth / 2 + getHOffset();
        int yStart = screenHeight - vOffset;
        renderBarBackground(graphics, player, screenWidth, screenHeight, vOffset);
        drawLayer(graphics, getPrimaryBarColor(player), xStart, yStart, level, barInfo.denominator().getValue(player));
        if (saturation > 0.0 && showSaturation()) {
            drawLayer(graphics, getSecondaryBarColor(player), xStart, yStart, saturation, barInfo.denominator().getValue(player));
        }
    }

    @Override
    public void renderBarDecorations(GuiGraphicsExtractor graphics, Player player, int screenWidth, int screenHeight, int vOffset) {
        int xStart = screenWidth / 2 + getHOffset();
        int yStart = screenHeight - vOffset;
        if (showPredictedHealing) {
            drawOverlayPrediction(graphics, player, xStart, yStart, 20.0, 20.0);
        }
        if (showExhaustion && ClassicBarNetwork.presentOnServer) {
            double maxExhaustionLevel = maxExhaustionGetter.getAsDouble();
            double exhaustionLevel = Math.min(exhaustionGetter.applyAsDouble(player), maxExhaustionLevel);
            drawExhaustion(graphics, xStart, yStart, exhaustionLevel, maxExhaustionLevel);
        }
    }

    protected void drawLayer(GuiGraphicsExtractor graphics, Color color, int x, int y, double value, double maxValue) {
        double barWidth = getWidth(value, maxValue);
        double barXStart = x + (getSide() == BarSide.RIGHT ? WIDTH - barWidth : 0.0);
        renderPartialBar(color, graphics, barXStart + 2.0, y + 2, barWidth);
    }

    protected void drawExhaustion(GuiGraphicsExtractor graphics, int x, int y, double exhaustionLevel, double maxLevel) {
        int barWidth = getWidth(exhaustionLevel, maxLevel);
        int barXStart = x + (getSide() == BarSide.RIGHT ? WIDTH - barWidth : 0);
        ModUtils.blit(graphics, BAR, barXStart + 2, y + 1, 1, 28, barWidth, 9, ARGB.white(0.25F));
    }

    public boolean showSaturation() {
        return showSaturation;
    }

    public abstract boolean isHealingItem(ItemStack stack, Player player);

    protected void drawOverlayPrediction(GuiGraphicsExtractor graphics, Player player, int x, int y, double maxLevel, double maxSaturationLevel) {
        ItemStack stack = player.getMainHandItem();
        if (!isHealingItem(stack, player)) {
            return;
        }

        double time = System.currentTimeMillis() / 1000.0 * ClassicBarConfig.TRANSITION_SPEED.get();
        float alpha = (float) (Math.sin(time) / 2.0 + 0.5);
        float level = barInfo.numerator().getValue(player);
        float potentialLevel = getPotentialHealing(stack, player);
        double restoredLevel = Math.min(maxLevel - level, potentialLevel);
        if (level < maxLevel) {
            drawLayer(graphics, getPrimaryBarColor(player).withAlpha(alpha), x, y, level + restoredLevel, maxLevel);
        }

        if (showSaturation()) {
            float saturation = getSaturationValue(player);
            float potentialSaturation = getPotentialSaturation(stack, player);
            double restoredSaturation = Math.min(maxSaturationLevel - saturation, potentialSaturation);
            if (saturation + potentialSaturation > level + restoredLevel) {
                restoredSaturation = potentialSaturation - (saturation + potentialSaturation - (level + restoredLevel));
            }
            drawLayer(graphics, getSecondaryBarColor(player).withAlpha(alpha), x, y, saturation + restoredSaturation, maxSaturationLevel);
        }
    }

    public abstract float getSaturationValue(Player player);

    public abstract int getPotentialHealing(ItemStack stack, Player player);

    public abstract float getPotentialSaturation(ItemStack stack, Player player);

    public Color getPrimaryBarColor(Player player) {
        return getBarSettings().colorProvider().getColor(player, barInfo.getRatio(player), 0);
    }

    public Color getSecondaryBarColor(Player player) {
        return getBarSettings().colorProvider().getColor(player, barInfo.getRatio(player), 1);
    }

    @Override
    public void renderText(GuiGraphicsExtractor graphics, Player player, int width, int height, int vOffset) {
        int xStart = width / 2 + getIconOffset();
        int yStart = height - vOffset;
        textHelper(graphics, xStart, yStart, barInfo.numerator().getValue(player), getPrimaryBarColor(player).argb());
    }

    @Override
    public Codec<? extends BarOverlay> codec() {
        return codec;
    }
}
