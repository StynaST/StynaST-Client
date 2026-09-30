package net.styna.stynastmc.tweaks.mixin;

import java.util.function.Consumer;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.styna.stynastmc.tweaks.ContainerPreview;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemContainerContents.class)
public abstract class ItemContainerContentsMixin {
    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private void stynastmc$hideListWhilePreviewing(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag,
            DataComponentGetter components, CallbackInfo ci) {
        if (components instanceof ItemStack stack && ContainerPreview.showing(stack)) {
            ci.cancel();
        }
    }
}
