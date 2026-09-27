package net.styna.stynastmc.chat.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

// Keeps far more chat lines than vanilla's 100 before trimming the oldest.
@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @Unique
    private static final int STYNASTMC_HISTORY_LIMIT = 16384;

    @ModifyConstant(method = {"addMessageToDisplayQueue", "addMessageToQueue"}, constant = @Constant(intValue = 100))
    private int stynastmc$historyLimit(int limit) {
        return STYNASTMC_HISTORY_LIMIT;
    }
}
