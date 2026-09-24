package net.styna.stynastmc.classicbar.impl.overlays.vanilla;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.styna.stynastmc.classicbar.api.BarSettings;
import net.styna.stynastmc.classicbar.impl.BarInfo;
import net.styna.stynastmc.classicbar.impl.overlays.templates.FoodLikeBarOverlay;
import net.styna.stynastmc.classicbar.util.ModUtils;

public class Food extends FoodLikeBarOverlay {
    public static final BarInfo INFO = BarInfo.builder("food")
            .setNumerator(player -> player.getFoodData().getFoodLevel())
            .setDenominator(fixed(20.0F))
            .build();
    public static final Codec<Food> CODEC = RecordCodecBuilder.create(o -> codecStartFoodLike(o).apply(o, Food::new));

    public Food(BarSettings barSettings, boolean showSaturation, boolean showExhaustion, boolean showHeldFood) {
        super(INFO, CODEC, () -> 4.0, p -> p.getFoodData().exhaustionLevel, barSettings, showSaturation, showExhaustion, showHeldFood);
    }

    @Override
    public boolean isHealingItem(ItemStack stack, Player player) {
        return stack.get(DataComponents.FOOD) != null;
    }

    @Override
    public int getPotentialHealing(ItemStack stack, Player player) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food == null ? 0 : food.nutrition();
    }

    @Override
    public float getSaturationValue(Player player) {
        return player.getFoodData().getSaturationLevel();
    }

    @Override
    public float getPotentialSaturation(ItemStack stack, Player player) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food == null ? 0.0F : food.saturation();
    }

    @Override
    public void renderIcon(GuiGraphicsExtractor graphics, Player player, int width, int height, int vOffset) {
        int xStart = width / 2 + getIconOffset();
        int yStart = height - vOffset;
        String suffix = player.hasEffect(MobEffects.HUNGER) ? "_hunger" : "";
        ModUtils.blitIcon(graphics, Identifier.withDefaultNamespace("hud/food_empty" + suffix), xStart, yStart);
        ModUtils.blitIcon(graphics, Identifier.withDefaultNamespace("hud/food_full" + suffix), xStart, yStart);
    }
}
