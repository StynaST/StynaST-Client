package net.styna.stynastmc.photomode;

import net.styna.stynastmc.BuildFlags;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.styna.stynastmc.config.ConfigKey;
import net.styna.stynastmc.freecam.Freecam;
import net.styna.stynastmc.tweaks.TweaksConfig;

/** Moves only the render camera. The player gets no input, rotation or interactions, so servers see someone idle. */
public final class PhotoMode {
    public static final KeyMapping KEY = new KeyMapping("key.stynastmc.photomode", InputConstants.KEY_F10, ConfigKey.CATEGORY);

    public enum Weather {
        SERVER, CLEAR, RAIN, THUNDER;

        public Component label() {
            return Component.translatable("stynastmc.photomode.weather." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double RADIUS = 0.15;

    private static boolean active;
    private static boolean pendingShot;
    private static ClientLevel level;
    private static double x;
    private static double y;
    private static double z;
    private static float yaw;
    private static float pitch;
    private static double speed = 8.0;
    private static long lastFrame;
    private static boolean rememberedHideGui;
    private static boolean uiHidden;
    private static CameraType rememberedCamera;

    private static int time = -1;
    private static Weather weather = Weather.SERVER;
    private static Boolean fog;
    private static Component status;

    private PhotoMode() {
    }

    public static void guardHud(Minecraft mc) {
        if (active) {
            setHudHidden(mc, true);
        }
    }

    public static boolean uiHidden() {
        return uiHidden;
    }

    public static boolean isActive() {
        return active;
    }

    public static void tick(Minecraft mc) {
        if (active) {
            if (mc.player == null || mc.level != level || mc.player.isDeadOrDying()) {
                exit();
            }
            return;
        }
        while (KEY.consumeClick()) {
            enter(mc);
        }
    }

    /** Replaces vanilla key handling while active, draining every binding so nothing reaches the server. */
    public static boolean handleKeybinds(Minecraft mc) {
        if (!active) {
            return false;
        }
        Options options = mc.options;
        boolean leave = drain(KEY);
        boolean panel = drain(options.keyAttack);
        // F1 is a key mapping now, so it gets drained below; it toggles the hint instead of the HUD.
        if (drain(options.keyToggleGui)) {
            uiHidden = !uiHidden;
        }
        for (KeyMapping key : options.keyMappings) {
            drain(key);
        }
        if (leave) {
            exit();
        } else if (panel) {
            mc.gui.setScreen(new PhotoScreen());
        }
        return true;
    }

    private static boolean drain(KeyMapping key) {
        boolean clicked = false;
        while (key.consumeClick()) {
            clicked = true;
        }
        return clicked;
    }

    private static void enter(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.gameMode == null || BuildFlags.FREECAM && Freecam.isEnabled()) {
            return;
        }
        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 position = camera.position();
        x = position.x;
        y = position.y;
        z = position.z;
        yaw = camera.yRot();
        pitch = camera.xRot();
        level = mc.level;
        lastFrame = System.nanoTime();

        mc.gameMode.stopDestroyBlock();
        if (mc.player.isUsingItem()) {
            mc.gameMode.releaseUsingItem(mc.player);
        }
        ClientInput idle = new ClientInput();
        idle.keyPresses = new Input(false, false, false, false, false, mc.player.input.keyPresses.shift(), false);
        mc.player.input = idle;

        rememberedHideGui = mc.gui.hud.isHidden();
        rememberedCamera = mc.options.getCameraType();
        setHudHidden(mc, true);
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        mc.smartCull = false;
        status = null;
        uiHidden = false;
        active = true;
    }

    public static void exit() {
        if (!active) {
            return;
        }
        active = false;
        Minecraft mc = Minecraft.getInstance();
        if (pendingShot) {
            pendingShot = false;
            mc.gameRenderer.setRenderBlockOutline(true);
        }
        mc.smartCull = true;
        setHudHidden(mc, rememberedHideGui);
        mc.options.setCameraType(rememberedCamera);
        if (mc.player != null) {
            mc.player.input = new KeyboardInput(mc.options);
        }
        if (mc.gui.screen() instanceof PhotoScreen) {
            mc.gui.setScreen(null);
        }
        level = null;
    }

    private static void setHudHidden(Minecraft mc, boolean hidden) {
        if (mc.gui.hud.isHidden() != hidden) {
            mc.gui.hud.toggle();
        }
    }

    public static void turn(double dx, double dy) {
        yaw += (float) dx * 0.15F;
        pitch = Mth.clamp(pitch + (float) dy * 0.15F, -90.0F, 90.0F);
    }

    public static void scroll(double amount) {
        speed = Mth.clamp(amount > 0 ? speed * 1.2 : speed / 1.2, 0.5, 64.0);
    }

    /** Runs once per rendered frame, so movement stays smooth at any tick rate. */
    public static void stepFrame() {
        long now = System.nanoTime();
        double dt = Math.min((now - lastFrame) / 1e9, 0.1);
        lastFrame = now;
        Options options = Minecraft.getInstance().options;
        double forward = axis(options.keyUp, options.keyDown);
        double strafe = axis(options.keyLeft, options.keyRight);
        double up = axis(options.keyJump, options.keyShift);
        if (forward == 0 && strafe == 0 && up == 0) {
            return;
        }
        double length = Math.max(1.0, Math.sqrt(forward * forward + strafe * strafe));
        double distance = speed * dt * (options.keySprint.isDown() ? 3.0 : 1.0);
        double sin = Math.sin(Math.toRadians(yaw));
        double cos = Math.cos(Math.toRadians(yaw));
        move((strafe * cos - forward * sin) / length * distance, up * distance, (forward * cos + strafe * sin) / length * distance);
    }

    private static double axis(KeyMapping positive, KeyMapping negative) {
        return (positive.isDown() ? 1 : 0) - (negative.isDown() ? 1 : 0);
    }

    private static void move(double dx, double dy, double dz) {
        AABB box = new AABB(x - RADIUS, y - RADIUS, z - RADIUS, x + RADIUS, y + RADIUS, z + RADIUS);
        List<VoxelShape> shapes = new ArrayList<>();
        level.getBlockCollisions(null, box.expandTowards(dx, dy, dz)).forEach(shapes::add);
        dy = Shapes.collide(Direction.Axis.Y, box, shapes, dy);
        box = box.move(0, dy, 0);
        dx = Shapes.collide(Direction.Axis.X, box, shapes, dx);
        box = box.move(dx, 0, 0);
        dz = Shapes.collide(Direction.Axis.Z, box, shapes, dz);
        x += dx;
        y += dy;
        z += dz;
    }

    public static double x() {
        return x;
    }

    public static double y() {
        return y;
    }

    public static double z() {
        return z;
    }

    public static float yaw() {
        return yaw;
    }

    public static float pitch() {
        return pitch;
    }

    public static int time() {
        return time;
    }

    public static void setTime(int ticks) {
        time = ticks;
    }

    public static long timeOverride() {
        return active ? time : -1;
    }

    public static Weather weather() {
        return weather;
    }

    public static void cycleWeather() {
        weather = Weather.values()[(weather.ordinal() + 1) % Weather.values().length];
    }

    public static float rainOverride() {
        if (!active || weather == Weather.SERVER) {
            return -1.0F;
        }
        return weather == Weather.CLEAR ? 0.0F : 1.0F;
    }

    public static float thunderOverride() {
        if (!active || weather == Weather.SERVER) {
            return -1.0F;
        }
        return weather == Weather.THUNDER ? 1.0F : 0.0F;
    }

    public static boolean fog() {
        return fog != null ? fog : !TweaksConfig.on(TweaksConfig.NO_FOG);
    }

    public static void setFog(boolean on) {
        fog = on;
    }

    public static Component status() {
        return status;
    }

    /** Grabbed from the next normal frame. A separate resized render desyncs Iris and Distant Horizons buffers. */
    public static void shoot() {
        if (active && !pendingShot) {
            pendingShot = true;
            Minecraft.getInstance().gameRenderer.setRenderBlockOutline(false);
        }
    }

    /** Runs after the level is drawn and before the GUI, so neither the hint nor the panel ends up in the shot. */
    public static void afterLevel() {
        if (!pendingShot) {
            return;
        }
        pendingShot = false;
        Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.setRenderBlockOutline(true);
        try {
            Screenshot.grab(mc.gameDirectory, nextName(mc), mc.gameRenderer.mainRenderTarget(), 1, message -> mc.execute(() -> saved(mc, message)));
        } catch (Exception e) {
            LOGGER.error("Photo mode capture failed", e);
            status = Component.translatable("stynastmc.photomode.failed");
        }
    }

    private static void saved(Minecraft mc, Component message) {
        status = message;
        mc.gui.hud.getChat().addClientSystemMessage(message);
    }

    private static String nextName(Minecraft mc) {
        String base = "photo_" + Util.getFilenameFormattedDateTime();
        File folder = new File(mc.gameDirectory, Screenshot.SCREENSHOT_DIR);
        String name = base + ".png";
        for (int i = 2; new File(folder, name).exists(); i++) {
            name = base + "_" + i + ".png";
        }
        return name;
    }
}
