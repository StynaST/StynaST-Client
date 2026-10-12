package net.styna.stynastmc.firstperson.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.styna.stynastmc.firstperson.FirstPerson;
import net.styna.stynastmc.firstperson.RenderStateAccess;

@Mixin(value = AvatarRenderer.class, priority = 500)
public class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void stynastmc$firstPersonParts(Avatar avatar, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        RenderStateAccess access = (RenderStateAccess) state;
        if (!access.stynastmc$isCameraEntity() || !(avatar instanceof Player player)) {
            return;
        }
        // Read the inventory directly, getItemBySlot is blanked while we extract.
        ItemStack main = player.getInventory().getSelectedItem();
        ItemStack off = player.getInventory().getItem(Inventory.SLOT_OFFHAND);
        if (FirstPerson.hideArmsAndItems(player)) {
            access.stynastmc$setHideArms(true);
        } else if (FirstPerson.dynamicHandsEnabled()) {
            access.stynastmc$setArmOffset(Mth.clamp(-player.getXRot() / 20 + 2, 0.0f, 0.7f));
            if (!FirstPerson.lookingDown(player.getXRot())) {
                if (!off.isEmpty() || main.is(Items.FILLED_MAP)) {
                    access.stynastmc$setHideLeftArm(true);
                }
                if (!main.isEmpty()) {
                    access.stynastmc$setHideRightArm(true);
                }
            }
        }
        if (player.isSwimming()) {
            access.stynastmc$setHideBody(true);
            state.showCape = false;
        }
    }
}
