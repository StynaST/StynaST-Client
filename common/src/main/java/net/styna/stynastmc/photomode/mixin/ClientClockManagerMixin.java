package net.styna.stynastmc.photomode.mixin;

import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.styna.stynastmc.photomode.PhotoMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.class)
public abstract class ClientClockManagerMixin {
    @Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true)
    private void stynastmc$photoTime(Holder<WorldClock> clock, CallbackInfoReturnable<Long> cir) {
        long time = PhotoMode.timeOverride();
        if (time >= 0) {
            cir.setReturnValue(time);
        }
    }
}
