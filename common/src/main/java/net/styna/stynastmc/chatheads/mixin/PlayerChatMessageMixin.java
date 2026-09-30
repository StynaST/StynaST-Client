/*
 * Ported from Chat Heads by dzwdz (https://github.com/dzwdz/chat_heads).
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.styna.stynastmc.chatheads.mixin;

import net.styna.stynastmc.chatheads.mixininterface.Ownable;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerChatMessage.class)
public abstract class PlayerChatMessageMixin implements Ownable {
	@Unique
	private PlayerInfo chatheads$owner;

	@Override
	public void chatheads$setOwner(PlayerInfo playerInfo) {
		chatheads$owner = playerInfo;
	}

	@Override
	public PlayerInfo chatheads$getOwner() {
		return chatheads$owner;
	}
}
