package net.styna.stynastmc.hotreload.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.texture.TextureAtlas;

@Mixin(TextureAtlas.class)
public interface TextureAtlasAccessor {
    @Accessor("maxMipLevel")
    int stynastmc$maxMipLevel();
}
