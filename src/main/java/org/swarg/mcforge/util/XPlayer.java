package org.swarg.mcforge.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.UUID;
import java.util.Map;
import java.lang.reflect.Field;
import com.mojang.authlib.GameProfile;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.UsernameCache;
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

    private static File playersDirectory;
    //ServerSide
    public static File getPlayersDirectoryFile() {
        if (playersDirectory == null) {
            try {
                ServerConfigurationManager scm = MinecraftServer.getServer().getConfigurationManager();
                Field f = null;
                try { f = ServerConfigurationManager.class.getDeclaredField("field_72412_k");} catch (Exception e) {;}
                if (f == null) {
                    f = ServerConfigurationManager.class.getDeclaredField("playerNBTManagerObj");
                }
                IPlayerFileData pfd = null;
                if (f != null) {
                    f.setAccessible(true);
                    pfd = (IPlayerFileData)f.get(scm);
                }
            
                if (pfd instanceof SaveHandler) {
                    Field f2 = null;
                    try { f2 = SaveHandler.class.getDeclaredField("field_75771_c");} catch (Exception e) {;}
                    if (f2 == null) {
                        try { f2 = SaveHandler.class.getDeclaredField("playersDirectory");} catch (Exception e) {;}
                    }
                    if (f2 != null) {
                        f2.setAccessible(true);
                        playersDirectory = (File) f2.get(pfd);
                    }
                }
            }
            catch (Exception e) {
            }
        }
        return playersDirectory;
    }

    //online & offline
    public static UUID getPlayerUUID(String name) {//GameProfile
        EntityPlayer player = MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);//online
        if (player != null && player.getGameProfile() != null) {
             return player.getGameProfile().getId();
        } else {
            //конкретные данные uuid кэшируемые в момент входа игрока на сервер
            //и сохраняемые в "usernamecache.json" похоже данный кэш никогда не очищается.
            return getUUIDForPlayerNameFromForgeUsernameCache(name);

            /*если имя задано несуществующего игрока вернёт новый сгенерированный
            под него профайл, но файлов никаких новых на диск не сохранит
            это не самый лучший вариант т.к. данный метод генерит новый uuid для несуществующего игрока
            Данный способ подходит только для лицензионых серверов либо для серверов
            без своей регистрации игроков (иначе uuid будут не совпадать с лежащими на сервере)
            это содержимое файла usercache.json на основе которого инстанцируется PlayerProfileCache
            return MinecraftServer.getServer().func_152358_ax().func_152655_a(name);*/
        }
    }

    /**
     * based Forge:UsernameCache
     * Кэш форжа uuid-имяигрока заполняется при входе игроков на сервер
     * usernamecache.json
     * @param name
     * @return
     */
    public static UUID getUUIDForPlayerNameFromForgeUsernameCache(String name) {
        if (!isNullOrEmpty(name)) {

            //если вместо имени указан тектовый вариант uuid
            //xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
            if (name.length() == 36 && name.indexOf('-') == 8 && name.lastIndexOf('-') == 23) {
                try {
                    /*DEBUG*/System.out.println("Name is UUID!!!");
                    return UUID.fromString(name);
                } catch (Exception e) {;}
            }

            if (UsernameCache.getMap().containsValue(name)) {
                Map<UUID, String> map = null; //net.minecraftforge.common.UsernameCache.getMap();//copy
                try {
                    //вопрос: что быстрее и дешевле? постоянно копировать всю мапу для поиска нужного имени
                    //или получать к ней доступ через рефлексию?
                    Field f = net.minecraftforge.common.UsernameCache.class.getDeclaredField("map");
                    f.setAccessible(true);
                    map = (Map<UUID, String>) f.get(null);
                }
                catch (Exception e) {
                }
                Iterator iter = map.entrySet().iterator();
                UUID uuid = null;
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    if (name.equals(entry.getValue())) {
                        uuid = (UUID) entry.getKey();
                    }
                }
                return uuid;
            }
        }
        return null;
    }

    public static int getForgeUsernameCacheSize() {
        Map<UUID, String> map = null; //net.minecraftforge.common.UsernameCache.getMap();//copy
        try {
            Field f = net.minecraftforge.common.UsernameCache.class.getDeclaredField("map");
            f.setAccessible(true);
            map = (Map<UUID, String>) f.get(null);
            return map.size();
        }
        catch (Exception e) {
        }
        return -1;//error
    }

    /**
     * Если игрок онлайн - его полное текущее состоания будет записано в нбт
     * иначе будет произведён поиск данных о игроке по его имени и прочитан с диска
     * @param name поддерживает как имя игрока( так и его uuid для оффлайн игроков)
     * @param onlyFromDisk from disk by player name
     * @param uuidBox для передачи uuid запрашиваемого игрока
     * @return
     */
    public static NBTTagCompound getPlayerFullNBTData(String name, boolean onlyFromDisk, UUID[] uuidBox) {
        try {
            EntityPlayer player = onlyFromDisk ? null : MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);//online

            if (player != null) {
                NBTTagCompound data = new NBTTagCompound();//online
                player.writeToNBT(data);
                return data;
            } else if (!isNullOrEmpty(name)) {
                UUID uuid = getPlayerUUID(name); //for offline from forge:usernamecache //Exists

                if (uuid != null) {
                    playersDirectory = getPlayersDirectoryFile();
                    File file = new File(playersDirectory, uuid.toString() + ".dat");
                    if (file.exists() && file.isFile()) {
                        NBTTagCompound data = CompressedStreamTools.readCompressed(new FileInputStream(file));
                        if (uuidBox != null && uuidBox.length > 0) {
                            uuidBox[0] = uuid;
                        }
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

    /**
     * create instance of offline EntityPlayer
     * @param name
     * @return
     */
    public static EntityPlayerMP getOfflineEntityPlayerMP(String name) {
        final MinecraftServer mcServer = MinecraftServer.getServer();
        if (!isNullOrEmpty(name) && mcServer != null) {
            UUID[] box = new UUID[1];
            NBTTagCompound data = getPlayerFullNBTData(name, true, box);
            //GameProfile gameprofile = mcServer.func_152358_ax().func_152655_a(name);
            if (data != null && box[0] != null) {
                GameProfile gameprofile = new GameProfile(box[0], name);
                WorldServer world = mcServer.worldServerForDimension(0);
                EntityPlayerMP playerMp = new EntityPlayerMP(mcServer, world, gameprofile, new ItemInWorldManager(world));
                mcServer.getConfigurationManager().readPlayerDataFromFile(playerMp);//NBTTagCompound nbt = .
                return playerMp;
            }
        }
        return null;
    }


    //SaveHandler
    public static boolean writePlayerData(UUID uuid, NBTTagCompound nbt) {
        try {
            getPlayersDirectoryFile();
            File fileTemp = new File(playersDirectory, uuid.toString() + ".dat.tmp");//player.getUniqueID().toString() проверить это! это метод из Entity
            File fileOrgl = new File(playersDirectory, uuid.toString() + ".dat");
            CompressedStreamTools.writeCompressed(nbt, new FileOutputStream(fileTemp));

            if (fileOrgl.exists()) {
                fileOrgl.delete();
            }

            fileTemp.renameTo(fileOrgl);
            //net.minecraftforge.event.ForgeEventFactory.firePlayerSavingEvent(p_75753_1_, this.playersDirectory, p_75753_1_.getUniqueID().toString());
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            //logger.warn("Failed to save player data for " + p_75753_1_.getCommandSenderName());
        }
        return false;
    }

}
