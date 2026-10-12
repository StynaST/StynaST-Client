package net.styna.stynastmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = StynaSTMC.MODID, name = Tags.MOD_NAME, version = Tags.VERSION, clientSideOnly = true, acceptedMinecraftVersions = "[1.12.2]",
        guiFactory = "net.styna.stynastmc.config.StynaGuiFactory")
public class StynaSTMC {
    public static final String MODID = Tags.MOD_ID;
    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    public static Path configDir() {
        return Loader.instance().getConfigDir().toPath();
    }

    public static Path modConfigDir() {
        return configDir().resolve("StynaSTMC");
    }

    public static Path modConfigFile(String name) {
        Path file = modConfigDir().resolve(name);
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            LOGGER.warn("Could not create {}", file.getParent(), e);
        }
        return file;
    }

    public static String version() {
        return Tags.VERSION;
    }

    public static boolean isLoaded(String modId) {
        return Loader.isModLoaded(modId);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        StynaSTMCClient.init();
        MinecraftForge.EVENT_BUS.register(StynaSTMCClient.class);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        StynaSTMCClient.keyBindings().forEach(ClientRegistry::registerKeyBinding);
    }
}
