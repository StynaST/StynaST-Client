package net.styna.stynastmc.skins.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.render.pip.GuiSkinRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.pip.GuiSkinRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.styna.stynastmc.capes.CapePreview;

/** Draws the attached cape in the same pass as the GUI skin preview, so depth sorts it against the body. */
@Mixin(GuiSkinRenderer.class)
public class GuiSkinRendererMixin {
    @Inject(method = "renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/GuiSkinRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
            at = @At("TAIL"))
    private void stynastmc$cape(GuiSkinRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CallbackInfo ci) {
        Identifier cape = CapePreview.capeFor(state.playerModel());
        if (cape != null) {
            collector.submitModel(CapePreview.model(), Unit.INSTANCE, poseStack, cape, 15728880, OverlayTexture.NO_OVERLAY, 0);
        }
    }
}
