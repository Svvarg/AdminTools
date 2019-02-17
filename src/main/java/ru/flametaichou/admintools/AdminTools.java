package ru.flametaichou.admintools;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.common.MinecraftForge;

@Mod (modid = "admintools", name = "Admin Tools", version = "0.1", acceptableRemoteVersions = "*")

public class AdminTools {

	public static ATEventHandler handler = new ATEventHandler();
	
	@EventHandler
	public void serverLoad(FMLServerStartingEvent event)
	{
		MinecraftForge.EVENT_BUS.register(handler);
		event.registerServerCommand(new AdminToolsCommands());
	}

}
