package ru.flametaichou.admintools;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

public class AdminToolsCommands extends CommandBase
{ 
    private final List<String> aliases;
  
    protected String fullEntityName; 
    protected Entity conjuredEntity; 
  
    public AdminToolsCommands()
    { 
        aliases = new ArrayList<String>(); 
        aliases.add("admin");
    } 
  
    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }
    
    @Override 
    public int compareTo(Object o)
    { 
        return 0; 
    } 

    @Override 
    public String getCommandName() 
    { 
        return "admin";
    } 

    @Override         
    public String getCommandUsage(ICommandSender var1) 
    { 
        return "/admin <mobclear/chestclear>";
    } 

    @Override 
    public List<String> getCommandAliases() 
    { 
        return this.aliases;
    } 

    @Override 
    public void processCommand(ICommandSender sender, String[] argString) {
        World world = sender.getEntityWorld(); 
    
        if (!world.isRemote) {
            if(argString.length == 0) {
                sender.addChatMessage(new ChatComponentText("/admin <mobclear (mob, range) / chestclear (range)>"));
                return; 
            }
            if (argString[0].equals("mobclear")) {
	            if(sender instanceof EntityPlayer) {
	            	EntityPlayer player = (EntityPlayer) sender;
                    if (argString.length < 2) {
                        sender.addChatMessage(new ChatComponentTranslation("mobclear.noname"));
                        return;
                    }
                    String mobname = argString[1];
                    if (argString.length < 3) {
                        sender.addChatMessage(new ChatComponentTranslation("mobclear.norange"));
                        return;
                    }
                    int radius = Integer.parseInt(argString[2]);
                    if (radius < 1) {
                        radius = 0;
                    }
                    if (radius > 50) {
                        sender.addChatMessage(new ChatComponentTranslation("mobclear.range50"));
                        radius = 50;
                    }
                    List e = player.worldObj.getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB.getBoundingBox(player.posX-radius, player.posY-radius, player.posZ-radius, (player.posX + radius),(player.posY + radius),(player.posZ + radius)));
                    if (e.size() > 0) {
                        int count = 0;
                        for (Object obj : e) {
                            EntityLiving entityLiving = (EntityLiving) obj;
                            if (entityLiving.getClass().getName().toLowerCase().contains(mobname.toLowerCase())) {
                                entityLiving.setDead();
                                count++;
                            }
                        }
                        sender.addChatMessage(new ChatComponentTranslation("mobclear.done", count));
                    }
	            }
	            return;
	        }
        
            if (argString[0].equals("chestclear")) {
	            if(sender instanceof EntityPlayer) {
	            	EntityPlayer player = (EntityPlayer)sender;
	            	int player_x = (int) player.posX;
                    int player_y = (int) player.posY;
                    int player_z = (int) player.posZ;
                    if (argString.length < 2) {
                        sender.addChatMessage(new ChatComponentTranslation("chestclear.norange"));
                        return;
                    }
                    int radius = Integer.parseInt(argString[1]);
                    if (radius < 1) {
                        radius = 0;
                    }
                    if (radius > 100) {
                        sender.addChatMessage(new ChatComponentTranslation("chestclear.range100"));
                        radius = 100;
                    }
                    int count = 0;
                    for (int x = player_x - radius; x <= player_x + radius; x++) {
                        for (int y = player_y - radius; y <= player_y + radius; y++) {
                            for (int z = player_z - radius; z <= player_z + radius; z++) {
                                TileEntity te = player.worldObj.getTileEntity(x, y, z);
                                if (te != null) {
                                    if (te instanceof IInventory) {
                                        IInventory inventory = ((IInventory) te);
                                        boolean flagCleaned = false;
                                        for (int i = 0; i < inventory.getSizeInventory(); i++) {
                                            if (!flagCleaned && inventory.getStackInSlot(i) != null) {
                                                count++;
                                                flagCleaned = true;
                                            }
                                            inventory.setInventorySlotContents(i, null);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    sender.addChatMessage(new ChatComponentTranslation("chestclear.done", count));
	            }
	            return;
	        }
        }
    } 

    @Override 
    public boolean canCommandSenderUseCommand(ICommandSender var1) 
    { 
        return true;
    } 

    @Override  
    public List<?> addTabCompletionOptions(ICommandSender var1, String[] var2) 
    { 
        return null; 
    } 

    @Override 
    public boolean isUsernameIndex(String[] var1, int var2) 
    { 
        return false;
    }
}
