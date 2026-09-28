package net.styna.stynastmc.tweaks.mixin;

import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import net.styna.stynastmc.tweaks.BookPickButton;
import net.styna.stynastmc.tweaks.BookTargetScreen;
import net.styna.stynastmc.tweaks.TradeCycler;
import net.styna.stynastmc.ui.Ui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {
    @Unique
    private BookPickButton stynastmc$pick;

    private MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void stynastmc$addCycler(CallbackInfo ci) {
        stynastmc$pick = null;
        if (!TradeCycler.supports((MerchantScreen) (Object) this)) {
            return;
        }
        TradeCycler.onScreenOpened();
        stynastmc$pick = addRenderableWidget(new BookPickButton(leftPos + 250, topPos + 8,
                () -> minecraft.gui.setScreen(new BookTargetScreen(this))));
        stynastmc$tooltip();
    }

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void stynastmc$drawStatus(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (stynastmc$pick == null) {
            return;
        }
        Component status = TradeCycler.status();
        if (status != null) {
            Ui.centeredText(g, status, leftPos, topPos + imageHeight + 4, imageWidth, 12, Ui.WHITE);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (stynastmc$pick != null && TradeCycler.KEY.matches(event)) {
            TradeCycler.toggle(menu);
            return true;
        }
        return super.keyPressed(event);
    }

    @Unique
    private void stynastmc$tooltip() {
        Map<String, Integer> targets = TradeCycler.targets();
        MutableComponent text = Component.translatable("stynastmc.trade.targets").withStyle(ChatFormatting.WHITE);
        if (targets.isEmpty()) {
            text.append("\n").append(Component.translatable("stynastmc.trade.none").withStyle(ChatFormatting.GRAY));
        }
        targets.forEach((id, level) -> {
            Identifier key = Identifier.tryParse(id);
            Component name = key == null ? Component.literal(id) : minecraft.getConnection().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT).get(key)
                    .<Component>map(holder -> Enchantment.getFullname(holder, level))
                    .orElse(Component.literal(id));
            text.append("\n").append(Component.translatable("stynastmc.trade.min_level", name).withStyle(ChatFormatting.GRAY));
        });
        text.append("\n\n").append(Component.translatable("stynastmc.trade.key_hint", TradeCycler.KEY.getTranslatedKeyMessage())
                .withStyle(ChatFormatting.DARK_GRAY));
        stynastmc$pick.setTooltip(Tooltip.create(text));
    }
}
