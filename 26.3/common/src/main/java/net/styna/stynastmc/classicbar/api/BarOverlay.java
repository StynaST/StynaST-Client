package net.styna.stynastmc.classicbar.api;

import java.util.Optional;

import com.mojang.serialization.Codec;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface BarOverlay {
    boolean enabled();

    BarSide getSide();

    boolean render(GuiGraphicsExtractor graphics, Player player, int screenWidth, int screenHeight, int vOffset);

    Optional<Identifier> disablesOverlay();

    void setErrored();

    String name();

    Codec<? extends BarOverlay> codec();
}
