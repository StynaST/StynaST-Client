package net.styna.stynastmc.zoom;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.styna.stynastmc.config.ConfigKey;
import net.styna.stynastmc.zoom.config.ZoomConfig;

/** Hold-to-zoom in the style of WI Zoom. Works with any camera entity, including Freecam. */
public final class Zoom {
    public static final KeyMapping KEY = new KeyMapping("key.stynastmc.zoom", InputConstants.KEY_V, ConfigKey.CATEGORY);

    private static double level = 1.0;
    private static double current = 1.0;
    private static long lastFrame;

    private Zoom() {
    }

    private static boolean active() {
        return KEY.isDown() && ZoomConfig.SPEC.isLoaded();
    }

    /** Called once per frame from the camera with the vanilla FOV. */
    public static float apply(float fov) {
        boolean zooming = active();
        if (!zooming) {
            level = ZoomConfig.SPEC.isLoaded() ? ZoomConfig.DEFAULT_LEVEL.get() : 3.0;
        }
        double target = zooming ? level : 1.0;

        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : Math.min((now - lastFrame) / 1e9, 0.1);
        lastFrame = now;
        if (ZoomConfig.SPEC.isLoaded() && ZoomConfig.SMOOTH.get()) {
            // Frame rate independent ease toward the target.
            current += (target - current) * (1 - Math.exp(-dt * ZoomConfig.SMOOTH_SPEED.get()));
            if (Math.abs(target - current) < 0.001) {
                current = target;
            }
        } else {
            current = target;
        }
        return (float) (fov / current);
    }

    public static boolean onScroll(double scrollY) {
        if (!active() || !ZoomConfig.SCROLL.get() || scrollY == 0) {
            return false;
        }
        double step = ZoomConfig.SCROLL_STEP.get();
        level = Mth.clamp(scrollY > 0 ? level * step : level / step, 1.0, ZoomConfig.MAX_LEVEL.get());
        return true;
    }

    public static double sensitivity(double sensitivity) {
        if (current <= 1.0 || !ZoomConfig.SPEC.isLoaded() || !ZoomConfig.SCALE_SENSITIVITY.get()) {
            return sensitivity;
        }
        // Vanilla turns by (s * 0.6 + 0.2)^3, so scale inside the cube root to slow turning by the zoom factor.
        double ss = (sensitivity * 0.6 + 0.2) / Math.cbrt(current);
        return (ss - 0.2) / 0.6;
    }

    public static boolean cinematicCamera() {
        return current > 1.0 && ZoomConfig.SPEC.isLoaded() && active() && ZoomConfig.CINEMATIC_CAMERA.get();
    }
}
