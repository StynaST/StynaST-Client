package net.styna.stynastmc.classicbar.api.colorprovider;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.styna.stynastmc.classicbar.api.Color;

public record DualEffectColorProvider(Holder<MobEffect> effect, Color primary, Color secondary, Color primaryUnderEffect, Color secondaryUnderEffect)
        implements ColorProvider {
    public static final MapCodec<DualEffectColorProvider> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("mob_effect").forGetter(DualEffectColorProvider::effect),
            Color.HEX_CODEC.fieldOf("primary").forGetter(DualEffectColorProvider::primary),
            Color.HEX_CODEC.fieldOf("secondary").forGetter(DualEffectColorProvider::secondary),
            Color.HEX_CODEC.fieldOf("primary_under_effect").forGetter(DualEffectColorProvider::primaryUnderEffect),
            Color.HEX_CODEC.fieldOf("secondary_under_effect").forGetter(DualEffectColorProvider::secondaryUnderEffect))
            .apply(instance, DualEffectColorProvider::new));
    public static final DualEffectColorProvider FOOD = new DualEffectColorProvider(
            MobEffects.HUNGER, Color.hex2Color("#B34D00"), Color.hex2Color("#FFCC00"), Color.hex2Color("#249016"), Color.hex2Color("#87BC00"));

    @Override
    public Color getColor(Player player, float ratio, int layer) {
        if (player.hasEffect(effect)) {
            return layer == 0 ? primaryUnderEffect : secondaryUnderEffect;
        }
        return layer == 0 ? primary : secondary;
    }

    @Override
    public ColorProviderSerializer<?> getSerializer() {
        return ColorProviderSerializers.DUAL_EFFECT;
    }
}
