/*
 * Ported from Chat Heads by dzwdz (https://github.com/dzwdz/chat_heads).
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.styna.stynastmc.chatheads.mixin;

import net.styna.stynastmc.chatheads.HeadData;
import net.styna.stynastmc.chatheads.mixininterface.HeadRenderable;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiMessage.class)
public abstract class GuiMessageMixin implements HeadRenderable {
    @Unique @NotNull
    public HeadData chatheads$headData = HeadData.EMPTY;

    @Override
    public void chatheads$setHeadData(@NotNull HeadData headData) {
        chatheads$headData = headData;
    }

    @Override @NotNull
    public HeadData chatheads$getHeadData() {
        return chatheads$headData;
    }
}
