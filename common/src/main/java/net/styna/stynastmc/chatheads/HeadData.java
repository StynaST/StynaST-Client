/*
 * Ported from Chat Heads by dzwdz (https://github.com/dzwdz/chat_heads).
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.styna.stynastmc.chatheads;

import net.minecraft.client.multiplayer.PlayerInfo;

// playerInfo gives us the texture, codePointIndex the position inside the message
public record HeadData(PlayerInfo playerInfo, int codePointIndex) {
    public static HeadData EMPTY = new HeadData(null, -1);

    public HeadData {
        if (playerInfo == null && codePointIndex >= 0)
            throw new AssertionError();
    }

    public static HeadData of(PlayerInfo playerInfo) {
        if (playerInfo == null) return EMPTY;
        return new HeadData(playerInfo, -1);
    }

    public boolean hasHeadPosition() {
        return codePointIndex >= 0;
    }
}
