package net.styna.stynastmc.tweaks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;

/** Worn armor drawn beside the hotbar, on the side away from the offhand slot. */
public final class ArmorHud {
    private static final EntityEquipmentSlot[] SLOTS = {EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS,
            EntityEquipmentSlot.FEET};

    private ArmorHud() {
    }

    public static void render(EntityPlayer player, int width, int height) {
        if (!TweaksConfig.on(TweaksConfig.ARMOR_HUD)) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean offhandLeft = player.getPrimaryHand().opposite() == EnumHandSide.LEFT;
        int center = width / 2;
        int y = height - 19;
        int step = offhandLeft ? 18 : -18;
        // Leave room for the hotbar attack indicator, which shares this side.
        int gap = 6;
        if (minecraft.gameSettings.attackIndicator == 2) {
            gap += 22;
        }
        int x = offhandLeft ? center + 91 + gap : center - 91 - gap - 16;
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableGUIStandardItemLighting();
        for (EntityEquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            minecraft.getRenderItem().renderItemAndEffectIntoGUI(player, stack, x, y);
            minecraft.getRenderItem().renderItemOverlays(minecraft.fontRenderer, stack, x, y);
            x += step;
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
    }
}
