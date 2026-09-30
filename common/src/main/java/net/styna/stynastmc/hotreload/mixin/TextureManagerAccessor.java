package net.styna.stynastmc.hotreload.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

@Mixin(TextureManager.class)
public interface TextureManagerAccessor {
    @Accessor("byPath")
    Map<Identifier, AbstractTexture> stynastmc$byPath();
}
