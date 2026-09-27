package net.styna.stynastmc.classicbar.impl.overlays.vanilla;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.styna.stynastmc.classicbar.api.BarSettings;
import net.styna.stynastmc.classicbar.impl.BarInfo;
import net.styna.stynastmc.classicbar.impl.overlays.templates.StackingBarOverlay;

public class Armor extends StackingBarOverlay {
    public static final BarInfo INFO = BarInfo.builder("armor")
            .setNumerator(LivingEntity::getArmorValue)
            .setDenominator(fixed(20.0F))
            .setIcons(Identifier.withDefaultNamespace("hud/armor_full"))
            .build();
    public static final Codec<Armor> CODEC = RecordCodecBuilder.create(o -> codecStart(o).apply(o, Armor::new));

    public Armor(BarSettings barSettings) {
        super(INFO, barSettings, CODEC);
    }
}
