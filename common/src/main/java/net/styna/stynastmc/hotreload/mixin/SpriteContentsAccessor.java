package net.styna.stynastmc.hotreload.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.SpriteContents;

@Mixin(SpriteContents.class)
public interface SpriteContentsAccessor {
    @Accessor("mipmapStrategy")
    MipmapStrategy stynastmc$mipmapStrategy();

    @Accessor("alphaCutoffBias")
    float stynastmc$alphaCutoffBias();
}
