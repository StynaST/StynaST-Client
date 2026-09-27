package net.styna.stynastmc.classicbar.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class ModUtils {
    private static final int TEXTURE_SIZE = 256;

    private ModUtils() {
    }

    public static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int u, int v, int width, int height) {
        blit(graphics, texture, x, y, u, v, width, height, -1);
    }

    public static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int u, int v, int width, int height, int color) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, TEXTURE_SIZE, TEXTURE_SIZE, color);
    }

    public static void blitIcon(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 9, 9);
    }

    public static void heartContainer(GuiGraphicsExtractor graphics, boolean hardcore, int x, int y) {
        blitIcon(graphics, Identifier.withDefaultNamespace(hardcore ? "hud/heart/container_hardcore" : "hud/heart/container"), x, y);
    }

    public static void drawStringOnHUD(GuiGraphicsExtractor graphics, String string, int xOffset, int yOffset, int color) {
        graphics.text(Minecraft.getInstance().font, string, xOffset + 2, yOffset + 2, color, true);
    }
}
