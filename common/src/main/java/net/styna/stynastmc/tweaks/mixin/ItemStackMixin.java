package net.styna.stynastmc.tweaks.mixin;

import java.util.function.Consumer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.styna.stynastmc.tweaks.TweaksConfig;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    // NeoForge moves the vanilla body into addDetailsToTooltipComponents
    @Inject(method = {"addDetailsToTooltip", "addDetailsToTooltipComponents"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/TooltipFlag;isAdvanced()Z", ordinal = 0))
    private void stynastmc$durability(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag flag,
            Consumer<Component> builder, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!TweaksConfig.on(TweaksConfig.DURABILITY_TOOLTIP) || !stack.isDamageableItem() || !display.shows(DataComponents.DAMAGE)) {
            return;
        }
        // Advanced tooltips already print durability for damaged items.
        if (flag.isAdvanced() && stack.isDamaged()) {
            return;
        }
        int max = stack.getMaxDamage();
        int left = max - stack.getDamageValue();
        builder.accept(Component.translatable("stynastmc.tooltip.durability",
                Component.literal(String.valueOf(left)).withColor(stack.getBarColor()), max).withColor(0xAAAAAA));
    }
}
