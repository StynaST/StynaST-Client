package net.styna.stynastmc.photomode.mixin;

import net.minecraft.world.level.Level;
import net.styna.stynastmc.photomode.PhotoMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The integrated server shares this class, so overrides stay on the client level. */
@Mixin(Level.class)
public abstract class LevelMixin {
    @Shadow
    public abstract boolean isClientSide();

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void stynastmc$photoRain(float partial, CallbackInfoReturnable<Float> cir) {
        float rain = PhotoMode.rainOverride();
        if (rain >= 0 && isClientSide()) {
            cir.setReturnValue(rain);
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void stynastmc$photoThunder(float partial, CallbackInfoReturnable<Float> cir) {
        float thunder = PhotoMode.thunderOverride();
        if (thunder >= 0 && isClientSide()) {
            cir.setReturnValue(thunder);
        }
    }
}
