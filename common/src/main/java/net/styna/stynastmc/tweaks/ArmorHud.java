package net.styna.stynastmc.tweaks;

import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Worn armor drawn beside the hotbar, on the side away from the offhand slot. */
public final class ArmorHud {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private ArmorHud() {
    }

    public static void render(GuiGraphicsExtractor g, Player player) {
        if (!TweaksConfig.on(TweaksConfig.ARMOR_HUD)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean offhandLeft = player.getMainArm().getOpposite() == HumanoidArm.LEFT;
        int center = g.guiWidth() / 2;
        int y = g.guiHeight() - 19;
        int step = offhandLeft ? 18 : -18;
        // Leave room for the hotbar attack indicator, which shares this side.
        int gap = 6;
        if (minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
            gap += 22;
        }
        int x = offhandLeft ? center + 91 + gap : center - 91 - gap - 16;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            g.item(player, stack, x, y, 0);
            g.itemDecorations(minecraft.font, stack, x, y);
            x += step;
        }
    }
}
