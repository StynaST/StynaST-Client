package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    // Setting the field directly skips setRenderViewEntity's entity shader reload.
    @Accessor("renderViewEntity")
    void stynastmc$setRenderViewEntity(Entity entity);
}
