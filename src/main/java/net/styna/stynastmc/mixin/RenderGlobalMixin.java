package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.MinecraftForgeClient;
import net.styna.stynastmc.firstperson.FirstPerson;

@Mixin(RenderGlobal.class)
public class RenderGlobalMixin {
    // Right after the entity loop, while entity lighting is still set up.
    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;release()V"))
    private void stynastmc$renderBody(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        if (MinecraftForgeClient.getRenderPass() == 0) {
            FirstPerson.renderBody(partialTicks);
        }
    }
}
