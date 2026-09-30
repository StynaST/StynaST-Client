package net.styna.stynastmc.tweaks.mixin;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.styna.stynastmc.tweaks.EnchantDescriptions;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class EnchantDescriptionsItemStackMixin {
    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void stynastmc$shiftHint(Item.TooltipContext context, @Nullable Player player, TooltipFlag flag,
            CallbackInfoReturnable<List<Component>> cir) {
        EnchantDescriptions.addShiftHint((ItemStack) (Object) this, cir.getReturnValue());
    }
}
