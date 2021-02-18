package org.swarg.mcforge.tools;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Field;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.IExtendedEntityProperties;
import org.swarg.mcforge.util.XPlayer;
import static net.minecraft.util.StringUtils.isNullOrEmpty;
import static org.swarg.mcforge.tools.InventoryCommands.arg;
import static org.swarg.mcforge.tools.InventoryCommands.isCmd;
import static org.swarg.mcforge.tools.InventoryCommands.toSender;

/**
 * 18-02-21
 * @author Swarg
 */
public class PlayerCommands {

    private static PlayerCommands INSTANCE;
    private Map<String, EntityPlayerMP> offlinePlayerCache = new HashMap<String, EntityPlayerMP>();

    public static PlayerCommands instance() {
        if (INSTANCE == null) {
            INSTANCE = new PlayerCommands();
        }
        return INSTANCE;
    }

    //online & offline via cache
    private EntityPlayerMP getEntityPlayerMP(String name) {
        EntityPlayerMP player = MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);//online
        if (player == null) {
            player = this.offlinePlayerCache.get(name);
            if (player == null) {
                player = XPlayer.getOfflineEntityPlayerMP(name);//offline from disk
            }
            if (player != null) {
                //toSender(sender, "Taken from hdd");
                this.offlinePlayerCache.put(name, player);
                return player;
            }
        } else {
            //если инстанс взят с онлайн игрока удалить из кэша запись
            this.offlinePlayerCache.remove(player.getCommandSenderName());
        }
        return player;
    }


    public void cmdPlayer(ICommandSender sender, String[] args) {
        int i = 1;
        String cmd = arg(args, i++);

        if (isNullOrEmpty(cmd) || "help".equals(cmd)) {
            toSender(sender, "<status/ext-props/custom-entity-data/clear-cache>");
        }
        else if (isCmd(cmd, "status", "st")) {
            cmdPlayerStatus(sender, args, i);
        }
        else if (isCmd(cmd, "ext-props", "ep")) {
            cmdExtendedPlayerProps(sender, args, i);
        }
        //customEntityData
        else if (isCmd(cmd, "custom-entity-data", "ced")) {
            cmdPlayerCustomEntityData(sender, args, i);
        }
        else if (isCmd(cmd, "clear-cache", "cc")) {
            this.offlinePlayerCache.clear();
            toSender(sender, "done");
        }
    }


    //todo?? eco without EntityPlayer Instance for case than dataread from disc
    public void cmdExtendedPlayerProps(ICommandSender sender, String[] args, int i) {
        //int i = 1;
        String response = "?";
        final String name = arg(args,i++);
        if (isNullOrEmpty(name) || "help".equals(name)) {
            response = "(playername) (extPropName | <list>)";
        } else {
            EntityPlayerMP player = getEntityPlayerMP(name);
            if (player == null) {
                response = "Not Found Player: " + name;
            } else {
                final String prop = arg(args,i++);//i < sz ? args[i++] : null;

                if (isNullOrEmpty(prop) || isCmd(prop, "list", "ls")) {
                    try {
                        Field field = Entity.class.getDeclaredField("extendedProperties");
                        Map<String, IExtendedEntityProperties> map = null;
                        if (field != null) {
                            field.setAccessible(true);
                            map = (Map<String, IExtendedEntityProperties>)field.get(player);
                        }
                        if (map != null && map.size() > 0) {
                            StringBuilder sb = new StringBuilder("--- ExtendedProperties[").append( name ).append("] ---\n");
                            for (String key : map.keySet()) {
                                sb.append(key).append("; ");
                            }
                            response = sb.toString();
                        } else {
                            response = "no extendedProperties in " + name;
                        }
                    }
                    catch (Exception e) {
                    }
                }
                else if (!isNullOrEmpty(prop)) {
                    IExtendedEntityProperties eep = player.getExtendedProperties(prop);
                    if (eep == null) {
                        response = "Not Found Prop: " + prop;
                    } else {
                        NBTTagCompound nbt = new NBTTagCompound();
                        eep.saveNBTData(nbt);
                        response = nbt.toString();
                    }
                }
                else response = "UKNOWN";
            }
        }
        toSender(sender, response);
    }

    /**
     * Entity.customEntityData
     * Например ТФК в данном НБТ хранит расширенные слоты инвентаря
     * @param sender
     * @param args
     */
    public void cmdPlayerCustomEntityData(ICommandSender sender, String[] args, int i) {
        //int i = 1;
        String response = "?";
        final String name = arg(args,i++);
        if (isNullOrEmpty(name) || isCmd(name, "help", "h")) {
            response = "(playername) (customEntityDataTag | <list>)";
        }
        else {
            NBTTagCompound customEntityData = null;
            EntityPlayerMP player = getEntityPlayerMP(name);//from online & offline via cache
            if (player != null) {//flag - offline-player
                customEntityData = player.getEntityData();
            }

            if (customEntityData == null) {
                response = "Not found for " + name;
            }
            else {
                if (customEntityData.func_150296_c().size() == 0) {
                    response = "CustomEntityData for "+ name + " is Empty";
                }
                else {
                    final String tagName = arg(args,i++);
                    //list of root tag in nbt CustomEntityData
                    if ("list".equals(tagName) || "ls".equals(tagName)) {
                        Set set = customEntityData.func_150296_c();
                        StringBuilder sb = new StringBuilder();
                        for (Object obj: set) {
                            sb.append(obj).append("; ");
                        }
                        response = sb.toString();
                    }
                    else if (!isNullOrEmpty(tagName)) {
                        NBTBase b = customEntityData.getTag(tagName);
                        if (b == null) {
                            response = "Not Found: " + tagName;
                        } else {
                            //String act = arg(args, i++);
                            //if ("remove".equals(act)|| "rm".equals(act)) {
                            //    response = "TODO";
                            //    customEntityData.removeTag(tagName);
                            //    response = "Changes saved:" + XPlayer.savePlayerDataToDisk(player);
                            //}
                            //view
                            //else {
                                response = b.toString();
                            //}
                        }
                    }
                    else {
                        response = customEntityData.toString();
                    }
                }
            }
        }
        toSender(sender, response);
    }

    private void cmdPlayerStatus(ICommandSender sender, String[] args, int i) {
        String response = "?";
        final String name = arg(args,i++);
        if (isNullOrEmpty(name) || isCmd(name, "help", "h")) {
            response = "(playername)";
        } else {
            EntityPlayerMP player = getEntityPlayerMP(name);//from online & offline via cache
            if (player == null) {
                response = "not found";
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Name: ").append(player.getCommandSenderName()).append('\n')
                  .append("UUID: ").append(player.getGameProfile().getId()).append('\n')
                  .append("Coord: ").append((int)player.posX).append(' ').append((int)player.posY).append(' ').append((int)player.posZ).append(" Dim:").append(player.dimension).append('\n')
                  .append("Spawn: ").append(player.getBedLocation(0)).append('\n')
                  .append("XpLevel: ").append(player.experienceLevel).append(" (").append(player.experienceTotal).append(")\n")
                  .append("GameType: ").append(player.theItemInWorldManager.getGameType()).append('\n')
                  .append("Health: ").append(player.getHealth()).append('\n')
                  .append("selectedItemSlot: ").append(player.inventory.currentItem).append(' ')
                  ;
                response = sb.toString();
            }
        }
        toSender(sender, response);
    }



}
