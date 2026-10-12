package net.styna.stynastmc.firstperson.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.styna.stynastmc.firstperson.FirstPerson;

/** Dynamic mode lowers the vanilla hands as you look down, until the body's arms take over. */
@Mixin(FirstPersonHandsAndItems.class)
public class FirstPersonHandsAndItemsMixin {
    @Shadow private float mainHandHeight;
    @Shadow private float offHandHeight;
    @Shadow private ItemStack mainHandItem;
    @Shadow private ItemStack offHandItem;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"), cancellable = true)
    private void stynastmc$lowerHands(LocalPlayer player, CallbackInfo ci) {
        if (!FirstPerson.shouldApply() || !FirstPerson.showVanillaHands() || !FirstPerson.dynamicHandsEnabled()) {
            return;
        }
        float xRot = player.getXRot();
        if (xRot <= 15) {
            return;
        }
        if (xRot < 30) {
            mainHandHeight = 15 / xRot;
            offHandHeight = 15 / xRot;
        } else {
            mainHandHeight -= mainHandHeight > -0.1f ? 0.15f : 0;
            offHandHeight -= offHandHeight > -0.1f ? 0.15f : 0;
        }
        mainHandItem = player.getMainHandItem();
        offHandItem = player.getOffhandItem();
        ci.cancel();
    }
}
