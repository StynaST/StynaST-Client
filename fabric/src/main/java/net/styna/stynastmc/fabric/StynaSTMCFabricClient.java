package net.styna.stynastmc.fabric;

import java.util.Map;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import fuzs.forgeconfigapiport.fabric.api.v5.client.ConfigScreenFactoryRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.config.ModConfig;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.StynaSTMCClient;
import net.styna.stynastmc.classicbar.BarHud;
import net.styna.stynastmc.classicbar.ClassicBar;
import net.styna.stynastmc.classicbar.HudLayers;
import net.styna.stynastmc.classicbar.HudStack;
import net.styna.stynastmc.classicbar.network.ClassicBarNetwork;
import net.styna.stynastmc.classicbar.network.ExhaustionSyncPayload;
import net.styna.stynastmc.clearspam.ClearSpam;
import net.styna.stynastmc.config.ConfigKey;
import net.styna.stynastmc.config.StynaConfigScreen;
import net.styna.stynastmc.muffler.Muffler;

public final class StynaSTMCFabricClient implements ClientModInitializer {
    /** Fabric element for each NeoForge layer id ClassicBar can hide. */
    private static final Map<Identifier, Identifier> LAYERS = Map.of(
            HudLayers.PLAYER_HEALTH, VanillaHudElements.HEALTH_BAR,
            HudLayers.ARMOR_LEVEL, VanillaHudElements.ARMOR_BAR,
            HudLayers.FOOD_LEVEL, VanillaHudElements.FOOD_BAR,
            HudLayers.VEHICLE_HEALTH, VanillaHudElements.MOUNT_HEALTH,
            HudLayers.AIR_LEVEL, VanillaHudElements.AIR_BAR,
            HudLayers.EXPERIENCE_LEVEL, VanillaHudElements.EXPERIENCE_LEVEL,
            HudLayers.CONTEXTUAL_INFO_BAR, VanillaHudElements.INFO_BAR);

    /** Vanilla's status bars start 39px above the bottom of the screen. */
    private static final int STACK_BASE = 39;

    @Override
    public void onInitializeClient() {
        StynaSTMCClient.init();
        // Listeners first: FCAP may load client configs as soon as they are registered.
        ModConfigEvents.loading(StynaSTMC.MODID).register(config -> configEvent(config, true));
        ModConfigEvents.reloading(StynaSTMC.MODID).register(config -> configEvent(config, false));
        for (StynaSTMCClient.Config config : StynaSTMCClient.CONFIGS) {
            ConfigRegistry.INSTANCE.register(StynaSTMC.MODID, ModConfig.Type.CLIENT, config.spec(), config.fileName());
        }
        ConfigScreenFactoryRegistry.INSTANCE.register(StynaSTMC.MODID, (modId, parent) -> new StynaConfigScreen(parent));

        KeyMapping.Category.register(ConfigKey.CATEGORY.id());
        StynaSTMCClient.keyMappings().forEach(KeyMappingHelper::registerKeyMapping);

        ClientTickEvents.START_CLIENT_TICK.register(StynaSTMCClient::preTick);
        ClientTickEvents.END_CLIENT_TICK.register(StynaSTMCClient::postTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> StynaSTMCClient.onLoggingOut());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(ClassicBar.command()));
        ClientReceiveMessageEvents.MODIFY_GAME.register(ClearSpam::onChat);
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> !ClearSpam.onSendChat(message));
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            Muffler.onScreenInit(screen, Screens.getWidgets(screen)::add);
            ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> Muffler.onScreenRender(s));
        });
        ClientPlayNetworking.registerGlobalReceiver(ExhaustionSyncPayload.TYPE, (payload, context) -> {
            payload.handle(context.player());
            ClassicBarNetwork.presentOnServer = true;
        });
        registerHud();
    }

    private static void configEvent(ModConfig modConfig, boolean loading) {
        for (StynaSTMCClient.Config config : StynaSTMCClient.CONFIGS) {
            if (modConfig.getSpec() == config.spec()) {
                (loading ? config.onLoad() : config.onReload()).run();
            }
        }
    }

    private static void registerHud() {
        Stack stack = new Stack();
        HudElementRegistry.attachElementAfter(VanillaHudElements.AIR_BAR, ClassicBar.LAYER_ID, (graphics, delta) -> {
            stack.left = STACK_BASE;
            stack.right = STACK_BASE;
            BarHud.render(graphics, delta, stack);
        });
        LAYERS.forEach((layer, element) -> HudElementRegistry.replaceElement(element, original -> (graphics, delta) -> {
            if (!ClassicBar.hidesLayer(layer)) {
                original.extractRenderState(graphics, delta);
            }
        }));
    }

    // Vanilla on Fabric doesn't track stack heights, so bars stack from the base each frame.
    private static final class Stack implements HudStack {
        private int left;
        private int right;

        @Override
        public int left() {
            return left;
        }

        @Override
        public int right() {
            return right;
        }

        @Override
        public void left(int height) {
            left = height;
        }

        @Override
        public void right(int height) {
            right = height;
        }
    }
}
