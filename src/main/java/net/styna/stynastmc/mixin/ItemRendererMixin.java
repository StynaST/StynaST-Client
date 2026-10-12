package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.styna.stynastmc.firstperson.FirstPerson;

/** Dynamic first person mode lowers the vanilla hands as you look down, until the body's arms take over. */
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @Shadow @Final private Minecraft mc;
    @Shadow private ItemStack itemStackMainHand;
    @Shadow private ItemStack itemStackOffHand;
    @Shadow private float equippedProgressMainHand;
    @Shadow private float prevEquippedProgressMainHand;
    @Shadow private float equippedProgressOffHand;
    @Shadow private float prevEquippedProgressOffHand;

    @Inject(method = "updateEquippedItem", at = @At("HEAD"), cancellable = true)
    private void stynastmc$lowerHands(CallbackInfo ci) {
        EntityPlayerSP player = mc.player;
        if (player == null || !FirstPerson.shouldApply() || !FirstPerson.showVanillaHands(player) || !FirstPerson.dynamicHandsEnabled()) {
            return;
        }
        float pitch = player.rotationPitch;
        if (pitch <= 15) {
            return;
        }
        prevEquippedProgressMainHand = equippedProgressMainHand;
        prevEquippedProgressOffHand = equippedProgressOffHand;
        if (pitch < 30) {
            equippedProgressMainHand = 15 / pitch;
            equippedProgressOffHand = 15 / pitch;
        } else {
            equippedProgressMainHand -= equippedProgressMainHand > -0.1f ? 0.15f : 0;
            equippedProgressOffHand -= equippedProgressOffHand > -0.1f ? 0.15f : 0;
        }
        itemStackMainHand = player.getHeldItemMainhand();
        itemStackOffHand = player.getHeldItemOffhand();
        ci.cancel();
    }
}
