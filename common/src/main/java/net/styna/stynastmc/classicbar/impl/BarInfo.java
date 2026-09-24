package net.styna.stynastmc.classicbar.impl;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.styna.stynastmc.classicbar.impl.overlays.templates.BarOverlayImpl;

/**
 * @param icons sprites drawn on top of each other as the bar icon
 */
public record BarInfo(
        String name,
        Predicate<Player> shouldRender,
        BarOverlayImpl.Numerator numerator,
        BarOverlayImpl.Denominator denominator,
        List<Identifier> icons) {
    public float getRatio(Player player) {
        return Mth.clamp(getUnclampedRatio(player), 0.0F, 1.0F);
    }

    public float getUnclampedRatio(Player player) {
        return numerator.getValue(player) / denominator.getValue(player);
    }

    public static Builder builder(String name) {
        Objects.requireNonNull(name, "name cannot be null");
        return new Builder(name);
    }

    public static class Builder {
        private final String name;
        private Predicate<Player> shouldRender = p -> true;
        private BarOverlayImpl.Numerator numerator;
        private BarOverlayImpl.Denominator denominator = p -> 20.0F;
        private List<Identifier> icons = List.of();

        public Builder(String name) {
            this.name = name;
        }

        public Builder setShouldRender(Predicate<Player> shouldRender) {
            this.shouldRender = Objects.requireNonNull(shouldRender);
            return this;
        }

        public Builder setNumerator(BarOverlayImpl.Numerator numerator) {
            this.numerator = numerator;
            return this;
        }

        public Builder setDenominator(BarOverlayImpl.Denominator denominator) {
            this.denominator = Objects.requireNonNull(denominator);
            return this;
        }

        public Builder setIcons(Identifier... icons) {
            this.icons = List.of(icons);
            return this;
        }

        public BarInfo build() {
            if (numerator == null) {
                throw new IllegalStateException("numerator is empty");
            }
            return new BarInfo(name, shouldRender, numerator, denominator, icons);
        }
    }
}
