package net.styna.stynastmc.tweaks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.styna.stynastmc.tweaks.EnchantIcons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Separate from ItemEnchantmentsMixin, which is skipped when Enchantment Descriptions is installed.
@Mixin(ItemEnchantments.class)
public abstract class ItemEnchantmentIconsMixin {
    @WrapOperation(method = "addToTooltip", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/Enchantment;getFullname(Lnet/minecraft/core/Holder;I)Lnet/minecraft/network/chat/Component;"))
    private Component stynastmc$icons(Holder<Enchantment> enchantment, int level, Operation<Component> original) {
        return EnchantIcons.prefix(enchantment, original.call(enchantment, level));
    }
}
