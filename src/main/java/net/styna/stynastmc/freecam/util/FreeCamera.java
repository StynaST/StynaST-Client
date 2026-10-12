package net.styna.stynastmc.freecam.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.styna.stynastmc.freecam.config.FreecamConfig;
import net.styna.stynastmc.freecam.config.Perspective;

/**
 * Camera entity. It is never added to the world, Freecam ticks it, and it always collides with blocks.
 */
public class FreeCamera extends Entity {
    public static final float EYE_HEIGHT = 0.4F;
    private static final double DIAGONAL = MathHelper.sin((float) Math.toRadians(45));

    public MovementInput input;

    public FreeCamera(World world) {
        super(world);
        setSize(0.6F, 0.6F);
        noClip = false;
        input = new MovementInputFromOptions(Minecraft.getMinecraft().gameSettings);
    }

    @Override
    protected void entityInit() {
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
    }

    @Override
    public float getEyeHeight() {
        return EYE_HEIGHT;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }

    public void copyPosition(Entity entity) {
        applyPosition(new FreecamPosition(entity));
    }

    public void applyPosition(FreecamPosition position) {
        setLocationAndAngles(position.x, position.y, position.z, position.yaw, position.pitch);
        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch;
        motionX = motionY = motionZ = 0;
        updateChunkCoords();
    }

    public void applyPerspective(Perspective perspective) {
        FreecamPosition position = new FreecamPosition(this);
        switch (perspective) {
            case INSIDE -> {
            }
            case FIRST_PERSON -> moveForwardUntilCollision(position, 0.4);
            case THIRD_PERSON_MIRROR -> {
                position.mirrorRotation();
                moveForwardUntilCollision(position, -4.0);
            }
            case THIRD_PERSON -> moveForwardUntilCollision(position, -4.0);
        }
    }

    /** Steps along the view direction, stopping before the camera would end up inside a block. */
    private void moveForwardUntilCollision(FreecamPosition position, double maxDistance) {
        double step = maxDistance < 0 ? -0.1 : 0.1;
        for (double distance = 0.0; distance < Math.abs(maxDistance); distance += 0.1) {
            FreecamPosition previous = new FreecamPosition(this);
            position.moveForward(step);
            applyPosition(position);
            if (!world.getCollisionBoxes(this, getEntityBoundingBox()).isEmpty()) {
                applyPosition(previous);
                return;
            }
        }
    }

    public void tickCamera() {
        ticksExisted++;
        prevPosX = lastTickPosX = posX;
        prevPosY = lastTickPosY = posY;
        prevPosZ = lastTickPosZ = posZ;
        input.updatePlayerMoveState();
        Minecraft mc = Minecraft.getMinecraft();
        setSprinting(mc.gameSettings.keyBindSprint.isKeyDown() && input.forwardKeyDown || isSprinting() && input.forwardKeyDown);
        double h = FreecamConfig.HORIZONTAL_SPEED.get();
        double v = FreecamConfig.VERTICAL_SPEED.get();
        if (FreecamConfig.FLIGHT_MODE.get() == net.styna.stynastmc.freecam.config.FlightMode.CREATIVE) {
            moveRelative(input.moveStrafe, 0.0F, input.moveForward, (float) (h / 10 * (isSprinting() ? 2 : 1)));
            if (input.jump ^ input.sneak) {
                motionY += (input.jump ? 1 : -1) * v / 10 * 3;
            }
            move(MoverType.SELF, motionX, motionY, motionZ);
            motionX *= 0.91;
            motionZ *= 0.91;
            motionY *= 0.6;
        } else {
            defaultMotion(h * (isSprinting() ? 1.5 : 1.0), v);
            move(MoverType.SELF, motionX, motionY, motionZ);
        }
        updateChunkCoords();
    }

    private void defaultMotion(double hSpeed, double vSpeed) {
        float yaw = rotationYaw * 0.017453292F;
        double fx = -MathHelper.sin(yaw);
        double fz = MathHelper.cos(yaw);
        double vx = 0.0;
        double vy = 0.0;
        double vz = 0.0;
        boolean straight = false;
        if (input.forwardKeyDown) {
            vx += fx * hSpeed;
            vz += fz * hSpeed;
            straight = true;
        }
        if (input.backKeyDown) {
            vx -= fx * hSpeed;
            vz -= fz * hSpeed;
            straight = true;
        }
        boolean strafing = false;
        // Right of the view is forward rotated by +90 degrees.
        if (input.rightKeyDown) {
            vx -= fz * hSpeed;
            vz += fx * hSpeed;
            strafing = true;
        }
        if (input.leftKeyDown) {
            vx += fz * hSpeed;
            vz -= fx * hSpeed;
            strafing = true;
        }
        if (straight && strafing) {
            vx *= DIAGONAL;
            vz *= DIAGONAL;
        }
        if (input.jump) {
            vy += vSpeed;
        }
        if (input.sneak) {
            vy -= vSpeed;
        }
        motionX = vx;
        motionY = vy;
        motionZ = vz;
    }

    private void updateChunkCoords() {
        chunkCoordX = MathHelper.floor(posX) >> 4;
        chunkCoordY = MathHelper.floor(posY) >> 4;
        chunkCoordZ = MathHelper.floor(posZ) >> 4;
    }
}
