package net.styna.stynastmc.hotreload.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.styna.stynastmc.hotreload.SkinTotem;

@Mixin(SpriteLoader.class)
public class SpriteLoaderMixin {
    @ModifyVariable(method = "stitch", at = @At("HEAD"), argsOnly = true)
    private List<SpriteContents> stynastmc$skinTotem(List<SpriteContents> sprites) {
        return SkinTotem.substitute(sprites);
    }
}
