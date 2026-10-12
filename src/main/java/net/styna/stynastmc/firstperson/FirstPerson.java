package net.styna.stynastmc.firstperson;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.styna.stynastmc.config.ConfigKey;
import net.styna.stynastmc.firstperson.FirstPersonConfig.VanillaHands;
import net.styna.stynastmc.freecam.Freecam;
import net.styna.stynastmc.mixin.ItemRendererAccessor;

/** Renders your own body in first person, after FirstPersonModel by tr7zw. */
public final class FirstPerson {
    public static final KeyBinding KEY = new KeyBinding("key.stynastmc.firstperson", Keyboard.KEY_F6, ConfigKey.CATEGORY);

    private static final float SNEAK_BODY_OFFSET = 0.27f;
    private static final float IN_VEHICLE_BODY_OFFSET = 0.20f;

    private static Boolean enabled;
    /** True while the body is drawn, so its head, helmet and hidden items get stripped. */
    public static boolean renderingPlayer;
    public static boolean hideArms;
    public static float armOffset;
    private static double offsetX;
    private static double offsetZ;
    private static long timeout;
    private static final Set<Item> autoVanillaHandItems = new HashSet<>();
    private static final Set<Item> autoDisableItems = new HashSet<>();
    private static List<? extends String> loadedVanilla;
    private static List<? extends String> loadedDisable;

    private FirstPerson() {
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
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

    public static void tick() {
        while (KEY.isPressed()) {
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
            Item item = Item.REGISTRY.getObject(new ResourceLocation(id.trim()));
            if (item != null && item != Items.AIR) {
                out.add(item);
            }
        }
    }

    /** Whether the body should be drawn this frame. */
    public static boolean shouldApply() {
        EntityPlayerSP player = mc().player;
        if (!isEnabled() || player == null || player.isSpectator() || Freecam.isEnabled()
                || mc().getRenderViewEntity() != player || mc().gameSettings.thirdPersonView != 0) {
            return false;
        }
        if (player.isPlayerSleeping() || player.isElytraFlying()) {
            // Brief grace period avoids a flicker when landing with an elytra.
            timeout = System.currentTimeMillis() + 100;
            return false;
        }
        if (System.currentTimeMillis() < timeout) {
            return false;
        }
        return !autoDisableItems.contains(player.getHeldItemMainhand().getItem())
                && !autoDisableItems.contains(player.getHeldItemOffhand().getItem());
    }

    /** Draws the local player shifted behind the camera, like in third person. Called during the entity pass. */
    public static void renderBody(float partialTicks) {
        if (!shouldApply()) {
            return;
        }
        EntityPlayerSP player = mc().player;
        updateOffset(player, partialTicks);
        hideArms = hideArmsAndItems(player);
        armOffset = !hideArms && dynamicHandsEnabled() ? MathHelper.clamp(-player.rotationPitch / 20 + 2, 0.0f, 0.7f) : 0;
        double posX = player.posX, prevPosX = player.prevPosX, lastX = player.lastTickPosX;
        double posZ = player.posZ, prevPosZ = player.prevPosZ, lastZ = player.lastTickPosZ;
        player.posX += offsetX;
        player.prevPosX += offsetX;
        player.lastTickPosX += offsetX;
        player.posZ += offsetZ;
        player.prevPosZ += offsetZ;
        player.lastTickPosZ += offsetZ;
        renderingPlayer = true;
        try {
            mc().getRenderManager().renderEntityStatic(player, partialTicks, false);
        } finally {
            renderingPlayer = false;
            player.posX = posX;
            player.prevPosX = prevPosX;
            player.lastTickPosX = lastX;
            player.posZ = posZ;
            player.prevPosZ = prevPosZ;
            player.lastTickPosZ = lastZ;
        }
    }

    public static double offsetX() {
        return offsetX;
    }

    public static double offsetZ() {
        return offsetZ;
    }

    private static void updateOffset(EntityPlayer player, float delta) {
        offsetX = 0;
        offsetZ = 0;
        float realYaw = rotLerp(delta, player.prevRenderYawOffset, player.renderYawOffset);
        float bodyOffset;
        if (player.isSneaking()) {
            bodyOffset = SNEAK_BODY_OFFSET + FirstPersonConfig.SNEAK_X_OFFSET.get() / 100f;
        } else if (player.isRiding()) {
            Entity vehicle = player.getRidingEntity();
            if (!(vehicle instanceof EntityBoat || vehicle instanceof EntityMinecart) && vehicle instanceof EntityLivingBase living) {
                realYaw = bodyRotOnMount(rotLerp(delta, living.prevRenderYawOffset, living.renderYawOffset), player.rotationYaw);
            }
            bodyOffset = IN_VEHICLE_BODY_OFFSET + FirstPersonConfig.SIT_X_OFFSET.get() / 100f;
        } else {
            bodyOffset = 0.25f + FirstPersonConfig.X_OFFSET.get() / 100f;
        }
        offsetX = bodyOffset * Math.sin(Math.toRadians(realYaw));
        offsetZ = -bodyOffset * Math.cos(Math.toRadians(realYaw));
    }

    private static float rotLerp(float delta, float from, float to) {
        return from + delta * MathHelper.wrapDegrees(to - from);
    }

    // Mounts drag the rider's body along once the head turns more than 50 degrees away.
    private static float bodyRotOnMount(float mountBodyRot, float riderHeadRot) {
        float head = MathHelper.wrapDegrees(riderHeadRot);
        float diff = MathHelper.wrapDegrees(head - mountBodyRot);
        if (Math.abs(diff) > 50.0F) {
            mountBodyRot = head - 50.0F * Math.signum(diff);
        }
        return MathHelper.wrapDegrees(mountBodyRot);
    }

    private static VanillaHands mode() {
        return loaded() ? FirstPersonConfig.VANILLA_HANDS.get() : VanillaHands.OFF;
    }

    private static boolean forcedVanilla(ItemStack main, ItemStack off) {
        return autoVanillaHandItems.contains(main.getItem()) || autoVanillaHandItems.contains(off.getItem())
                || autoDisableItems.contains(main.getItem()) || autoDisableItems.contains(off.getItem());
    }

    /** Keep drawing the vanilla first person hands. */
    public static boolean showVanillaHands(EntityPlayer player) {
        ItemStack main = player.inventory.getCurrentItem();
        ItemStack off = player.inventory.offHandInventory.get(0);
        VanillaHands mode = mode();
        return mode == VanillaHands.ALL || mode == VanillaHands.ALL_DOUBLE
                || (mode == VanillaHands.ITEMS && (!main.isEmpty() || !off.isEmpty())) || forcedVanilla(main, off);
    }

    /** Hide the body's arms and held items because the vanilla hands take over. */
    public static boolean hideArmsAndItems(EntityPlayer player) {
        if (lookingDown(player.rotationPitch)) {
            return false;
        }
        return mode() != VanillaHands.OFF || forcedVanilla(player.inventory.getCurrentItem(), player.inventory.offHandInventory.get(0));
    }

    public static boolean dynamicHandsEnabled() {
        EntityPlayerSP player = mc().player;
        return player != null && FirstPersonConfig.DYNAMIC_MODE.get() && mode() != VanillaHands.OFF
                && !forcedVanilla(player.inventory.getCurrentItem(), player.inventory.offHandInventory.get(0));
    }

    public static boolean lookingDown(float pitch) {
        return dynamicHandsEnabled() && pitch > 30;
    }

    public static void onRenderHand(RenderSpecificHandEvent event) {
        EntityPlayerSP player = mc().player;
        if (!shouldApply()) {
            return;
        }
        if (!showVanillaHands(player) || dynamicHandsEnabled() && event.getInterpolatedPitch() > 35) {
            event.setCanceled(true);
            return;
        }
        // Vanilla only draws the empty main arm, ALL_DOUBLE adds the empty off arm too.
        if (mode() == VanillaHands.ALL_DOUBLE && event.getHand() == EnumHand.OFF_HAND && event.getItemStack().isEmpty()
                && player.getHeldItemMainhand().getItem() != Items.FILLED_MAP && !player.isInvisible()) {
            EnumHandSide side = player.getPrimaryHand().opposite();
            GlStateManager.pushMatrix();
            ((ItemRendererAccessor) mc().getItemRenderer()).stynastmc$renderArm(event.getEquipProgress(), event.getSwingProgress(), side);
            GlStateManager.popMatrix();
        }
    }
}
