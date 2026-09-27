package net.styna.stynastmc.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.styna.stynastmc.config.StynaConfigScreen;

public final class StynaSTMCModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return StynaConfigScreen::new;
    }
}
