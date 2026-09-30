/*
 * Ported from Chat Heads by dzwdz (https://github.com/dzwdz/chat_heads).
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.styna.stynastmc.chatheads.mixininterface;

import net.styna.stynastmc.chatheads.HeadData;
import org.jetbrains.annotations.NotNull;

public interface HeadRenderable {
    @NotNull HeadData chatheads$getHeadData();
    void chatheads$setHeadData(@NotNull HeadData headData);
}
