package net.styna.stynastmc.firstperson.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.styna.stynastmc.firstperson.RenderStateAccess;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements RenderStateAccess {
    @Unique
    private boolean stynastmc$cameraEntity;
    @Unique
    private boolean stynastmc$hideLeftArm;
    @Unique
    private boolean stynastmc$hideRightArm;
    @Unique
    private boolean stynastmc$hideBody;
    @Unique
    private float stynastmc$armOffset;

    @Override
    public void stynastmc$setCameraEntity(boolean value) {
        stynastmc$cameraEntity = value;
    }

    @Override
    public boolean stynastmc$isCameraEntity() {
        return stynastmc$cameraEntity;
    }

    @Override
    public void stynastmc$setHideLeftArm(boolean value) {
        stynastmc$hideLeftArm = value;
    }

    @Override
    public boolean stynastmc$hideLeftArm() {
        return stynastmc$hideLeftArm;
    }

    @Override
    public void stynastmc$setHideRightArm(boolean value) {
        stynastmc$hideRightArm = value;
    }

    @Override
    public boolean stynastmc$hideRightArm() {
        return stynastmc$hideRightArm;
    }

    @Override
    public void stynastmc$setHideBody(boolean value) {
        stynastmc$hideBody = value;
    }

    @Override
    public boolean stynastmc$hideBody() {
        return stynastmc$hideBody;
    }

    @Override
    public void stynastmc$setArmOffset(float offset) {
        stynastmc$armOffset = offset;
    }

    @Override
    public float stynastmc$armOffset() {
        return stynastmc$armOffset;
    }
}
