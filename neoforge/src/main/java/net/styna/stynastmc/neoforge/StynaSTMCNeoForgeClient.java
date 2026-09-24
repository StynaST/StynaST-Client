package net.styna.stynastmc.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.styna.stynastmc.StynaSTMC;
import net.styna.stynastmc.StynaSTMCClient;
import net.styna.stynastmc.classicbar.BarHud;
import net.styna.stynastmc.classicbar.ClassicBar;
import net.styna.stynastmc.classicbar.HudStack;
import net.styna.stynastmc.clearspam.ClearSpam;
import net.styna.stynastmc.config.ConfigKey;
import net.styna.stynastmc.config.StynaConfigScreen;
import net.styna.stynastmc.muffler.Muffler;
import net.styna.stynastmc.zoom.Zoom;

@Mod(value = StynaSTMC.MODID, dist = Dist.CLIENT)
public final class StynaSTMCNeoForgeClient {
    public StynaSTMCNeoForgeClient(IEventBus modEventBus, ModContainer modContainer) {
        StynaSTMCClient.init();
        for (StynaSTMCClient.Config config : StynaSTMCClient.CONFIGS) {
            modContainer.registerConfig(ModConfig.Type.CLIENT, config.spec(), config.fileName());
        }
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> new StynaConfigScreen(parent));

        modEventBus.addListener((ModConfigEvent.Loading event) -> configEvent(event, true));
        modEventBus.addListener((ModConfigEvent.Reloading event) -> configEvent(event, false));
        modEventBus.addListener(StynaSTMCNeoForgeClient::registerKeys);
        modEventBus.addListener(StynaSTMCNeoForgeClient::registerLayers);

        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener(EventPriority.HIGH, (ClientTickEvent.Pre event) -> StynaSTMCClient.preTick(Minecraft.getInstance()));
        bus.addListener(EventPriority.HIGH, (ClientTickEvent.Post event) -> StynaSTMCClient.postTick(Minecraft.getInstance()));
        bus.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> StynaSTMCClient.onLoggingOut());
        bus.addListener((RenderGuiLayerEvent.Pre event) -> {
            if (ClassicBar.hidesLayer(event.getName())) {
                event.setCanceled(true);
            }
        });
        bus.addListener((RegisterClientCommandsEvent event) -> event.getDispatcher().register(ClassicBar.command()));
        bus.addListener((ClientChatReceivedEvent event) -> {
            boolean overlay = event instanceof ClientChatReceivedEvent.System system && system.isOverlay();
            event.setMessage(ClearSpam.onChat(event.getMessage(), overlay));
        });
        bus.addListener((ClientChatEvent event) -> {
            if (ClearSpam.onSendChat(event.getMessage())) {
                event.setCanceled(true);
            }
        });
        bus.addListener((InputEvent.MouseScrollingEvent event) -> {
            if (Zoom.onScroll(event.getScrollDeltaY())) {
                event.setCanceled(true);
            }
        });
        bus.addListener((CalculatePlayerTurnEvent event) -> {
            event.setMouseSensitivity(Zoom.sensitivity(event.getMouseSensitivity()));
            if (Zoom.cinematicCamera()) {
                event.setCinematicCameraEnabled(true);
            }
        });
        bus.addListener((ScreenEvent.Init.Post event) -> Muffler.onScreenInit(event.getScreen(), event::addListener));
        bus.addListener((ScreenEvent.Render.Pre event) -> Muffler.onScreenRender(event.getScreen()));
    }

    private static void configEvent(ModConfigEvent event, boolean loading) {
        for (StynaSTMCClient.Config config : StynaSTMCClient.CONFIGS) {
            if (event.getConfig().getSpec() == config.spec()) {
                (loading ? config.onLoad() : config.onReload()).run();
            }
        }
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(ConfigKey.CATEGORY);
        StynaSTMCClient.keyMappings().forEach(event::register);
    }

    private static void registerLayers(RegisterGuiLayersEvent event) {
        Identifier id = ClassicBar.LAYER_ID;
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, id, (graphics, delta) -> {
            var hud = Minecraft.getInstance().gui.hud;
            BarHud.render(graphics, delta, new HudStack() {
                @Override
                public int left() {
                    return hud.leftHeight;
                }

                @Override
                public int right() {
                    return hud.rightHeight;
                }

                @Override
                public void left(int height) {
                    hud.leftHeight = height;
                }

                @Override
                public void right(int height) {
                    hud.rightHeight = height;
                }
            });
        });
    }
}
