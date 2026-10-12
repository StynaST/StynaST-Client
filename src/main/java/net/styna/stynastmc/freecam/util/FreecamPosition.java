package net.styna.stynastmc.freecam.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class FreecamPosition {
    public double x;
    public double y;
    public double z;
    public float pitch;
    public float yaw;

    public FreecamPosition(Entity entity) {
        x = entity.posX;
        // Keep the eyes where the source entity's eyes are.
        y = entity.posY + entity.getEyeHeight() - FreeCamera.EYE_HEIGHT;
        z = entity.posZ;
        yaw = entity.rotationYaw;
        pitch = entity.rotationPitch;
    }

    /** Invert the rotation so that it is mirrored, as third person front view does. */
    public void mirrorRotation() {
        yaw += 180.0F;
        pitch = -pitch;
    }

    public void moveForward(double distance) {
        Vec3d look = Vec3d.fromPitchYaw(pitch, yaw);
        x += look.x * distance;
        y += look.y * distance;
        z += look.z * distance;
    }

    public int chunkX() {
        return (int) Math.floor(x) >> 4;
    }

    public int chunkZ() {
        return (int) Math.floor(z) >> 4;
    }
}
