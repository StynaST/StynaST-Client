package net.styna.stynastmc.classicbar.impl.overlays.vanilla;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.styna.stynastmc.classicbar.api.BarSettings;
import net.styna.stynastmc.classicbar.impl.BarInfo;
import net.styna.stynastmc.classicbar.impl.overlays.templates.StackingBarOverlay;
import net.styna.stynastmc.classicbar.util.ModUtils;

public class Absorption extends StackingBarOverlay {
    public static final BarInfo INFO = BarInfo.builder("absorption")
            .setShouldRender(player -> player.getAbsorptionAmount() > 0.0F)
            .setNumerator(LivingEntity::getAbsorptionAmount)
            .setDenominator(LivingEntity::getMaxHealth)
            .build();
    public static final Codec<Absorption> CODEC = RecordCodecBuilder.create(o -> codecStart(o).apply(o, Absorption::new));

    public Absorption(BarSettings barSettings) {
        super(INFO, barSettings, CODEC);
    }

    @Override
    public void renderIcon(GuiGraphicsExtractor graphics, Player player, int width, int height, int vOffset) {
        int xStart = width / 2 + getIconOffset();
        int yStart = height - vOffset;
        boolean hardcore = isHardcore(player);
        ModUtils.heartContainer(graphics, hardcore, xStart, yStart);
        ModUtils.blitIcon(graphics, Identifier.withDefaultNamespace(hardcore ? "hud/heart/absorbing_hardcore_full" : "hud/heart/absorbing_full"), xStart, yStart);
    }
}
