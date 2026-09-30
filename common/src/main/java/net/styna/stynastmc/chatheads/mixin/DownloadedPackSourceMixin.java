/*
 * Ported from Chat Heads by dzwdz (https://github.com/dzwdz/chat_heads).
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.styna.stynastmc.chatheads.mixin;

import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.chatheads.ChatHeads;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.resources.server.PackReloadConfig;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DownloadedPackSource.class)
public abstract class DownloadedPackSourceMixin {
    @Inject(method = "loadRequestedPacks", at = @At(value = "RETURN"))
    public void chatheads$checkForDisableResource(List<PackReloadConfig.IdAndPath> list, CallbackInfoReturnable<List<Pack>> cir) {
        List<Pack> packs = cir.getReturnValue();
        if (packs == null) return; // in the rare case the server pack is invalid

        for (Pack pack : packs) {
            try (PackResources resources = pack.open()) {
                if (resources.getResource(PackType.CLIENT_RESOURCES, ChatHeads.DISABLE_RESOURCE) != null) {
                    ChatHeads.serverDisabledChatHeads = true;
                    StynaSTMC.LOGGER.info("Chat Heads disabled by server request");
                }
            }
        }
    }
}
