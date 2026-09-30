/*
 * Ported from Chat Heads by dzwdz (https://github.com/dzwdz/chat_heads).
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.styna.stynastmc.chatheads;

import com.mojang.blaze3d.font.GlyphInfo;

import static net.styna.stynastmc.chatheads.ChatHeads.CONFIG;

// adds one pixel of padding when used inside the chat
public class PaddedChatGlyph implements GlyphInfo {
    public GlyphInfo glyphInfo;

    public PaddedChatGlyph(GlyphInfo original) {
        this.glyphInfo = original;
    }

    @Override
    public float getAdvance() {
        return glyphInfo.getAdvance() + (ChatHeads.customHeadRendering ? CONFIG.rightPadding() + Math.round(2*CONFIG.threeDeeNess()) : 0);
    }
}
