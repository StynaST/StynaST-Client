package net.styna.stynastmc.classicbar.api.colorprovider;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.player.Player;
import net.styna.stynastmc.classicbar.api.Color;
import net.styna.stynastmc.classicbar.impl.overlays.templates.BarOverlayImpl;
import net.styna.stynastmc.classicbar.util.HealthEffect;

public record TransitioningEffectColorProvider(
        Map<Float, Color> normalColors, Map<Float, Color> poisonColors, Map<Float, Color> witherColors, Map<Float, Color> frozenColors,
        Color hardcoreColor)
        implements ColorProvider {
    public static final MapCodec<TransitioningEffectColorProvider> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TransitioningColorProvider.codec().fieldOf("normal").forGetter(TransitioningEffectColorProvider::normalColors),
            TransitioningColorProvider.codec().fieldOf("poison").forGetter(TransitioningEffectColorProvider::poisonColors),
            TransitioningColorProvider.codec().fieldOf("wither").forGetter(TransitioningEffectColorProvider::witherColors),
            TransitioningColorProvider.codec().fieldOf("frozen").forGetter(TransitioningEffectColorProvider::frozenColors),
            Color.HEX_CODEC.fieldOf("hardcore").forGetter(TransitioningEffectColorProvider::hardcoreColor))
            .apply(instance, TransitioningEffectColorProvider::new));
    public static final TransitioningEffectColorProvider DEFAULT = new TransitioningEffectColorProvider(
            TransitioningColorProvider.gradient("#FF0000", "#FFFF00", "#00FF00"),
            TransitioningColorProvider.gradient("#00FF00", "#55FF55", "#00FF00"),
            TransitioningColorProvider.gradient("#555555", "#AAAAAA", "#555555"),
            TransitioningColorProvider.gradient("#7fafff", "#7fafff", "#7fafff"),
            Color.hex2Color("#54030A"));

    @Override
    public Color getColor(Player player, float ratio, int layer) {
        HealthEffect effect = BarOverlayImpl.getHealthEffect(player);
        if (effect == HealthEffect.NONE && player.level().getLevelData().isHardcore()) {
            return hardcoreColor;
        }
        Map<Float, Color> colors = switch (effect) {
            case NONE -> normalColors;
            case POISON -> poisonColors;
            case WITHER -> witherColors;
            case FROZEN -> frozenColors;
        };
        return TransitioningColorProvider.calculateScaledColor(ratio, colors);
    }

    @Override
    public ColorProviderSerializer<?> getSerializer() {
        return ColorProviderSerializers.TRANSITIONING_EFFECT;
    }
}
