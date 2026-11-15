package net.styna.stynastmc.tweaks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.styna.stynastmc.StynaSTMC;

/** Puts icons of the gear an enchantment goes on before its name. Glyphs take the name's color. */
public final class EnchantIcons {
    private static final char ARMOR_SET = '\uE900';
    private static final char FIRST_ARMOR = '\uE901';
    private static final char FIRST_TOOL = '\uE905';
    private static final char ANY = '\uE90A';
    private static final char FIRST_OTHER_GEAR = '\uE90B';
    private static final char MISC = '\uE911';
    private static final FontDescription FONT = new FontDescription.Resource(StynaSTMC.id("enchant_icons"));
    private static final ItemStack[] ARMOR = {
            new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE),
            new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS)};
    private static final ItemStack[] TOOLS = {
            new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.DIAMOND_AXE),
            new ItemStack(Items.DIAMOND_SHOVEL), new ItemStack(Items.DIAMOND_HOE)};
    private static final ItemStack[] OTHER_GEAR = {
            new ItemStack(Items.BOW), new ItemStack(Items.FISHING_ROD), new ItemStack(Items.CROSSBOW),
            new ItemStack(Items.TRIDENT), new ItemStack(Items.MACE), new ItemStack(Items.DIAMOND_SPEAR)};

    private EnchantIcons() {
    }

    public static Component prefix(Holder<Enchantment> enchantment, Component name) {
        Component colored = enchantment.is(EnchantmentTags.CURSE) ? name : name.copy().withStyle(color(enchantment.value().getWeight()));
        return Component.empty().append(Component.literal(glyphs(enchantment.value())).withStyle(colored.getStyle().withFont(FONT))).append(" ").append(colored);
    }

    // Enchantment weight is rarity: 10 common, 5 uncommon, 2 rare, 1 very rare.
    private static ChatFormatting color(int weight) {
        return weight >= 10 ? ChatFormatting.GOLD : weight >= 5 ? ChatFormatting.DARK_GREEN : weight >= 2 ? ChatFormatting.AQUA : ChatFormatting.LIGHT_PURPLE;
    }

    // Full armor, or armor plus tools, collapses to one icon so Protection and Unbreaking stay short.
    private static String glyphs(Enchantment enchantment) {
        String armor = matching(enchantment, ARMOR, FIRST_ARMOR);
        String tools = matching(enchantment, TOOLS, FIRST_TOOL);
        boolean allArmor = armor.length() == ARMOR.length;
        if (allArmor && tools.length() == TOOLS.length) {
            return String.valueOf(ANY);
        }
        String glyphs = (allArmor ? String.valueOf(ARMOR_SET) : armor) + tools + matching(enchantment, OTHER_GEAR, FIRST_OTHER_GEAR);
        return glyphs.isEmpty() ? String.valueOf(MISC) : glyphs;
    }

    private static String matching(Enchantment enchantment, ItemStack[] probes, char firstGlyph) {
        StringBuilder glyphs = new StringBuilder();
        for (int i = 0; i < probes.length; i++) {
            if (enchantment.isSupportedItem(probes[i])) {
                glyphs.append((char) (firstGlyph + i));
            }
        }
        return glyphs.toString();
    }
}
