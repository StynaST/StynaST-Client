package net.styna.stynastmc.hotreload.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(TextureAtlasSprite.class)
public interface TextureAtlasSpriteAccessor {
    @Accessor("padding")
    int stynastmc$padding();
}
