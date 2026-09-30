package net.styna.stynastmc.tweaks;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;

/** Description text comes from Enchantment Descriptions' lang files (assets/enchdesc/lang). */
public final class EnchantDescriptions {
    private static final String[] SUFFIXES = {"desc", "description", "info"};

    private EnchantDescriptions() {
    }

    private static boolean enabled() {
        return EnchantDescriptionsConfig.on(EnchantDescriptionsConfig.ENABLED);
    }

    private static boolean shiftSatisfied() {
        return !EnchantDescriptionsConfig.on(EnchantDescriptionsConfig.REQUIRE_SHIFT) || Minecraft.getInstance().hasShiftDown();
    }

    public static void addDescription(Holder<Enchantment> enchantment, int level, Consumer<Component> lines) {
        if (!enabled() || !shiftSatisfied()) {
            return;
        }
        Component description = find(enchantment, level);
        if (description != null) {
            lines.accept(Component.literal(" ").append(description).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static void addShiftHint(ItemStack stack, List<Component> lines) {
        if (enabled() && !shiftSatisfied() && hasEnchantments(stack)) {
            lines.add(Component.translatable("enchdesc.activate.message").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static boolean hasEnchantments(ItemStack stack) {
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        return (enchantments != null && !enchantments.isEmpty()) || (stored != null && !stored.isEmpty());
    }

    private static @Nullable Component find(Holder<Enchantment> enchantment, int level) {
        Identifier id = enchantment.unwrapKey().map(key -> key.identifier()).orElse(null);
        Component found = id == null ? null : find("enchantment." + id.getNamespace() + "." + id.getPath() + ".", level);
        if (found == null && enchantment.value().description().getContents() instanceof TranslatableContents translatable) {
            found = find(translatable.getKey() + ".", level);
        }
        return found;
    }

    private static @Nullable Component find(String base, int level) {
        Language language = Language.getInstance();
        for (String suffix : SUFFIXES) {
            String key = base + suffix;
            if (language.has(key)) {
                return Component.translatable(key);
            }
            if (language.has(key + "." + level)) {
                return Component.translatable(key + "." + level);
            }
        }
        return null;
    }
}
