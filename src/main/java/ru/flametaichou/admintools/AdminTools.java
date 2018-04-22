package ru.flametaichou.admintools;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod (modid = "admintools", name = "Admin Tools", version = "0.1", acceptableRemoteVersions = "*")

public class AdminTools {
	
	@EventHandler
	public void serverLoad(FMLServerStartingEvent event)
	{
		event.registerServerCommand(new AdminToolsCommands());
	}
	
}
