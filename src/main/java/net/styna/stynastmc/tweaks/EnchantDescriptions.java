package net.styna.stynastmc.tweaks;

import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.styna.stynastmc.util.Text;

/** Description text comes from Enchantment Descriptions' lang files (assets/enchdesc/lang). */
public final class EnchantDescriptions {
    private static final String[] SUFFIXES = {"desc", "description", "info"};
    private static final int WRAP = 220;

    private EnchantDescriptions() {
    }

    private static boolean enabled() {
        return EnchantDescriptionsConfig.on(EnchantDescriptionsConfig.ENABLED);
    }

    private static boolean shiftSatisfied() {
        return !EnchantDescriptionsConfig.on(EnchantDescriptionsConfig.REQUIRE_SHIFT) || GuiScreen.isShiftKeyDown();
    }

    /** Puts each description right under its enchantment's tooltip line. */
    public static void addDescriptions(ItemStack stack, List<String> lines) {
        if (!enabled()) {
            return;
        }
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        if (enchantments.isEmpty()) {
            return;
        }
        if (!shiftSatisfied()) {
            lines.add("§8" + Text.tr("enchdesc.activate.message"));
            return;
        }
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            String description = find(entry.getKey(), entry.getValue());
            int index = lines.indexOf(entry.getKey().getTranslatedName(entry.getValue()));
            if (description == null || index < 0) {
                continue;
            }
            List<String> wrapped = Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(description, WRAP);
            for (int i = 0; i < wrapped.size(); i++) {
                lines.add(index + 1 + i, "§8 " + wrapped.get(i));
            }
        }
    }

    private static String find(Enchantment enchantment, int level) {
        ResourceLocation id = enchantment.getRegistryName();
        String found = null;
        if (id != null) {
            // 1.13 renamed sweeping to sweeping_edge, which is the key the lang files use.
            String path = id.getPath().equals("sweeping") ? "sweeping_edge" : id.getPath();
            found = find("enchantment." + id.getNamespace() + "." + path + ".", level);
        }
        return found != null ? found : find(enchantment.getName() + ".", level);
    }

    private static String find(String base, int level) {
        for (String suffix : SUFFIXES) {
            String key = base + suffix;
            if (Text.has(key)) {
                return Text.tr(key);
            }
            if (Text.has(key + "." + level)) {
                return Text.tr(key + "." + level);
            }
        }
        return null;
    }
}
