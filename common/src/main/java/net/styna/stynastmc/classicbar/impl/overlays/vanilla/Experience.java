package net.styna.stynastmc.classicbar.impl.overlays.vanilla;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.styna.stynastmc.classicbar.api.BarSettings;
import net.styna.stynastmc.classicbar.impl.BarInfo;
import net.styna.stynastmc.classicbar.impl.overlays.templates.BarOverlayImpl;

public class Experience extends BarOverlayImpl {
    private static final Identifier ORB = Identifier.withDefaultNamespace("textures/entity/experience/experience_orb.png");
    // ExperienceOrbRenderer's tint frozen halfway through its pulse.
    private static final int ORB_TINT = ARGB.color(255, 127, 255, 3);
    public static final BarInfo INFO = BarInfo.builder("experience")
            .setShouldRender(player -> Minecraft.getInstance().gameMode != null && Minecraft.getInstance().gameMode.hasExperience())
            .setNumerator(player -> player.experienceProgress)
            .setDenominator(fixed(1.0F))
            .build();
    public static final Codec<Experience> CODEC = RecordCodecBuilder.create(o -> codecStart(o).apply(o, Experience::new));

    public Experience(BarSettings settings) {
        super(INFO, settings);
    }

    @Override
    public Codec<? extends BarOverlayImpl> codec() {
        return CODEC;
    }

    // Same rounding as the vanilla bar: floor(progress * (width + 1)), capped at the width.
    @Override
    public void renderBar(GuiGraphicsExtractor graphics, Player player, int screenWidth, int screenHeight, int vOffset) {
        int barWidth = Math.min(WIDTH, (int) (player.experienceProgress * (WIDTH + 1)));
        renderBarBackground(graphics, player, screenWidth, screenHeight, vOffset);
        if (barWidth > 0) {
            renderPartialBar(getBarSettings().colorProvider().getColor(player, barInfo.getRatio(player), 0), graphics,
                    getXStartBar(screenWidth, barWidth) + 2, screenHeight - vOffset + 2, barWidth);
        }
    }

    @Override
    public void renderText(GuiGraphicsExtractor graphics, Player player, int width, int height, int vOffset) {
        if (player.experienceLevel <= 0) {
            return;
        }
        textHelper(graphics, width / 2 + getIconOffset(), height - vOffset, player.experienceLevel,
                getBarSettings().colorProvider().getColor(player, barInfo.getRatio(player), 0).argb());
    }

    @Override
    public void renderIcon(GuiGraphicsExtractor graphics, Player player, int width, int height, int vOffset) {
        // Second smallest orb: a 10x10 orb at (19, 3) on the 64x64 sheet.
        graphics.blit(RenderPipelines.GUI_TEXTURED, ORB, width / 2 + getIconOffset(), height - vOffset, 19.0F, 3.0F, 10, 10, 64, 64, ORB_TINT);
    }
}
