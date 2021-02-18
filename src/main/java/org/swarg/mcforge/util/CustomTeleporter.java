package org.swarg.mcforge.util;

import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

/**
 * 17-02-21
 * @author Swarg
 */
public class CustomTeleporter extends Teleporter {

   public CustomTeleporter(WorldServer par1WorldServer) {
      super(par1WorldServer);
   }

   public void placeInPortal(Entity par1Entity, double par2, double par4, double par6, float par8) {}
}

