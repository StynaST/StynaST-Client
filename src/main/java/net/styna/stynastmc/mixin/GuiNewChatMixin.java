package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.gui.GuiNewChat;

// Keeps far more chat lines than vanilla's 100 before trimming the oldest.
@Mixin(GuiNewChat.class)
public class GuiNewChatMixin {
    @Unique
    private static final int STYNASTMC_HISTORY_LIMIT = 16384;

    @ModifyConstant(method = "setChatLine", constant = @Constant(intValue = 100))
    private int stynastmc$historyLimit(int limit) {
        return STYNASTMC_HISTORY_LIMIT;
    }
}
