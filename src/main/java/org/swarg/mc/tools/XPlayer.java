package org.swarg.mc.tools;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import com.mojang.authlib.GameProfile;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import static net.minecraft.util.StringUtils.isNullOrEmpty;

/**
 * 17-02-21
 * @author Swarg
 */
public class XPlayer {

    public static boolean isOpWithEmptyInventory(EntityPlayerMP p) {
        if (p != null && /*Op*/MinecraftServer.getServer().getConfigurationManager().func_152596_g( p.getGameProfile()) && p.inventory != null) {
            //p.inventoryContainer;
            //p.inventory.
            ItemStack[] ainv = p.inventory.armorInventory;
            ItemStack[] minv = p.inventory.mainInventory;
            final int asz = ainv == null ? 0:ainv.length;
            final int msz = minv == null ? 0:minv.length;
            if (asz > 0 && msz > 0) {
                for (int i = 0; i < asz; i++) {
                    if (ainv[i] != null) {
                        return false;
                    }
                }
                for (int i = 0; i < msz; i++) {
                    if (minv[i] != null) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Синхронизация инветрарей двух игроков
     * например для просмотра путём переноса инвентаря игрока scr к игроку dst
     * [WARN] dst-игрок теряет всё что у него было ранее и его инвентарь
     * становится копией игрока scr. Переносятся в том числе и расширенные
     * инвентари других модов, если их загрузка реализована в
     * player.inventory.readFromNBT
     * а данные расширенного инвентаря храненятся в Entity.customEntityData
     * @param src
     * @param dst
     * @param copyCed - customEntityData (Can store extended mods inventories)
     */
    public static boolean copyInvenotryAndCustomData(EntityPlayer src, EntityPlayer dst, boolean copyCed) {
        if (src != null && dst != null) {
            NBTTagList nbt = new NBTTagList();
            src.inventory.writeToNBT(nbt);//src-other
            if (copyCed) {
                NBTTagCompound customEntityData = src.getEntityData();
                //todo inventory-like only check via NBTTagList id damage count
                try {
                    Field fced = Entity.class.getDeclaredField("customEntityData");
                    fced.setAccessible(true);
                    fced.set(dst, customEntityData.copy());//copy??
                }
                catch (Exception e) {
                    //String msg = "Fail to sync customEntityData! "+e.getMessage();
                    ///*DEBUG*/System.out.println(msg);
                }
            }
            dst.inventory.readFromNBT(nbt);//opPlayer
            return true;
        }
        return false;
    }

    /**
     * Get instance of offline player
     * @param name
     * @return
     */
    public static EntityPlayerMP getEntityPlayerMPFromDisk(String name) {
        final MinecraftServer mcServer = MinecraftServer.getServer();
        if (!isNullOrEmpty(name) && mcServer != null) {
            GameProfile gameprofile = mcServer.func_152358_ax().func_152655_a(name);
            if (gameprofile != null) {
                EntityPlayerMP playerMp = new EntityPlayerMP(mcServer, mcServer.worldServerForDimension(0), gameprofile,
                        new net.minecraft.server.management.ItemInWorldManager(mcServer.worldServerForDimension(0)));
                mcServer.getConfigurationManager().readPlayerDataFromFile(playerMp);//NBTTagCompound nbt = .
                return playerMp;
            }
        }
        return null;
    }

        /**
     * Without statiscics
     * scm.writePlayerData(p)
     * @param p
     * @return
     */
    public static boolean savePlayerDataToDisk(EntityPlayer p) {
        if (p != null) {
            try {
                IPlayerFileData pfd = getIPlayerFileData();
                if (pfd != null) {
                    pfd.writePlayerData(p);
                    return true;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    //ServerSide
    public static IPlayerFileData getIPlayerFileData() {
        try {
            ServerConfigurationManager scm = MinecraftServer.getServer().getConfigurationManager();
            Field f = null;
            try { f = ServerConfigurationManager.class.getDeclaredField("field_72412_k");} catch (Exception e) {;}
            if (f == null) {
                f = ServerConfigurationManager.class.getDeclaredField("playerNBTManagerObj");
            }
            if (f != null) {
                f.setAccessible(true);
                IPlayerFileData pfd = (IPlayerFileData)f.get(scm);
                return pfd;
            }
        }
        catch (Exception e) {
        }
        return null;
    }

    private static File playersDirectory;
    public static File getPlayersDirectoryFile() {
        if (playersDirectory == null) {
            IPlayerFileData pfd = getIPlayerFileData();//SaveHandeler
            try {
                if (pfd instanceof SaveHandler) {
                    Field f = null;
                    try { f = SaveHandler.class.getDeclaredField("field_75771_c");} catch (Exception e) {;}
                    if (f == null) {
                        try { f = SaveHandler.class.getDeclaredField("playersDirectory");} catch (Exception e) {;}
                    }
                    if (f != null) {
                        f.setAccessible(true);
                        playersDirectory = (File) f.get(pfd);
                    }
                }
            }
            catch (Exception e) {
            }
        }
        return playersDirectory;
    }
    public static boolean isExistGameProfile(GameProfile gp) {
        if (gp != null) {
            getPlayersDirectoryFile();
            if (playersDirectory != null) {
                File file = new File(playersDirectory, gp.getId().toString() + ".dat");
                return (file.exists() && file.isFile());
            }
        }
        return false;
    }

    /**
     * from disk by player name
     * @param name
     * @return
     */
    public static NBTTagCompound getPlayerNbtData(String name, boolean onlyFromDisk) {
        try {
            EntityPlayer player = onlyFromDisk ? null : MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);//online

            if (player != null) {
                NBTTagCompound data = new NBTTagCompound();//online
                player.writeToNBT(data);
                return data;
            } else {
                GameProfile gameprofile = getPlayerGameProfile(name); //offline  //Exists
                if (gameprofile != null) {
                    playersDirectory = getPlayersDirectoryFile();
                    File file = new File(playersDirectory, gameprofile.getId().toString() + ".dat");
                    if (file.exists() && file.isFile()) {
                        NBTTagCompound data = CompressedStreamTools.readCompressed(new FileInputStream(file));
                        return data;
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //online & offline
    public static GameProfile getPlayerGameProfile(String name) {
        EntityPlayer player = MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);//online
        if (player != null) {
            return player.getGameProfile();
        } else {
            //если имя задано несуществующего игрока вернёт новый сгенерированный под него профайл, но файлов никаких новых на диск не сохранит
            //это не самый лучший вариант т.к. данный метод генерит новый uuid для несуществующего игрока
            return MinecraftServer.getServer().func_152358_ax().func_152655_a(name);
        }
    }

}
