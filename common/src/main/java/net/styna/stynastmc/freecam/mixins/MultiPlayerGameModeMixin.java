package net.styna.stynastmc.freecam.mixins;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.styna.stynastmc.freecam.Freecam;
import net.styna.stynastmc.freecam.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.styna.stynastmc.freecam.Freecam.MC;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    // Prevents interacting with blocks when allowInteract is disabled.
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(LocalPlayer player,
                                 InteractionHand hand,
                                 BlockHitResult hitResult,
                                 CallbackInfoReturnable<InteractionResult> cir) {
        if (freecam$disableInteract()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    // Prevents interacting with entities when allowInteract is disabled, and prevents interacting with self.
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void onInteractEntity(
            Player player,
            Entity entity,
            EntityHitResult hitResult,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (entity == MC.player || freecam$disableInteract()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }


    // Prevents attacking self.
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(Player player, Entity target, CallbackInfo ci) {
        if (target == MC.player) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean freecam$disableInteract() {
        return Freecam.isEnabled() && !Freecam.isPlayerControlEnabled() && ModConfig.get().shouldPreventInteractions();
    }
}
