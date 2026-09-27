package net.styna.stynastmc.tweaks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import net.styna.stynastmc.tweaks.DebugPauseKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/KeyEvent;isEscape()Z"))
    private boolean stynastmc$debugPauseKey(KeyEvent event, Operation<Boolean> original) {
        return DebugPauseKey.KEY.matches(event);
    }
}
