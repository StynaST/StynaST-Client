package net.styna.stynastmc.classicbar.impl.overlays.vanilla;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.styna.stynastmc.classicbar.api.BarSettings;
import net.styna.stynastmc.classicbar.impl.BarInfo;
import net.styna.stynastmc.classicbar.impl.overlays.templates.BarOverlayImpl;

public class MountHealth extends BarOverlayImpl {
    public static final BarInfo INFO = BarInfo.builder("mount_health")
            .setShouldRender(player -> player.getVehicle() instanceof LivingEntity mount && mount.isAlive())
            .setNumerator(player -> ((LivingEntity) player.getVehicle()).getHealth())
            .setDenominator(player -> ((LivingEntity) player.getVehicle()).getMaxHealth())
            .setIcons(Identifier.withDefaultNamespace("hud/heart/vehicle_container"), Identifier.withDefaultNamespace("hud/heart/vehicle_full"))
            .build();
    public static final Codec<MountHealth> CODEC = RecordCodecBuilder.create(o -> codecStart(o).apply(o, MountHealth::new));

    public MountHealth(BarSettings barSettings) {
        super(INFO, barSettings);
    }

    @Override
    public Codec<? extends BarOverlayImpl> codec() {
        return CODEC;
    }
}
