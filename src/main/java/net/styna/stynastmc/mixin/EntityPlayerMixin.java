package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.styna.stynastmc.firstperson.FirstPerson;

/** Strips the helmet, and the held items when vanilla hands take over, from the first person body. */
@Mixin(EntityPlayer.class)
public class EntityPlayerMixin {
    @Inject(method = "getItemStackFromSlot", at = @At("HEAD"), cancellable = true)
    private void stynastmc$hideEquipment(EntityEquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        if (!FirstPerson.renderingPlayer || (Object) this != Minecraft.getMinecraft().player) {
            return;
        }
        if (slot == EntityEquipmentSlot.HEAD || FirstPerson.hideArms && slot.getSlotType() == EntityEquipmentSlot.Type.HAND) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
