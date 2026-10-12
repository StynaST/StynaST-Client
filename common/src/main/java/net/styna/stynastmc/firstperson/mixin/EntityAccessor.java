package net.styna.stynastmc.firstperson.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("position")
    Vec3 stynastmc$rawPosition();

    @Accessor("position")
    void stynastmc$setRawPosition(Vec3 position);
}
