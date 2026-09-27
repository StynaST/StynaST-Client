package net.styna.stynastmc.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.extract.LevelExtractor;

/** Mirrors NeoForge's patch: render the local player while the camera is another entity, e.g. in freecam. */
@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {
    @WrapOperation(method = "extractVisibleEntities", at = @At(value = "CONSTANT", args = "classValue=net/minecraft/client/player/LocalPlayer"))
    private boolean stynastmc$renderLocalPlayer(Object entity, Operation<Boolean> original) {
        LocalPlayer player = Minecraft.getInstance().player;
        return original.call(entity) && !(entity == player && !player.isSpectator());
    }
}
