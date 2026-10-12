package net.styna.stynastmc.firstperson.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.styna.stynastmc.firstperson.FirstPerson;

/** Extracts the camera entity in first person, shifted behind the camera like in third person. */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
    @Invoker("extractEntity")
    abstract EntityRenderState stynastmc$extractEntity(Entity entity, float partialTickTime);

    @Inject(method = "extractVisibleEntities", at = @At("HEAD"))
    private void stynastmc$extractCameraEntity(Camera camera, Frustum frustum, DeltaTracker deltaTracker,
            LevelRenderState levelRenderState, CallbackInfo ci) {
        Entity entity = camera.entity();
        if (entity == null || camera.isDetached() || !FirstPerson.shouldApply()) {
            return;
        }
        float delta = deltaTracker.getGameTimeDeltaPartialTick(true);
        EntityAccessor access = (EntityAccessor) entity;
        Vec3 pos = access.stynastmc$rawPosition();
        double xo = entity.xo, yo = entity.yo, zo = entity.zo;
        double xOld = entity.xOld, yOld = entity.yOld, zOld = entity.zOld;
        FirstPerson.updateOffset(entity, delta);
        Vec3 offset = FirstPerson.offset();
        access.stynastmc$setRawPosition(pos.add(offset));
        entity.xo += offset.x;
        entity.yo += offset.y;
        entity.zo += offset.z;
        entity.xOld += offset.x;
        entity.yOld += offset.y;
        entity.zOld += offset.z;
        FirstPerson.renderingPlayer = true;
        try {
            levelRenderState.entityRenderStates.add(stynastmc$extractEntity(entity, delta));
        } finally {
            FirstPerson.renderingPlayer = false;
            access.stynastmc$setRawPosition(pos);
            // Some mods leave the bounding box at the shifted position.
            entity.refreshDimensions();
            entity.xo = xo;
            entity.yo = yo;
            entity.zo = zo;
            entity.xOld = xOld;
            entity.yOld = yOld;
            entity.zOld = zOld;
        }
    }
}
