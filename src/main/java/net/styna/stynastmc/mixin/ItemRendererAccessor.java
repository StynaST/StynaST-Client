package net.styna.stynastmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.util.EnumHandSide;

@Mixin(ItemRenderer.class)
public interface ItemRendererAccessor {
    @Invoker("renderArmFirstPerson")
    void stynastmc$renderArm(float equipProgress, float swingProgress, EnumHandSide side);
}
