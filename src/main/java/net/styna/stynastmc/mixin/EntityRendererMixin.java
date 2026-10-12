package net.styna.stynastmc.mixin;

import java.util.Arrays;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.styna.stynastmc.freecam.Freecam;
import net.styna.stynastmc.freecam.config.FreecamConfig;
import net.styna.stynastmc.tweaks.TweaksConfig;
import net.styna.stynastmc.zoom.Zoom;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    @Final
    private int[] lightmapColors;

    @Unique
    private Entity stynastmc$camera;

    /** World FOV only; the hand is drawn with useFOVSetting false and stays unzoomed. */
    @Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true)
    private void stynastmc$zoom(float partialTicks, boolean useFOVSetting, CallbackInfoReturnable<Float> cir) {
        if (useFOVSetting) {
            cir.setReturnValue(Zoom.apply(cir.getReturnValueF()));
        }
    }

    @Redirect(method = {"updateCameraAndRender", "updateRenderer"}, at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/client/settings/GameSettings;mouseSensitivity:F"))
    private float stynastmc$zoomSensitivity(GameSettings settings) {
        return Zoom.sensitivity(settings.mouseSensitivity);
    }

    @Redirect(method = {"updateCameraAndRender", "updateRenderer"}, at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/client/settings/GameSettings;smoothCamera:Z"))
    private boolean stynastmc$zoomCinematic(GameSettings settings) {
        return settings.smoothCamera || Zoom.cinematicCamera();
    }

    @Inject(method = "applyBobbing", at = @At("HEAD"), cancellable = true)
    private void stynastmc$noBobbing(float partialTicks, CallbackInfo ci) {
        if (TweaksConfig.on(TweaksConfig.NO_BOBBING)) {
            ci.cancel();
        }
    }

    @Inject(method = "updateLightmap", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;updateDynamicTexture()V"))
    private void stynastmc$fullBright(float partialTicks, CallbackInfo ci) {
        if (Freecam.isEnabled() && FreecamConfig.FULL_BRIGHT.get()) {
            Arrays.fill(lightmapColors, 0xFFFFFFFF);
        }
    }

    // The HUD reads the camera entity, so hand it the player while freecam owns the camera.
    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V"))
    private void stynastmc$hudFromPlayer(float partialTicks, long nanoTime, CallbackInfo ci) {
        stynastmc$swapToPlayer();
    }

    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V", shift = At.Shift.AFTER))
    private void stynastmc$hudDone(float partialTicks, long nanoTime, CallbackInfo ci) {
        stynastmc$restore();
    }

    // Interactions can come from the player instead of the camera.
    @Inject(method = "getMouseOver", at = @At("HEAD"))
    private void stynastmc$pickFromPlayer(float partialTicks, CallbackInfo ci) {
        if (Freecam.interactFromPlayer()) {
            stynastmc$swapToPlayer();
        }
    }

    @Inject(method = "getMouseOver", at = @At("RETURN"))
    private void stynastmc$pickDone(float partialTicks, CallbackInfo ci) {
        stynastmc$restore();
    }

    @Unique
    private void stynastmc$swapToPlayer() {
        if (Freecam.isEnabled() && mc.player != null) {
            stynastmc$camera = mc.getRenderViewEntity();
            ((MinecraftAccessor) mc).stynastmc$setRenderViewEntity(mc.player);
        }
    }

    @Unique
    private void stynastmc$restore() {
        if (stynastmc$camera != null) {
            ((MinecraftAccessor) mc).stynastmc$setRenderViewEntity(stynastmc$camera);
            stynastmc$camera = null;
        }
    }
}
