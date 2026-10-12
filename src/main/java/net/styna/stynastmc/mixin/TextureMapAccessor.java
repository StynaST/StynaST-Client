package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.texture.TextureMap;

@Mixin(TextureMap.class)
public interface TextureMapAccessor {
    @Accessor("mipmapLevels")
    int stynastmc$mipmapLevels();
}
