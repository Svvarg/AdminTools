package ru.flametaichou.admintools;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import ru.flametaichou.admintools.handlers.AdminToolsCommands;
import ru.flametaichou.admintools.handlers.EntityEventHandler;
import ru.flametaichou.admintools.handlers.ServerEventHandler;

@Mod (modid = "admintools", name = "Admin Tools", version = "0.2", acceptableRemoteVersions = "*")

public class AdminTools {

	public static EntityEventHandler handler = new EntityEventHandler();
	public static ServerEventHandler serverEventHandler = new ServerEventHandler();

	@EventHandler
	public void serverLoad(FMLServerStartingEvent event) {
		MinecraftForge.EVENT_BUS.register(handler);
		FMLCommonHandler.instance().bus().register(serverEventHandler);
		event.registerServerCommand(new AdminToolsCommands());
	}

	@EventHandler
	public void preLoad(FMLPreInitializationEvent event) {
		ConfigHelper.setupConfig(new Configuration(event.getSuggestedConfigurationFile()));
	}

}
