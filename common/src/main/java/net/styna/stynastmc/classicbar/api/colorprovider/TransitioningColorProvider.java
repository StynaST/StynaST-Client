package net.styna.stynastmc.classicbar.api.colorprovider;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.UnboundedMapCodec;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.styna.stynastmc.classicbar.api.Color;

public record TransitioningColorProvider(Map<Float, Color> colors) implements ColorProvider {
    public static final MapCodec<TransitioningColorProvider> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            codec().fieldOf("colors").forGetter(TransitioningColorProvider::colors))
            .apply(instance, TransitioningColorProvider::new));
    public static final TransitioningColorProvider DEFAULT = new TransitioningColorProvider(gradient("#FF0000", "#FFFF00", "#00FF00"));

    public static UnboundedMapCodec<Float, Color> codec() {
        return Codec.unboundedMap(Codec.STRING.xmap(Float::parseFloat, Object::toString), Color.HEX_CODEC);
    }

    /** Colors at 25%, 50% and 75%. */
    static Map<Float, Color> gradient(String low, String mid, String high) {
        Map<Float, Color> colors = new LinkedHashMap<>();
        colors.put(0.25F, Color.hex2Color(low));
        colors.put(0.5F, Color.hex2Color(mid));
        colors.put(0.75F, Color.hex2Color(high));
        return colors;
    }

    public static Color calculateScaledColor(double d3, Map<Float, Color> colors) {
        int index = -1;
        Color below = null;
        Float belowFraction = null;
        Color above = null;
        Float aboveFraction = null;
        boolean aboveEverything = true;

        for (Map.Entry<Float, Color> entry : colors.entrySet()) {
            float fraction = entry.getKey();
            above = entry.getValue();
            aboveFraction = fraction;
            if (fraction > d3) {
                aboveEverything = false;
                break;
            }

            below = entry.getValue();
            belowFraction = fraction;
            index++;
        }

        if (index == -1) {
            return above;
        }
        if (aboveEverything) {
            return below;
        }

        double d4 = Mth.inverseLerp(d3, belowFraction, aboveFraction);
        return below.colorBlend(above, (float) d4);
    }

    @Override
    public Color getColor(Player player, float ratio, int layer) {
        return calculateScaledColor(ratio, colors);
    }

    @Override
    public ColorProviderSerializer<?> getSerializer() {
        return ColorProviderSerializers.TRANSITIONING;
    }
}
