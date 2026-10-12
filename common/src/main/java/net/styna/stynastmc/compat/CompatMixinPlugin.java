package net.styna.stynastmc.compat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Skips the mixins of a feature when the mod it was ported from is installed. */
public final class CompatMixinPlugin implements IMixinConfigPlugin {
    private static final String CHAT_HEADS_PACKAGE = "net.styna.stynastmc.chatheads.mixin.";
    private static final String FIRST_PERSON_PACKAGE = "net.styna.stynastmc.firstperson.mixin.";
    private static final Map<String, String> MIXINS = Map.of(
            "net.styna.stynastmc.tweaks.mixin.ItemEnchantmentsMixin", ModCompat.ENCHANT_DESCRIPTIONS,
            "net.styna.stynastmc.tweaks.mixin.EnchantDescriptionsItemStackMixin", ModCompat.ENCHANT_DESCRIPTIONS,
            "net.styna.stynastmc.tweaks.mixin.ItemContainerContentsMixin", ModCompat.SHULKER_BOX_TOOLTIP,
            "net.styna.stynastmc.tweaks.mixin.ContainerPreviewItemStackMixin", ModCompat.SHULKER_BOX_TOOLTIP);

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String modId = mixinClassName.startsWith(CHAT_HEADS_PACKAGE) ? ModCompat.CHAT_HEADS
                : mixinClassName.startsWith(FIRST_PERSON_PACKAGE) ? ModCompat.FIRST_PERSON : MIXINS.get(mixinClassName);
        return modId == null || !ModCompat.isLoaded(modId);
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
