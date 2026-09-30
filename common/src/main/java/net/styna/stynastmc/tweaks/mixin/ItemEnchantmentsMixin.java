package net.styna.stynastmc.tweaks.mixin;

import java.util.function.Consumer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.styna.stynastmc.tweaks.EnchantDescriptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEnchantments.class)
public abstract class ItemEnchantmentsMixin {
    @WrapOperation(method = "addToTooltip", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/Enchantment;getFullname(Lnet/minecraft/core/Holder;I)Lnet/minecraft/network/chat/Component;"))
    private Component stynastmc$remember(Holder<Enchantment> enchantment, int level, Operation<Component> original,
            @Share("enchantment") LocalRef<Holder<Enchantment>> current, @Share("level") LocalIntRef currentLevel) {
        current.set(enchantment);
        currentLevel.set(level);
        return original.call(enchantment, level);
    }

    // Both the sorted and unsorted loops add the name through consumer.accept, the description goes right under it.
    @Inject(method = "addToTooltip", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", shift = At.Shift.AFTER))
    private void stynastmc$describe(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components,
            CallbackInfo ci, @Share("enchantment") LocalRef<Holder<Enchantment>> current, @Share("level") LocalIntRef currentLevel) {
        if (current.get() != null) {
            EnchantDescriptions.addDescription(current.get(), currentLevel.get(), consumer);
        }
    }
}
