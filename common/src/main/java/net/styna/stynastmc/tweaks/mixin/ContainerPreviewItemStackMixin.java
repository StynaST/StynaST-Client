package net.styna.stynastmc.tweaks.mixin;

import java.util.List;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.styna.stynastmc.tweaks.ContainerPreview;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ContainerPreviewItemStackMixin {
    @Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
    private void stynastmc$containerPreview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        ContainerPreview.image((ItemStack) (Object) this).ifPresent(image -> cir.setReturnValue(Optional.of(image)));
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void stynastmc$previewHint(Item.TooltipContext context, @Nullable Player player, TooltipFlag flag,
            CallbackInfoReturnable<List<Component>> cir) {
        ContainerPreview.addHint((ItemStack) (Object) this, cir.getReturnValue());
    }
}
