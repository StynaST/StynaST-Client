package net.styna.stynastmc.firstperson;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.styna.stynastmc.config.ConfigKey;
import net.styna.stynastmc.firstperson.FirstPersonConfig.VanillaHands;
import net.styna.stynastmc.freecam.Freecam;

/** Renders your own body in first person. Port of FirstPersonModel by tr7zw. */
public final class FirstPerson {
    public static final KeyMapping KEY = new KeyMapping("key.stynastmc.firstperson", InputConstants.KEY_F6, ConfigKey.CATEGORY);

    private static final float SNEAK_BODY_OFFSET = 0.27f;
    private static final float SWIM_UP_BODY_OFFSET = 0.60f;
    private static final float SWIM_DOWN_BODY_OFFSET = 0.50f;
    private static final float IN_VEHICLE_BODY_OFFSET = 0.20f;

    private static Boolean enabled;
    /** True while the camera entity is extracted by us, so its head, helmet and hidden items get stripped. */
    public static boolean renderingPlayer;
    private static Vec3 offset = Vec3.ZERO;
    private static long timeout;
    private static final Set<Item> autoVanillaHandItems = new HashSet<>();
    private static final Set<Item> autoDisableItems = new HashSet<>();
    private static List<? extends String> loadedVanilla;
    private static List<? extends String> loadedDisable;

    private FirstPerson() {
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static boolean loaded() {
        return FirstPersonConfig.SPEC.isLoaded();
    }

    public static boolean isEnabled() {
        if (enabled == null) {
            if (!loaded()) {
                return false;
            }
            enabled = FirstPersonConfig.ENABLED_BY_DEFAULT.get();
        }
        return enabled;
    }

    public static void tick(Minecraft minecraft) {
        while (KEY.consumeClick()) {
            enabled = !isEnabled();
        }
        if (loaded() && (loadedVanilla != FirstPersonConfig.AUTO_VANILLA_HANDS.get()
                || loadedDisable != FirstPersonConfig.AUTO_DISABLE.get())) {
            loadedVanilla = FirstPersonConfig.AUTO_VANILLA_HANDS.get();
            loadedDisable = FirstPersonConfig.AUTO_DISABLE.get();
            readItems(loadedVanilla, autoVanillaHandItems);
            readItems(loadedDisable, autoDisableItems);
        }
    }

    private static void readItems(List<? extends String> ids, Set<Item> out) {
        out.clear();
        for (String id : ids) {
            Identifier key = Identifier.tryParse(id.trim());
            if (key != null) {
                BuiltInRegistries.ITEM.getOptional(key).ifPresent(out::add);
            }
        }
    }

    /** Whether the body should be drawn this frame. */
    public static boolean shouldApply() {
        LocalPlayer player = mc().player;
        if (!isEnabled() || player == null || player.isSpectator() || Freecam.isEnabled()
                || mc().getCameraEntity() != player || mc().options.getCameraType() != CameraType.FIRST_PERSON) {
            return false;
        }
        if (player.isSleeping() || player.isAutoSpinAttack() || player.isFallFlying()
                || (player.getSwimAmount(1f) != 0 && !player.isVisuallySwimming())) {
            // Brief grace period avoids a flicker when landing with an elytra.
            timeout = System.currentTimeMillis() + 100;
            return false;
        }
        if (System.currentTimeMillis() < timeout) {
            return false;
        }
        return !player.isScoping() && !holds(player, autoDisableItems);
    }

    private static boolean holds(Player player, Set<Item> items) {
        return items.contains(player.getInventory().getSelectedItem().getItem())
                || items.contains(player.getInventory().getItem(Inventory.SLOT_OFFHAND).getItem());
    }

    public static Vec3 offset() {
        return offset;
    }

    /** X/Z shift that puts the body behind the camera, like in third person. */
    public static void updateOffset(Entity entity, float delta) {
        offset = Vec3.ZERO;
        if (!(entity instanceof Player player) || player.isSleeping()) {
            return;
        }
        double x = 0;
        double y = 0;
        double z = 0;
        float realYaw = Mth.rotLerp(delta, player.yBodyRotO, player.yBodyRot);
        float bodyOffset;
        if (player.isVisuallySwimming()) {
            player.yBodyRot = player.yHeadRot;
            bodyOffset = player.xRotO > 0 ? SWIM_UP_BODY_OFFSET : SWIM_DOWN_BODY_OFFSET;
        } else if (player.isCrouching() || player.getPose() == Pose.CROUCHING) {
            bodyOffset = SNEAK_BODY_OFFSET + FirstPersonConfig.SNEAK_X_OFFSET.get() / 100f;
        } else if (player.isPassenger()) {
            if (!(player.getVehicle() instanceof Boat || player.getVehicle() instanceof Minecart)
                    && player.getVehicle() instanceof LivingEntity living) {
                realYaw = bodyRotOnMount(Mth.rotLerp(delta, living.yBodyRotO, living.yBodyRot), player.getYRot());
            }
            bodyOffset = IN_VEHICLE_BODY_OFFSET + FirstPersonConfig.SIT_X_OFFSET.get() / 100f;
        } else {
            bodyOffset = 0.25f + FirstPersonConfig.X_OFFSET.get() / 100f;
        }
        x += bodyOffset * Math.sin(Math.toRadians(realYaw));
        z -= bodyOffset * Math.cos(Math.toRadians(realYaw));
        if (player.isVisuallySwimming()) {
            if (player.xRotO > 0 && player.isUnderWater()) {
                y += 0.6f * Math.sin(Math.toRadians(player.xRotO));
            } else {
                y += 0.01f * -Math.sin(Math.toRadians(player.xRotO));
            }
        }
        offset = new Vec3(x, y, z);
    }

    // Mounts drag the rider's body along once the head turns more than 50 degrees away.
    private static float bodyRotOnMount(float mountBodyRot, float riderHeadRot) {
        float head = Mth.wrapDegrees(riderHeadRot);
        float diff = Mth.wrapDegrees(head - mountBodyRot);
        if (Mth.abs(diff) > 50.0F) {
            mountBodyRot = head - 50.0F * Math.signum(diff);
        }
        return Mth.wrapDegrees(mountBodyRot);
    }

    private static VanillaHands mode() {
        return loaded() ? FirstPersonConfig.VANILLA_HANDS.get() : VanillaHands.OFF;
    }

    private static boolean forcedVanilla(ItemStack main, ItemStack off) {
        return autoVanillaHandItems.contains(main.getItem()) || autoVanillaHandItems.contains(off.getItem())
                || autoDisableItems.contains(main.getItem()) || autoDisableItems.contains(off.getItem());
    }

    private static ItemStack mainHand(Player player) {
        return player.getInventory().getSelectedItem();
    }

    private static ItemStack offHand(Player player) {
        return player.getInventory().getItem(Inventory.SLOT_OFFHAND);
    }

    /** Keep drawing the vanilla first person hands. */
    public static boolean showVanillaHands() {
        LocalPlayer player = mc().player;
        if (player == null) {
            return false;
        }
        ItemStack main = mainHand(player);
        ItemStack off = offHand(player);
        VanillaHands mode = mode();
        return mode == VanillaHands.ALL || mode == VanillaHands.ALL_DOUBLE
                || (mode == VanillaHands.ITEMS && (!main.isEmpty() || !off.isEmpty())) || forcedVanilla(main, off);
    }

    /** Hide the body's arms and held items because the vanilla hands take over. */
    public static boolean hideArmsAndItems(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (FirstPersonConfig.SKIP_SWIMMING.get() && player.isSwimming()) {
            return false;
        }
        if (lookingDown(entity.getXRot())) {
            return false;
        }
        return mode() != VanillaHands.OFF || forcedVanilla(mainHand(player), offHand(player));
    }

    public static boolean dynamicHandsEnabled() {
        LocalPlayer player = mc().player;
        if (player == null || FirstPersonConfig.SKIP_SWIMMING.get() && player.isSwimming()) {
            return false;
        }
        return FirstPersonConfig.DYNAMIC_MODE.get() && mode() != VanillaHands.OFF && !forcedVanilla(mainHand(player), offHand(player));
    }

    public static boolean lookingDown(float xRot) {
        return dynamicHandsEnabled() && xRot > 30;
    }
}
