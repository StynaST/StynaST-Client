package net.styna.stynastmc.classicbar.api.colorprovider;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.world.entity.player.Player;
import net.styna.stynastmc.classicbar.api.Color;

public interface ColorProvider {
    Codec<ColorProvider> CODEC = Codec.STRING
            .<ColorProviderSerializer<?>>comapFlatMap(name -> Optional.<ColorProviderSerializer<?>>ofNullable(ColorProviderSerializers.MAP.get(name))
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown color provider: " + name)), ColorProviderSerializer::name)
            .dispatch(ColorProvider::getSerializer, ColorProviderSerializer::codec);

    Color getColor(Player player, float ratio, int layer);

    ColorProviderSerializer<?> getSerializer();
}
