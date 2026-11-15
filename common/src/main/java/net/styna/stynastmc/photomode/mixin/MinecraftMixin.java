package net.styna.stynastmc.photomode.mixin;

import net.minecraft.client.Minecraft;
import net.styna.stynastmc.photomode.PhotoMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void stynastmc$photoKeys(CallbackInfo ci) {
        if (PhotoMode.handleKeybinds((Minecraft) (Object) this)) {
            ci.cancel();
        }
    }
}
