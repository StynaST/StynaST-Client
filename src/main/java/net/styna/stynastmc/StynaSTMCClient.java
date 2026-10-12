package net.styna.stynastmc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ChatType;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.styna.stynastmc.capes.Capes;
import net.styna.stynastmc.capes.CapesConfig;
import net.styna.stynastmc.clearspam.ClearSpam;
import net.styna.stynastmc.clearspam.config.ClearSpamConfig;
import net.styna.stynastmc.config.ConfigKey;
import net.styna.stynastmc.config.StynaConfigSpec;
import net.styna.stynastmc.firstperson.FirstPerson;
import net.styna.stynastmc.firstperson.FirstPersonConfig;
import net.styna.stynastmc.fonts.FontRuntime;
import net.styna.stynastmc.fonts.FontsConfig;
import net.styna.stynastmc.freecam.Freecam;
import net.styna.stynastmc.freecam.config.FreecamConfig;
import net.styna.stynastmc.hotreload.HotReload;
import net.styna.stynastmc.mixin.GuiStatsAccessor;
import net.styna.stynastmc.muffler.Muffler;
import net.styna.stynastmc.skins.CustomSkins;
import net.styna.stynastmc.skins.RemoteSkins;
import net.styna.stynastmc.skins.SkinsBridge;
import net.styna.stynastmc.skins.SkinsConfig;
import net.styna.stynastmc.stats.StatsViewScreen;
import net.styna.stynastmc.tweaks.ArmorHud;
import net.styna.stynastmc.tweaks.ContainerPreview;
import net.styna.stynastmc.tweaks.DarknessCompat;
import net.styna.stynastmc.tweaks.ContainerPreviewConfig;
import net.styna.stynastmc.tweaks.EnchantDescriptions;
import net.styna.stynastmc.tweaks.EnchantDescriptionsConfig;
import net.styna.stynastmc.tweaks.TweaksConfig;
import net.styna.stynastmc.zoom.Zoom;
import net.styna.stynastmc.zoom.config.ZoomConfig;

/** Client entry point: loads configs and routes Forge events to the features. */
public final class StynaSTMCClient {
    public static final class Config {
        final StynaConfigSpec spec;
        final String name;
        final Runnable onLoad;
        final Runnable onReload;

        Config(StynaConfigSpec spec, String name, Runnable onLoad, Runnable onReload) {
            this.spec = spec;
            this.name = name;
            this.onLoad = onLoad;
            this.onReload = onReload;
        }

        Path path() {
            return StynaSTMC.modConfigFile(name + ".toml");
        }
    }

    private static final Runnable NONE = () -> {
    };
    private static long lastConfigCheck;
    private static boolean fontApplied;

    public static final List<Config> CONFIGS = Arrays.asList(
            new Config(ClearSpamConfig.SPEC, "clearspam", ClearSpam::syncConfig, ClearSpam::syncConfig),
            new Config(FreecamConfig.SPEC, "freecam", NONE, NONE),
            new Config(FontsConfig.SPEC, "fonts", NONE, FontRuntime::applyConfigured),
            new Config(SkinsConfig.SPEC, "skins", NONE, SkinsBridge::onConfigReloading),
            new Config(CapesConfig.SPEC, "capes", NONE, Capes::onConfigReloading),
            new Config(ZoomConfig.SPEC, "zoom", NONE, NONE),
            new Config(FirstPersonConfig.SPEC, "firstperson", NONE, NONE),
            new Config(TweaksConfig.SPEC, "tweaks", NONE, NONE),
            new Config(EnchantDescriptionsConfig.SPEC, "enchantdescriptions", NONE, NONE),
            new Config(ContainerPreviewConfig.SPEC, "containerpreview", NONE, NONE));

    private StynaSTMCClient() {
    }

    public static void init() {
        for (Config config : CONFIGS) {
            config.spec.load(config.path());
            config.onLoad.run();
        }
        Muffler.load();
    }

    public static List<KeyBinding> keyBindings() {
        List<KeyBinding> keys = new ArrayList<>(Freecam.keys());
        keys.add(Zoom.KEY);
        keys.add(FirstPerson.KEY);
        keys.add(Muffler.KEY);
        keys.add(ConfigKey.OPEN);
        return keys;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.START) {
            if (!fontApplied) {
                fontApplied = true;
                FontRuntime.applyConfigured();
            }
            Freecam.preTick();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastConfigCheck >= 1000) {
            lastConfigCheck = now;
            for (Config config : CONFIGS) {
                if (config.spec.reloadIfModified()) {
                    config.onReload.run();
                }
            }
        }
        ConfigKey.tick(minecraft);
        FirstPerson.tick();
        Muffler.tick(minecraft);
        HotReload.tick(minecraft);
        DarknessCompat.tick();
        CustomSkins.tick();
        RemoteSkins.tick();
    }

    @SubscribeEvent
    public static void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            RemoteSkins.clear();
            ClearSpam.onDisconnect();
        });
    }

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent event) {
        if (event.getType() != ChatType.GAME_INFO) {
            event.setMessage(ClearSpam.onChat(event.getMessage()));
        }
    }

    @SubscribeEvent
    public static void onSendChat(ClientChatEvent event) {
        if (ClearSpam.onSendChat(event.getMessage())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouse(MouseEvent event) {
        if (event.getDwheel() != 0 && Minecraft.getMinecraft().currentScreen == null && Zoom.onScroll(event.getDwheel())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        Muffler.onPlaySound(event.getSound());
    }

    @SubscribeEvent
    public static void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        Muffler.onScreenInit(event.getGui(), event.getButtonList());
    }

    @SubscribeEvent
    public static void onGuiAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (Muffler.onButton(event.getGui(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGuiDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        Muffler.onScreenRender(event.getGui());
    }

    @SubscribeEvent
    public static void onGuiOpen(GuiOpenEvent event) {
        if (event.getGui() != null && event.getGui().getClass() == GuiStats.class && TweaksConfig.on(TweaksConfig.STATS_SCREEN)
                && Minecraft.getMinecraft().player != null) {
            event.setGui(new StatsViewScreen(((GuiStatsAccessor) event.getGui()).stynastmc$parent()));
        }
    }

    @SubscribeEvent
    public static void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HOTBAR) {
            return;
        }
        if (Minecraft.getMinecraft().getRenderViewEntity() instanceof EntityPlayer player && !player.isSpectator()) {
            ArmorHud.render(player, event.getResolution().getScaledWidth(), event.getResolution().getScaledHeight());
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        EnchantDescriptions.addDescriptions(event.getItemStack(), event.getToolTip());
        ContainerPreview.onTooltip(event.getItemStack(), event.getToolTip());
    }

    @SubscribeEvent
    public static void onTooltipDrawn(RenderTooltipEvent.PostText event) {
        ContainerPreview.render(event.getStack(), event.getLines(), event.getX(), event.getY());
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (Freecam.isEnabled() && !FreecamConfig.SHOW_PLAYER.get() && event.getEntityPlayer() == Minecraft.getMinecraft().player) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderSpecificHand(RenderSpecificHandEvent event) {
        FirstPerson.onRenderHand(event);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (Freecam.isEnabled() && !FreecamConfig.SHOW_HAND.get()) {
            event.setCanceled(true);
        }
    }

    // Overlays describe the player's head, which is somewhere else while the camera is free.
    @SubscribeEvent
    public static void onBlockOverlay(RenderBlockOverlayEvent event) {
        if (Freecam.isEnabled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockHighlight(DrawBlockHighlightEvent event) {
        if (Freecam.blocksInteraction()) {
            event.setCanceled(true);
        }
    }
}
