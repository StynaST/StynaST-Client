package net.styna.stynastmc.classicbar.api.colorprovider;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.styna.stynastmc.classicbar.api.Color;

public record DualEffectMapColorProvider(Color primary, Color secondary, Map<Holder<MobEffect>, TwoColors> effectMap) implements ColorProvider {
    public static final MapCodec<DualEffectMapColorProvider> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Color.HEX_CODEC.fieldOf("primary").forGetter(DualEffectMapColorProvider::primary),
            Color.HEX_CODEC.fieldOf("secondary").forGetter(DualEffectMapColorProvider::secondary),
            Codec.unboundedMap(BuiltInRegistries.MOB_EFFECT.holderByNameCodec(), TwoColors.CODEC).fieldOf("mob_effects").forGetter(DualEffectMapColorProvider::effectMap))
            .apply(instance, DualEffectMapColorProvider::new));

    @Override
    public Color getColor(Player player, float ratio, int layer) {
        for (Map.Entry<Holder<MobEffect>, TwoColors> entry : effectMap.entrySet()) {
            if (player.hasEffect(entry.getKey())) {
                return layer == 0 ? entry.getValue().primary() : entry.getValue().secondary();
            }
        }
        return layer == 0 ? primary : secondary;
    }

    @Override
    public ColorProviderSerializer<?> getSerializer() {
        return ColorProviderSerializers.DUAL_EFFECT_MAP;
    }
}
