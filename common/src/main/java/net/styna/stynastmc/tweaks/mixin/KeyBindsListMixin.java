package net.styna.stynastmc.tweaks.mixin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.styna.stynastmc.tweaks.KeyBindFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBindsList.class)
public abstract class KeyBindsListMixin extends ContainerObjectSelectionList<KeyBindsList.Entry> implements KeyBindFilter.Target {
    @Unique
    private List<KeyBindsList.Entry> stynastmc$all = List.of();

    private KeyBindsListMixin(Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void stynastmc$capture(KeyBindsScreen screen, Minecraft minecraft, CallbackInfo ci) {
        stynastmc$all = List.copyOf(children());
    }

    @Override
    public void stynastmc$filter(KeyBindFilter filter) {
        List<KeyBindsList.Entry> shown = new ArrayList<>();
        KeyBindsList.Entry category = null;
        for (KeyBindsList.Entry entry : stynastmc$all) {
            if (!(entry instanceof KeyEntryAccessor key)) {
                category = entry;
            } else if (filter.matches(key.stynastmc$key())) {
                // Categories only show above at least one match.
                if (category != null) {
                    shown.add(category);
                    category = null;
                }
                shown.add(entry);
            }
        }
        replaceEntries(shown);
        setScrollAmount(0);
    }
}
