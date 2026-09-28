package net.styna.stynastmc.tweaks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.config.ConfigKey;

/**
 * Drives Trade Cycling's reroll packet from the client until a librarian offers a wanted book.
 * Only sends what Trade Cycling's own button sends, one packet per offers reply.
 */
public final class TradeCycler {
    public static final KeyMapping KEY = new KeyMapping("key.stynastmc.trade_cycle", InputConstants.KEY_F7, ConfigKey.CATEGORY);
    private static final String PACKET = "de.maxhenkel.tradecycling.net.CycleTradesPacket";
    private static final int TIMEOUT = 60;
    private static final int MAX_CYCLES = 5000;

    private static Class<?> packetClass;
    private static boolean checked;
    private static boolean running;
    private static boolean awaiting;
    private static int wait;
    private static int cycles;
    private static Component status;

    private TradeCycler() {
    }

    public static boolean available() {
        if (!checked) {
            checked = true;
            try {
                packetClass = Class.forName(PACKET);
            } catch (ClassNotFoundException | LinkageError e) {
                packetClass = null;
            }
        }
        return packetClass != null;
    }

    public static boolean supports(MerchantScreen screen) {
        if (!available()) {
            return false;
        }
        if (Minecraft.getInstance().crosshairPickEntity instanceof Villager villager) {
            return villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN);
        }
        return screen.getTitle().getContents() instanceof TranslatableContents t && t.getKey().endsWith(".librarian");
    }

    public static boolean running() {
        return running;
    }

    public static Component status() {
        return status;
    }

    public static void onScreenOpened() {
        if (!running) {
            status = null;
        }
    }

    public static Map<String, Integer> targets() {
        Map<String, Integer> targets = new LinkedHashMap<>();
        if (!TweaksConfig.SPEC.isLoaded()) {
            return targets;
        }
        for (String entry : TweaksConfig.BOOK_TARGETS.get()) {
            int split = entry.lastIndexOf('=');
            try {
                targets.put(split < 0 ? entry.trim() : entry.substring(0, split).trim(),
                        split < 0 ? 1 : Integer.parseInt(entry.substring(split + 1).trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return targets;
    }

    public static void setTargets(Map<String, Integer> targets) {
        List<String> list = new ArrayList<>();
        targets.forEach((id, level) -> list.add(id + "=" + level));
        TweaksConfig.BOOK_TARGETS.set(list);
        TweaksConfig.SPEC.save();
    }

    public static void toggle(MerchantMenu menu) {
        if (running) {
            stop(Component.translatable("stynastmc.trade.stopped", cycles));
            return;
        }
        if (targets().isEmpty()) {
            status = Component.translatable("stynastmc.trade.no_targets");
            return;
        }
        if (menu.getTraderXp() > 0) {
            status = Component.translatable("stynastmc.trade.locked");
            return;
        }
        Component hit = find(menu.getOffers());
        if (hit != null) {
            found(hit);
            return;
        }
        running = true;
        cycles = 0;
        send();
    }

    public static void onOffers(MerchantOffers offers) {
        if (!running) {
            return;
        }
        awaiting = false;
        wait = 0;
        Component hit = find(offers);
        if (hit != null) {
            found(hit);
        } else {
            status = Component.translatable("stynastmc.trade.cycling", cycles);
        }
    }

    public static void tick(Minecraft minecraft) {
        while (KEY.consumeClick()) {
        }
        if (!running) {
            return;
        }
        if (!(minecraft.gui.screen() instanceof MerchantScreen screen)) {
            stop(null);
            return;
        }
        wait++;
        if (awaiting) {
            if (wait > TIMEOUT) {
                stop(Component.translatable("stynastmc.trade.no_response"));
            }
            return;
        }
        if (wait < TweaksConfig.CYCLE_DELAY.get()) {
            return;
        }
        if (screen.getMenu().getTraderXp() > 0) {
            stop(Component.translatable("stynastmc.trade.locked"));
        } else if (cycles >= MAX_CYCLES) {
            stop(Component.translatable("stynastmc.trade.gave_up", cycles));
        } else {
            send();
        }
    }

    private static void send() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            stop(null);
            return;
        }
        try {
            CustomPacketPayload payload = (CustomPacketPayload) packetClass.getDeclaredConstructor().newInstance();
            minecraft.getConnection().send(new ServerboundCustomPayloadPacket(payload));
        } catch (ReflectiveOperationException | RuntimeException e) {
            StynaSTMC.LOGGER.error("Failed to send Trade Cycling packet", e);
            stop(Component.translatable("stynastmc.trade.failed"));
            return;
        }
        cycles++;
        awaiting = true;
        wait = 0;
        status = Component.translatable("stynastmc.trade.cycling", cycles);
    }

    private static Component find(MerchantOffers offers) {
        Map<String, Integer> targets = targets();
        for (MerchantOffer offer : offers) {
            ItemEnchantments stored = offer.getResult().get(DataComponents.STORED_ENCHANTMENTS);
            if (stored == null) {
                continue;
            }
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
                Integer min = entry.getKey().unwrapKey().map(key -> targets.get(key.identifier().toString())).orElse(null);
                if (min != null && entry.getIntValue() >= min) {
                    return Enchantment.getFullname(entry.getKey(), entry.getIntValue());
                }
            }
        }
        return null;
    }

    private static void found(Component book) {
        stop(Component.translatable("stynastmc.trade.found", book, cycles));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0F));
    }

    private static void stop(Component message) {
        running = false;
        awaiting = false;
        wait = 0;
        status = message;
    }
}
