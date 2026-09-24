package net.styna.stynastmc.classicbar.impl.overlays.vanilla;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.styna.stynastmc.classicbar.api.BarSettings;
import net.styna.stynastmc.classicbar.impl.BarInfo;
import net.styna.stynastmc.classicbar.impl.overlays.templates.BarOverlayImpl;

public class Air extends BarOverlayImpl {
    public static final BarInfo INFO = BarInfo.builder("air")
            .setShouldRender(player -> player.getAirSupply() < player.getMaxAirSupply())
            .setNumerator(Entity::getAirSupply)
            .setDenominator(Entity::getMaxAirSupply)
            .setIcons(Identifier.withDefaultNamespace("hud/air"))
            .build();
    public static final Codec<Air> CODEC = RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, Air::new));

    public Air(BarSettings settings) {
        super(INFO, settings);
    }

    @Override
    public Codec<? extends BarOverlayImpl> codec() {
        return CODEC;
    }
}
