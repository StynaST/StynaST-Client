package net.styna.stynastmc.skins.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.styna.stynastmc.skins.SkinsBridge;

/** Chat head glyphs and player heads resolve skins through this cache instead of the player entity. */
@Mixin(PlayerSkinRenderCache.RenderInfo.class)
public abstract class PlayerSkinRenderInfoMixin {
    @Shadow @Final private GameProfile gameProfile;
    @Shadow @Final private PlayerSkin playerSkin;
    @Shadow private RenderType itemRenderType;
    @Shadow private GpuTextureView textureView;
    @Shadow private GlyphRenderTypes glyphRenderTypes;

    @Unique
    private Identifier stynastmc$texture;

    @Unique
    private PlayerSkin stynastmc$skin() {
        return SkinsBridge.forPlayer(gameProfile.id(), playerSkin);
    }

    // Our skins can arrive or change after this entry was cached, so drop the derived render state when they do.
    @Inject(method = {"renderType", "textureView", "glyphRenderTypes"}, at = @At("HEAD"))
    private void stynastmc$refresh(CallbackInfoReturnable<?> cir) {
        Identifier texture = stynastmc$skin().body().texturePath();
        if (!texture.equals(stynastmc$texture)) {
            stynastmc$texture = texture;
            itemRenderType = null;
            textureView = null;
            glyphRenderTypes = null;
        }
    }

    @ModifyExpressionValue(method = {"playerSkin", "renderType", "textureView", "glyphRenderTypes"}, at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/renderer/PlayerSkinRenderCache$RenderInfo;playerSkin:Lnet/minecraft/world/entity/player/PlayerSkin;"))
    private PlayerSkin stynastmc$customSkin(PlayerSkin original) {
        return stynastmc$skin();
    }
}
