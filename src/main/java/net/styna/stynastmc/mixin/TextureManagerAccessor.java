package net.styna.stynastmc.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

@Mixin(TextureManager.class)
public interface TextureManagerAccessor {
    @Accessor("mapTextureObjects")
    Map<ResourceLocation, ITextureObject> stynastmc$textures();
}
