package net.styna.stynastmc.classicbar.impl.overlays.vanilla;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.styna.stynastmc.classicbar.api.BarSettings;
import net.styna.stynastmc.classicbar.api.BarSide;
import net.styna.stynastmc.classicbar.api.Color;
import net.styna.stynastmc.classicbar.impl.BarInfo;
import net.styna.stynastmc.classicbar.impl.overlays.templates.BarOverlayImpl;
import net.styna.stynastmc.classicbar.util.ModUtils;

public class Health extends BarOverlayImpl {
    public static final BarInfo INFO = BarInfo.builder("health")
            .setNumerator(LivingEntity::getHealth)
            .setDenominator(LivingEntity::getMaxHealth)
            .build();
    public static final Codec<Health> CODEC = RecordCodecBuilder.create(o -> codecStart(o).apply(o, Health::new));

    private double playerHealth = 0.0;
    private long healthUpdateCounter = 0L;
    private double lastPlayerHealth = 0.0;

    public Health(BarSettings settings) {
        super(INFO, settings);
    }

    @Override
    public Codec<? extends BarOverlayImpl> codec() {
        return CODEC;
    }

    @Override
    public void renderBar(GuiGraphicsExtractor graphics, Player player, int screenWidth, int screenHeight, int vOffset) {
        int updateCounter = Minecraft.getInstance().gui.hud.getGuiTicks();
        double health = player.getHealth();
        double barWidth = getBarWidth(player);
        boolean highlight = healthUpdateCounter > updateCounter && (healthUpdateCounter - updateCounter) / 3L % 2L == 1L;
        if (health < playerHealth && player.getInvulnerableTime() > 0) {
            healthUpdateCounter = updateCounter + 20;
            lastPlayerHealth = playerHealth;
        } else if (health > playerHealth && player.getInvulnerableTime() > 0) {
            healthUpdateCounter = updateCounter + 10;
        }

        playerHealth = health;
        double displayHealth = health + (lastPlayerHealth - health) * (player.getInvulnerableTime() / 20.0);
        int xStart = screenWidth / 2 + getHOffset();
        int yStart = screenHeight - vOffset;
        if (displayHealth > health) {
            double w = getWidth(displayHealth, player.getMaxHealth());
            double f = xStart + (getSide() == BarSide.RIGHT ? WIDTH - barWidth : 0.0);
            double off = getSide() == BarSide.RIGHT ? w - barWidth : 0.0;
            renderPartialBar(Color.WHITE, graphics, f + 2.0 - off, yStart + 2, w);
        }

        renderSimpleBar(getBarSettings().colorProvider().getColor(player, barInfo.getRatio(player), 0), graphics, player, screenWidth, screenHeight, vOffset, highlight);
    }

    @Override
    public void renderIcon(GuiGraphicsExtractor graphics, Player player, int width, int height, int vOffset) {
        int xStart = width / 2 + getIconOffset();
        int yStart = height - vOffset;
        boolean hardcore = isHardcore(player);
        ModUtils.heartContainer(graphics, hardcore, xStart, yStart);
        ModUtils.blitIcon(graphics, getHealthEffect(player).heartSprite(hardcore), xStart, yStart);
    }
}
