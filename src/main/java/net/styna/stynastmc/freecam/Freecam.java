package net.styna.stynastmc.freecam;

import java.util.Arrays;
import java.util.List;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraft.util.text.TextComponentTranslation;
import net.styna.stynastmc.freecam.config.FreecamConfig;
import net.styna.stynastmc.freecam.tripod.TripodRegistry;
import net.styna.stynastmc.freecam.tripod.TripodSlot;
import net.styna.stynastmc.freecam.util.FreeCamera;
import net.styna.stynastmc.freecam.util.FreecamPosition;

/** Freecam with tripods, after the Freecam mod by MinecraftFreecam. Every key starts unbound here. */
public final class Freecam {
    public static final String CATEGORY = "key.category.freecam.controls";
    public static final KeyBinding KEY_TOGGLE = new KeyBinding("key.freecam.toggle", Keyboard.KEY_NONE, CATEGORY);
    public static final KeyBinding KEY_CONTROL_PLAYER = new KeyBinding("key.freecam.controlPlayer.toggle", Keyboard.KEY_NONE, CATEGORY);
    public static final KeyBinding KEY_TRIPOD_RESET = new KeyBinding("key.freecam.tripod.reset", Keyboard.KEY_NONE, CATEGORY);
    public static final KeyBinding KEY_OUTLINE = new KeyBinding("key.freecam.outlinePlayer.toggle", Keyboard.KEY_NONE, CATEGORY);
    // Tapping toggle switches freecam, holding it plus a hotbar key opens a tripod.
    private static final int MAX_TAP_TICKS = 10;

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final TripodRegistry tripods = new TripodRegistry();

    private static boolean freecamEnabled;
    private static boolean tripodEnabled;
    private static boolean playerControlEnabled;
    private static boolean outlineEnabled;
    private static TripodSlot activeTripod = TripodSlot.NONE;
    private static FreeCamera freeCamera;
    private static int rememberedView = -1;
    private static float lastHealth = -1;

    private static boolean toggleWasDown;
    private static int toggleHeld;
    private static boolean toggleUsed;

    private Freecam() {
    }

    public static List<KeyBinding> keys() {
        return Arrays.asList(KEY_TOGGLE, KEY_CONTROL_PLAYER, KEY_TRIPOD_RESET, KEY_OUTLINE);
    }

    public static void preTick() {
        if (MC.world == null || MC.player == null) {
            if (isEnabled()) {
                disableQuietly();
            }
            tripods.clear();
            lastHealth = -1;
            return;
        }
        if (isEnabled() && (freeCamera.world != MC.world || shouldDisableOnDamage())) {
            disableQuietly();
        }
        lastHealth = MC.player.getHealth();
        tickKeys();
        if (!isEnabled()) {
            return;
        }
        MC.gameSettings.thirdPersonView = 0;
        if (!playerControlEnabled && MC.player.movementInput instanceof MovementInputFromOptions) {
            MC.player.movementInput = new MovementInput();
        }
        freeCamera.tickCamera();
    }

    private static boolean shouldDisableOnDamage() {
        return FreecamConfig.DISABLE_ON_DAMAGE.get() && !MC.player.isCreative() && lastHealth >= 0 && MC.player.getHealth() < lastHealth;
    }

    private static void tickKeys() {
        boolean down = KEY_TOGGLE.isKeyDown();
        if (down) {
            if (!toggleWasDown) {
                toggleHeld = 0;
                toggleUsed = false;
            }
            toggleHeld++;
            if (activateTripodHandler()) {
                toggleUsed = true;
            }
        } else if (toggleWasDown && !toggleUsed && toggleHeld < MAX_TAP_TICKS) {
            toggle();
        }
        toggleWasDown = down;
        drain(KEY_TOGGLE);

        while (KEY_CONTROL_PLAYER.isPressed()) {
            switchControls();
        }
        if (KEY_TRIPOD_RESET.isKeyDown()) {
            resetTripodHandler();
        }
        drain(KEY_TRIPOD_RESET);
        while (KEY_OUTLINE.isPressed()) {
            toggleOutline();
        }
    }

    private static void drain(KeyBinding key) {
        while (key.isPressed()) {
            // Presses are handled through isKeyDown.
        }
    }

    // Runs before vanilla handles hotbar keys, so consuming them here keeps the selected slot.
    private static boolean activateTripodHandler() {
        boolean activated = false;
        for (int i = 0; i < MC.gameSettings.keyBindsHotbar.length; i++) {
            while (MC.gameSettings.keyBindsHotbar[i].isPressed()) {
                toggleTripod(TripodSlot.ofHotbar(i));
                activated = true;
            }
        }
        return activated;
    }

    private static void resetTripodHandler() {
        for (int i = 0; i < MC.gameSettings.keyBindsHotbar.length; i++) {
            while (MC.gameSettings.keyBindsHotbar[i].isPressed()) {
                resetCamera(TripodSlot.ofHotbar(i));
            }
        }
    }

    public static void toggle() {
        if (tripodEnabled) {
            toggleTripod(activeTripod);
            return;
        }
        if (freecamEnabled) {
            onDisable();
            notify(FreecamConfig.NOTIFY_FREECAM.get(), "freecam.msg.disabled");
        } else {
            onEnable();
            freeCamera = new FreeCamera(MC.world);
            moveToPlayer();
            MC.setRenderViewEntity(freeCamera);
            notify(FreecamConfig.NOTIFY_FREECAM.get(), "freecam.msg.enabled");
        }
        freecamEnabled = !freecamEnabled;
        if (!freecamEnabled) {
            onDisabled();
        }
    }

    private static void toggleTripod(TripodSlot tripod) {
        if (tripod == TripodSlot.NONE) {
            return;
        }
        if (tripodEnabled) {
            onDisableTripod();
            if (activeTripod == tripod) {
                tripodEnabled = false;
            } else {
                onEnableTripod(tripod);
            }
        } else {
            if (freecamEnabled) {
                toggle();
            }
            onEnableTripod(tripod);
            tripodEnabled = true;
        }
        if (!tripodEnabled) {
            activeTripod = TripodSlot.NONE;
            onDisabled();
        }
    }

    private static void onEnableTripod(TripodSlot tripod) {
        onEnable();
        FreecamPosition position = tripods.get(tripod);
        if (position != null && !MC.world.getChunkProvider().isChunkGeneratedAt(position.chunkX(), position.chunkZ())) {
            tripods.put(tripod, null);
            position = null;
        }
        freeCamera = new FreeCamera(MC.world);
        if (position == null) {
            moveToPlayer();
        } else {
            freeCamera.applyPosition(position);
        }
        MC.setRenderViewEntity(freeCamera);
        activeTripod = tripod;
        notify(FreecamConfig.NOTIFY_TRIPOD.get(), "freecam.msg.tripod.open", tripod);
    }

    private static void onDisableTripod() {
        tripods.put(activeTripod, new FreecamPosition(freeCamera));
        onDisable();
        notify(FreecamConfig.NOTIFY_TRIPOD.get(), "freecam.msg.tripod.close", activeTripod);
    }

    private static void onEnable() {
        MC.renderChunksMany = false;
        outlineEnabled = FreecamConfig.OUTLINE_PLAYER.get();
        rememberedView = MC.gameSettings.thirdPersonView;
        MC.gameSettings.thirdPersonView = 0;
    }

    private static void onDisable() {
        MC.renderChunksMany = true;
        if (MC.player != null) {
            MC.setRenderViewEntity(MC.player);
            MC.player.movementInput = new MovementInputFromOptions(MC.gameSettings);
        }
        playerControlEnabled = false;
        freeCamera = null;
    }

    private static void onDisabled() {
        if (rememberedView >= 0) {
            MC.gameSettings.thirdPersonView = rememberedView;
            rememberedView = -1;
        }
    }

    /** Leaves freecam without messages, for disconnects, world changes and damage. */
    private static void disableQuietly() {
        if (tripodEnabled && freeCamera != null && MC.world != null && freeCamera.world == MC.world) {
            tripods.put(activeTripod, new FreecamPosition(freeCamera));
        }
        onDisable();
        freecamEnabled = false;
        tripodEnabled = false;
        activeTripod = TripodSlot.NONE;
        onDisabled();
    }

    private static void resetCamera(TripodSlot tripod) {
        if (tripod == TripodSlot.NONE) {
            return;
        }
        if (tripodEnabled && activeTripod == tripod && freeCamera != null) {
            moveToPlayer();
        } else {
            tripods.put(tripod, null);
        }
        notify(FreecamConfig.NOTIFY_TRIPOD.get(), "freecam.msg.tripod.reset", tripod);
    }

    public static void switchControls() {
        if (!isEnabled()) {
            return;
        }
        if (playerControlEnabled) {
            freeCamera.input = new MovementInputFromOptions(MC.gameSettings);
            MC.player.movementInput = new MovementInput();
        } else {
            MC.player.movementInput = new MovementInputFromOptions(MC.gameSettings);
            freeCamera.input = new MovementInput();
        }
        playerControlEnabled = !playerControlEnabled;
    }

    public static void toggleOutline() {
        if (isEnabled()) {
            outlineEnabled = !outlineEnabled;
        }
    }

    private static void moveToPlayer() {
        freeCamera.copyPosition(MC.player);
        freeCamera.applyPerspective(FreecamConfig.PERSPECTIVE.get());
    }

    private static void notify(boolean enabled, String key, Object... args) {
        if (enabled && MC.player != null) {
            Object[] shown = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                shown[i] = String.valueOf(args[i]);
            }
            MC.player.sendStatusMessage(new TextComponentTranslation(key, shown), true);
        }
    }

    public static FreeCamera getFreeCamera() {
        return freeCamera;
    }

    public static boolean isEnabled() {
        return freecamEnabled || tripodEnabled;
    }

    public static boolean isOutlineEnabled() {
        return isEnabled() && outlineEnabled;
    }

    public static boolean isPlayerControlEnabled() {
        return playerControlEnabled;
    }

    /** Mouse look and movement go to the camera rather than the player. */
    public static boolean controlsCamera() {
        return isEnabled() && !playerControlEnabled;
    }

    public static boolean blocksInteraction() {
        return controlsCamera() && FreecamConfig.preventInteractions();
    }

    public static boolean interactFromPlayer() {
        return isEnabled() && (playerControlEnabled || FreecamConfig.interactFromPlayer());
    }

    public static boolean freezesPlayer() {
        return controlsCamera() && FreecamConfig.FREEZE_PLAYER.get();
    }
}
