package net.styna.stynastmc.tweaks.mixin;

import net.minecraft.client.Options;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.styna.stynastmc.tweaks.KeyBindFilter;
import net.styna.stynastmc.tweaks.TweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBindsScreen.class)
public abstract class KeyBindsScreenMixin extends OptionsSubScreen {
    @Shadow
    private KeyBindsList keyBindsList;
    @Unique
    private KeyBindFilter stynastmc$filter = KeyBindFilter.NONE;

    private KeyBindsScreenMixin(Screen lastScreen, Options options, Component title) {
        super(lastScreen, options, title);
    }

    @Override
    protected void addTitle() {
        if (!TweaksConfig.on(TweaksConfig.KEYBIND_SEARCH)) {
            super.addTitle();
            return;
        }
        layout.setHeaderHeight(58);
        LinearLayout column = layout.addToHeader(LinearLayout.vertical().spacing(6));
        column.defaultCellSetting().alignHorizontallyCenter();
        column.addChild(new StringWidget(title, font));
        LinearLayout row = column.addChild(LinearLayout.horizontal().spacing(6));

        EditBox search = row.addChild(new EditBox(font, 200, 20, Component.translatable("stynastmc.keybinds.search")));
        search.setHint(Component.translatable("stynastmc.keybinds.search"));
        search.setValue(stynastmc$filter.query());
        search.setResponder(text -> stynastmc$apply(new KeyBindFilter(text, stynastmc$filter.mode())));

        row.addChild(CycleButton.builder(KeyBindFilter.Mode::label, stynastmc$filter.mode())
                .withValues(KeyBindFilter.Mode.values())
                .create(0, 0, 120, 20, Component.translatable("stynastmc.keybinds.show"),
                        (button, mode) -> stynastmc$apply(new KeyBindFilter(stynastmc$filter.query(), mode))));
    }

    @Inject(method = "addContents", at = @At("TAIL"))
    private void stynastmc$reapply(CallbackInfo ci) {
        if (!stynastmc$filter.isEmpty()) {
            stynastmc$apply(stynastmc$filter);
        }
    }

    @Unique
    private void stynastmc$apply(KeyBindFilter filter) {
        stynastmc$filter = filter;
        if (keyBindsList != null) {
            ((KeyBindFilter.Target) keyBindsList).stynastmc$filter(filter);
        }
    }
}
