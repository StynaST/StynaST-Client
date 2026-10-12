package net.styna.stynastmc.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.BlockShulkerBox;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.styna.stynastmc.tweaks.ContainerPreview;

/** Drops vanilla's item list while the preview grid shows the contents. */
@Mixin(BlockShulkerBox.class)
public class BlockShulkerBoxMixin {
    @Inject(method = "addInformation", at = @At("HEAD"), cancellable = true)
    private void stynastmc$hideList(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag, CallbackInfo ci) {
        if (ContainerPreview.showing(stack)) {
            ci.cancel();
        }
    }
}
