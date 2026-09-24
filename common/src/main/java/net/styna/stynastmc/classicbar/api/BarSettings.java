package net.styna.stynastmc.classicbar.api;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.styna.stynastmc.classicbar.api.colorprovider.ColorProvider;
import net.styna.stynastmc.classicbar.api.colorprovider.SingleColorProvider;

public record BarSettings(
        boolean enabled,
        Optional<Identifier> disablesOverlay,
        BarSide side,
        boolean fitted,
        ColorProvider colorProvider,
        boolean show_text,
        boolean show_icon) {
    public static final MapCodec<BarSettings> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("enabled").forGetter(BarSettings::enabled),
            Identifier.CODEC.optionalFieldOf("disables_overlay").forGetter(BarSettings::disablesOverlay),
            BarSide.CODEC.fieldOf("side").forGetter(BarSettings::side),
            Codec.BOOL.fieldOf("fitted").forGetter(BarSettings::fitted),
            ColorProvider.CODEC.fieldOf("color_provider").forGetter(BarSettings::colorProvider),
            Codec.BOOL.fieldOf("show_text").forGetter(BarSettings::show_text),
            Codec.BOOL.fieldOf("show_icon").forGetter(BarSettings::show_icon))
            .apply(instance, BarSettings::new));

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean enabled = true;
        private Identifier disablesOverlay;
        private BarSide side = BarSide.LEFT;
        private ColorProvider colorProvider = new SingleColorProvider(Color.WHITE);
        private boolean fitted = false;
        private boolean show_text = true;
        private boolean show_icon = true;

        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder setDisablesOverlay(Identifier disablesOverlay) {
            this.disablesOverlay = disablesOverlay;
            return this;
        }

        public Builder setSide(BarSide side) {
            this.side = side;
            return this;
        }

        public Builder fitted() {
            this.fitted = true;
            return this;
        }

        public Builder setShowText(boolean show_text) {
            this.show_text = show_text;
            return this;
        }

        public Builder setColorProvider(ColorProvider colorProvider) {
            this.colorProvider = colorProvider;
            return this;
        }

        public Builder setShowIcon(boolean show_icon) {
            this.show_icon = show_icon;
            return this;
        }

        public BarSettings build() {
            return new BarSettings(enabled, Optional.ofNullable(disablesOverlay), side, fitted, colorProvider, show_text, show_icon);
        }
    }
}
