package net.styna.stynastmc.firstperson;

/** Extra per-state flags on LivingEntityRenderState, filled during extraction and read by the model and layers. */
public interface RenderStateAccess {
    void stynastmc$setCameraEntity(boolean value);

    boolean stynastmc$isCameraEntity();

    void stynastmc$setHideLeftArm(boolean value);

    boolean stynastmc$hideLeftArm();

    void stynastmc$setHideRightArm(boolean value);

    boolean stynastmc$hideRightArm();

    void stynastmc$setHideBody(boolean value);

    boolean stynastmc$hideBody();

    void stynastmc$setArmOffset(float offset);

    float stynastmc$armOffset();

    default void stynastmc$setHideArms(boolean value) {
        stynastmc$setHideLeftArm(value);
        stynastmc$setHideRightArm(value);
    }
}
