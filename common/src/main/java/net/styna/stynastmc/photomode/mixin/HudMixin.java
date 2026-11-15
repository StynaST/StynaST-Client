package net.styna.stynastmc.photomode.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.styna.stynastmc.photomode.PhotoMode;
import net.styna.stynastmc.ui.Ui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void stynastmc$photoF1(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
        PhotoMode.guardHud(Minecraft.getInstance());
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void stynastmc$photoHint(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (PhotoMode.isActive() && !PhotoMode.uiHidden() && mc.gui.screen() == null) {
            Component hint = Component.translatable("stynastmc.photomode.hint", PhotoMode.KEY.getTranslatedKeyMessage());
            int width = mc.getWindow().getGuiScaledWidth();
            Ui.centeredText(g, hint, 0, 6, width, 10, Ui.WHITE);
            if (PhotoMode.status() != null) {
                Ui.centeredText(g, PhotoMode.status(), 0, 18, width, 10, Ui.MUTED);
            }
        }
    }
}
