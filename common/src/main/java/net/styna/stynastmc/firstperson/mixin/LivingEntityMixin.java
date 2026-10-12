package net.styna.stynastmc.firstperson.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.styna.stynastmc.firstperson.FirstPerson;

/** Strips the helmet, and the held items when vanilla hands take over, from the first person body. */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "getItemBySlot", at = @At("HEAD"), cancellable = true)
    private void stynastmc$hideEquipment(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        if (!FirstPerson.renderingPlayer || !((Object) this instanceof Player player) || !Minecraft.getInstance().isSameThread()) {
            return;
        }
        if (slot == EquipmentSlot.HEAD
                || (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) && FirstPerson.hideArmsAndItems(player)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
