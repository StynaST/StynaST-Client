package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderFish;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.styna.stynastmc.firstperson.FirstPerson;

/** Starts the fishing line at the body's hand instead of the vanilla first person hand. */
@Mixin(RenderFish.class)
public class RenderFishMixin {
    private static boolean stynastmc$fromBody(EntityPlayer angler) {
        return angler == Minecraft.getMinecraft().player && FirstPerson.shouldApply() && !FirstPerson.hideArmsAndItems(angler);
    }

    // Ordinal 0 tilts the bobber sprite, ordinal 1 picks the first person hand position.
    @Redirect(method = "doRender", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;thirdPersonView:I", ordinal = 1))
    private int stynastmc$handFromBody(GameSettings settings) {
        return stynastmc$fromBody(Minecraft.getMinecraft().player) ? 1 : settings.thirdPersonView;
    }

    @Redirect(method = "doRender", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;posX:D"))
    private double stynastmc$posX(EntityPlayer angler) {
        return angler.posX + (stynastmc$fromBody(angler) ? FirstPerson.offsetX() : 0);
    }

    @Redirect(method = "doRender", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;prevPosX:D"))
    private double stynastmc$prevPosX(EntityPlayer angler) {
        return angler.prevPosX + (stynastmc$fromBody(angler) ? FirstPerson.offsetX() : 0);
    }

    @Redirect(method = "doRender", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;posZ:D"))
    private double stynastmc$posZ(EntityPlayer angler) {
        return angler.posZ + (stynastmc$fromBody(angler) ? FirstPerson.offsetZ() : 0);
    }

    @Redirect(method = "doRender", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;prevPosZ:D"))
    private double stynastmc$prevPosZ(EntityPlayer angler) {
        return angler.prevPosZ + (stynastmc$fromBody(angler) ? FirstPerson.offsetZ() : 0);
    }
}
