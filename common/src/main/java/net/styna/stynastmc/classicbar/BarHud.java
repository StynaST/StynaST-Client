package net.styna.stynastmc.classicbar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.JsonOps;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.client.gui.contextualbar.ExperienceBar;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.player.Player;
import net.styna.stynastmc.classicbar.api.BarOverlay;
import net.styna.stynastmc.classicbar.api.BarRegistry;
import net.styna.stynastmc.classicbar.api.BarSide;
import net.styna.stynastmc.classicbar.config.ClassicBarConfig;

public final class BarHud {
    private static final int CONTEXTUAL_BAR_SPACE = 6;

    // Swapped atomically, since config reloads can happen off the render thread.
    private static volatile List<BarOverlay> bars = List.of();
    private static volatile Set<Identifier> replacedLayers = Set.of();

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, HudStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        Hud hud = minecraft.gui.hud;
        if (!ClassicBarConfig.ENABLED.get() || hud.isHidden() || !(minecraft.getCameraEntity() instanceof Player player)
                || player.getAbilities().invulnerable || player.isSpectator()) {
            return;
        }

        // Nothing is drawn in the contextual bar slot, so drop the stacks into the space it would use.
        if (!contextualBarVisible(hud)) {
            stack.left(stack.left() - CONTEXTUAL_BAR_SPACE);
            stack.right(stack.right() - CONTEXTUAL_BAR_SPACE);
        }

        Profiler.get().push("classicbars_hud");
        for (BarOverlay overlay : bars) {
            BarSide side = overlay.getSide();
            try {
                int offset = side == BarSide.LEFT ? stack.left() : stack.right();
                if (overlay.render(graphics, player, graphics.guiWidth(), graphics.guiHeight(), offset)) {
                    if (side == BarSide.LEFT) {
                        stack.left(stack.left() + 10);
                    } else {
                        stack.right(stack.right() + 10);
                    }
                }
            } catch (Throwable e) {
                ClassicBar.LOGGER.error("Disabling broken overlay {}", overlay.name(), e);
                overlay.setErrored();
            }
        }
        Profiler.get().pop();
    }

    // The contextual bar slot is shared with the locator and horse jump bars, so only hide it while it shows XP.
    public static boolean hidesExperienceBar() {
        return replaces(HudLayers.EXPERIENCE_LEVEL)
                && Minecraft.getInstance().gui.hud.contextualInfoBar.getSecond() instanceof ExperienceBar;
    }

    private static boolean contextualBarVisible(Hud hud) {
        return hud.contextualInfoBar.getSecond() != ContextualBar.EMPTY && !hidesExperienceBar();
    }

    public static boolean replaces(Identifier layer) {
        return replacedLayers.contains(layer);
    }

    public static void reload() {
        List<BarOverlay> loaded = new ArrayList<>();
        Set<Identifier> replaced = new HashSet<>();
        for (Map.Entry<String, BarOverlay> entry : BarRegistry.REGISTRY.entrySet()) {
            BarOverlay bar;
            try {
                bar = entry.getValue().codec().parse(JsonOps.INSTANCE, ClassicBarConfig.readBar(entry.getKey())).getOrThrow();
            } catch (Exception e) {
                ClassicBar.LOGGER.error("Invalid config for bar {}, using defaults", entry.getKey(), e);
                bar = entry.getValue();
            }
            loaded.add(bar);
            if (bar.enabled()) {
                bar.disablesOverlay().ifPresent(replaced::add);
            }
        }

        List<? extends String> priority = ClassicBarConfig.PRIORITY.get();
        loaded.sort(Comparator.comparingInt(bar -> {
            int index = priority.indexOf(bar.name());
            return index < 0 ? Integer.MAX_VALUE : index;
        }));
        bars = List.copyOf(loaded);
        replacedLayers = Set.copyOf(replaced);
    }
}
