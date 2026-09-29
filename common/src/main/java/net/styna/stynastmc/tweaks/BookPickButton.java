package net.styna.stynastmc.tweaks;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.ui.Ui;

/** Icon button in the merchant screen, sized like Trade Cycling's reroll button. */
public class BookPickButton extends AbstractButton {
    private static final Identifier ICON = StynaSTMC.id("trade/pick");
    private static final Identifier ICON_HOVER = StynaSTMC.id("trade/pick_hover");

    private final Runnable onPress;

    public BookPickButton(int x, int y, Runnable onPress) {
        super(x, y, 18, 14, Component.translatable("stynastmc.trade.targets"));
        this.onPress = onPress;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        onPress.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        Ui.sprite(g, isHovered() ? ICON_HOVER : ICON, getX(), getY(), width, height);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
