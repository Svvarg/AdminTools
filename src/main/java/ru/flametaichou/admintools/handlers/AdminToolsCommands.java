package ru.flametaichou.admintools.handlers;

import java.io.IOException;
import java.net.*;
import java.util.*;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StringUtils;
import net.minecraft.world.World;
import net.minecraft.world.WorldManager;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import ru.flametaichou.admintools.AdminTools;
import ru.flametaichou.admintools.util.ConfigHelper;
import ru.flametaichou.admintools.util.Logger;

public class AdminToolsCommands extends CommandBase
{
    private final List<String> aliases;
    private final static Random random = new Random();

    public AdminToolsCommands()
    {
        aliases = new ArrayList<String>();
        aliases.add("atools");
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
        return "atools";
    }

    @Override
    public String getCommandUsage(ICommandSender var1)
    {
        return "/atools <mobclear/chestclear/restoreplayer/chunkregen/mobfind/findte/findblock/entityinfo/tileentityinfo/iteminfo/automessage>";
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
            if (argString.length == 0) {
                sender.addChatMessage(new ChatComponentText("/atools <mobclear (mob, range) / chestclear (range) / restoreplayer / chunkregen / mobfind (mob, range) / findte (te, range)  / findblock (block, range)  / entityinfo (range) / tileentityinfo (range) / iteminfo / automessage (reload)>"));
                return;
            }
            if (argString[0].equals("mobclear")) {
                if (sender instanceof EntityPlayer) {
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
                    List e = player.worldObj.getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB.getBoundingBox(player.posX - radius, player.posY - radius, player.posZ - radius, (player.posX + radius), (player.posY + radius), (player.posZ + radius)));
                    if (e.size() > 0) {
                        int count = 0;
                        for (Object obj : e) {
                            EntityLiving entityLiving = (EntityLiving) obj;
                            if (entityLiving.getClass().getName().toLowerCase().contains(mobname.toLowerCase())) {
                                entityLiving.setDead();
                                entityLiving.worldObj.removeEntity(entityLiving);
                                entityLiving.worldObj.onEntityRemoved(entityLiving);
                                WorldManager worldManager = new WorldManager(MinecraftServer.getServer(), (WorldServer) entityLiving.worldObj);
                                worldManager.onEntityDestroy(entityLiving);

                                count++;
                            }
                        }
                        sender.addChatMessage(new ChatComponentTranslation("mobclear.done", count));
                    }
                }
                return;
            }

            if (argString[0].equals("mobfind")) {
                if (sender instanceof EntityPlayer) {
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
                    if (radius > 200) {
                        sender.addChatMessage(new ChatComponentTranslation("mobclear.range50"));
                        radius = 200;
                    }
                    List e = player.worldObj.getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB.getBoundingBox(player.posX - radius, player.posY - radius, player.posZ - radius, (player.posX + radius), (player.posY + radius), (player.posZ + radius)));
                    if (e.size() > 0) {
                        for (Object obj : e) {
                            EntityLiving entityLiving = (EntityLiving) obj;
                            if (entityLiving.getClass().getName().toLowerCase().contains(mobname.toLowerCase())) {
                                sender.addChatMessage(new ChatComponentTranslation(entityLiving.getClass().getName() + " - x:" + entityLiving.posX + " y:" + entityLiving.posY + " z:" + entityLiving.posZ));
                            }
                        }
                    }
                }
                return;
            }

            if (argString[0].equals("findte")) {
                if (sender instanceof EntityPlayer) {
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
                    if (radius > 200) {
                        sender.addChatMessage(new ChatComponentTranslation("mobclear.range50"));
                        radius = 200;
                    }

                    int x = (int) player.posX ;
                    int y = (int) player.posY ;
                    int z = (int) player.posZ ;
                    List<TileEntity> e = new ArrayList<TileEntity>();
                    for (int block_x = x - radius; block_x < x + radius; block_x++) {
                        for (int block_y = y - radius; block_y < y + radius; block_y++) {
                            for (int block_z = z - radius; block_z < z + radius; block_z++) {
                                TileEntity te = world.getTileEntity(block_x, block_y, block_z);
                                if (Objects.nonNull(te)) {
                                    e.add(te);
                                }
                            }
                        }
                    }

                    if (e.size() > 0) {
                        for (Object obj : e) {
                            TileEntity tileEntity = (TileEntity) obj;
                            if (tileEntity.getClass().getName().toLowerCase().contains(mobname.toLowerCase())) {
                                sender.addChatMessage(new ChatComponentTranslation(tileEntity.getClass().getName() + " - x:" + tileEntity.xCoord + " y:" + tileEntity.yCoord + " z:" + tileEntity.zCoord));
                            }
                        }
                    }
                }
                return;
            }

            if (argString[0].equals("findblock")) {
                if (sender instanceof EntityPlayer) {
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
                    if (radius > 200) {
                        sender.addChatMessage(new ChatComponentTranslation("mobclear.range50"));
                        radius = 200;
                    }
                    int x = (int) player.posX ;
                    int y = (int) player.posY ;
                    int z = (int) player.posZ ;
                    for (int block_x = x - radius; block_x < x + radius; block_x++) {
                        for (int block_y = y - radius; block_y < y + radius; block_y++) {
                            for (int block_z = z - radius; block_z < z + radius; block_z++) {
                                Block b = world.getBlock(block_x, block_y, block_z);
                                if (Objects.nonNull(b) && b.getUnlocalizedName().toLowerCase().contains(mobname.toLowerCase())) {
                                    sender.addChatMessage(new ChatComponentTranslation(b.getClass().getName() + " - x:" + block_x + " y:" + block_y + " z:" + block_z));
                                }
                            }
                        }
                    }
                }
                return;
            }

            if (argString[0].equals("entityinfo")) {
                if (sender instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) sender;
                    if (argString.length < 2) {
                        sender.addChatMessage(new ChatComponentTranslation("mobclear.norange"));
                        return;
                    }
                    int radius = Integer.parseInt(argString[1]);
                    if (radius < 1) {
                        radius = 0;
                    }
                    if (radius > 16) {
                        sender.addChatMessage(new ChatComponentTranslation("mobclear.range50"));
                        radius = 16;
                    }
                    List e = player.worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(player.posX - radius, player.posY - radius, player.posZ - radius, (player.posX + radius), (player.posY + radius), (player.posZ + radius)));
                    if (e.size() > 0) {
                        for (Object obj : e) {
                            if (obj instanceof EntityPlayer) {
                                EntityPlayer tempPlayer = (EntityPlayer) obj;
                                if (tempPlayer.getDisplayName().equals(player.getDisplayName())) {
                                    continue;
                                }
                            }
                            if (obj instanceof EntityLiving) {
                                EntityLiving entityLiving = (EntityLiving) obj;
                                String type = "EntityLiving";
                                sender.addChatMessage(new ChatComponentText(String.format("EntityInfo: %s (%s) (#%s) Type: %s",
                                        entityLiving.getCommandSenderName(),
                                        entityLiving.getCustomNameTag(),
                                        entityLiving.getEntityId(),
                                        type)));
                                sender.addChatMessage(new ChatComponentText("Class: " + entityLiving.getClass().getName() + ", posision x:" + entityLiving.posX + " y:" + entityLiving.posY + " z:" + entityLiving.posZ));
                                NBTTagCompound nbtData = new NBTTagCompound();
                                entityLiving.writeEntityToNBT(nbtData);
                                sender.addChatMessage(new ChatComponentText(String.format("NBT: %s", nbtData)));
                            } else {
                                Entity entity = (Entity) obj;
                                String type = "Entity";
                                sender.addChatMessage(new ChatComponentText(String.format("EntityInfo: %s (#%s) Type: %s",
                                        entity.getCommandSenderName(),
                                        entity.getEntityId(),
                                        type)));
                                sender.addChatMessage(new ChatComponentText("Class: " + entity.getClass().getName() + ", posision x:" + entity.posX + " y:" + entity.posY + " z:" + entity.posZ));
                                NBTTagCompound nbtData = new NBTTagCompound();
                                entity.writeToNBT(nbtData);
                                sender.addChatMessage(new ChatComponentText(String.format("NBT: %s", nbtData)));
                            }
                        }
                    }
                }
                return;
            }

            if (argString[0].equals("tileentityinfo")) {
                if (sender instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) sender;
                    if (argString.length < 2) {
                        sender.addChatMessage(new ChatComponentTranslation("mobclear.norange"));
                        return;
                    }
                    int radius = Integer.parseInt(argString[1]);
                    if (radius < 1) {
                        radius = 0;
                    }

                    /*flag to search TileEntities only in the sender's chunk*/
                    boolean onlyInOneChunk = argString.length > 2 && argString[2].equals("-chunk");

                    if (radius > 16) {
                        sender.addChatMessage(new ChatComponentTranslation("mobclear.range50"));
                        radius = onlyInOneChunk ? 32 : /*Area*/16;
                    }

                    int x = (int) player.posX ;
                    int y = (int) player.posY ;
                    int z = (int) player.posZ ;

                    List<TileEntity> e = new ArrayList<TileEntity>();

                    //[CHUNK]Search TE only in player chunk with distance limit by Y Coord
                    if (onlyInOneChunk) {
                            int cx = player.chunkCoordX;
                            int cz = player.chunkCoordZ;
                            Chunk chunk = ((EntityPlayer) sender).worldObj.getChunkProvider().provideChunk(cx, cz);
                            if (chunk != null) {
                                Map teMap = chunk.chunkTileEntityMap;
                                if (teMap != null && !teMap.isEmpty()) {
                                    Iterator iter = teMap.values().iterator();
                                    while (iter.hasNext()) {
                                        Object obj = iter.next();
                                        if (obj instanceof TileEntity) {
                                            TileEntity te = (TileEntity) obj;
                                            int distance = Math.abs(te.yCoord - y);
                                            if (distance <= radius) {
                                                e.add(te);
                                            }
                                        }
                                    }
                                }
                            }
                    }
                    //[AREA]Search TE by radius from player Coords as center
                    else {
                            for (int block_x = x - radius; block_x < x + radius; block_x++) {
                                for (int block_y = y - radius; block_y < y + radius; block_y++) {
                                    for (int block_z = z - radius; block_z < z + radius; block_z++) {
                                        TileEntity te = world.getTileEntity(block_x, block_y, block_z);
                                        if (Objects.nonNull(te)) {
                                            e.add(te);
                                        }
                                    }
                                }
                            }
                    }

                    if (e.size() > 0) {
                        for (Object obj : e) {
                            TileEntity te = (TileEntity) obj;
                            /*Used a ChatComponentText instead of a chatComponentTranslation here because the latter can cause crashes*/
                            sender.addChatMessage(new ChatComponentText(String.format("TileEntityInfo: %s Block: %s (%s) Metadata: %s",
                                    te.getClass().getSimpleName(),
                                    te.getBlockType().getUnlocalizedName(),
                                    te.getBlockType().getLocalizedName(),
                                    String.valueOf(te.blockMetadata))));
                            sender.addChatMessage(new ChatComponentText("Class: " + te.getClass().getName() + ",  x:" + te.xCoord + " y:" + te.yCoord + " z:" + te.zCoord));
                            NBTTagCompound nbtData = new NBTTagCompound();
                            te.writeToNBT(nbtData);
                            sender.addChatMessage(new ChatComponentText("NBT: " + nbtData));
                        }
                    } else {
                        sender.addChatMessage(new ChatComponentText("Not found any TileEntities"));
                    }
                }
                return;
            }

            if (argString[0].equals("chestclear")) {
                if (sender instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) sender;
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
                                if (te != null && !player.worldObj.getTileEntity(x, y, z).getClass().getName().contains("TileEntityMapFrame")) {
                                    if (te instanceof IInventory) {
                                        IInventory inventory = ((IInventory) te);
                                        boolean flagCleaned = false;
                                        if (inventory.getSizeInventory() > 1) {
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
                    }
                    sender.addChatMessage(new ChatComponentTranslation("chestclear.done", count));
                }
                return;
            }

            if (argString[0].equals("restoreplayer")) {

                if (argString.length < 2) {
                    sender.addChatMessage(new ChatComponentTranslation("restoreplayer.noname"));
                    return;
                }

                String playername = argString[1];
                for (WorldServer worldServer : MinecraftServer.getServer().worldServers) {
                    for (Object obj :  worldServer.playerEntities) {
                        EntityPlayerMP player = (EntityPlayerMP) obj;
                        if (player.getDisplayName().toLowerCase().startsWith(playername.toLowerCase())) {
                            EntityPlayerMP targetPlayer = player;
                            targetPlayer.setHealth(targetPlayer.getMaxHealth());
                            targetPlayer.setAbsorptionAmount(0);
                            targetPlayer.heal(targetPlayer.getMaxHealth());
                        }
                    }
                }

                sender.addChatMessage(new ChatComponentTranslation("restoreplayer.done"));
                return;
            }

            if (argString[0].equals("chunkregen")) {
                if (sender instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) sender;
                    try {
//                        ChunkProviderServer chunkServer = (ChunkProviderServer) world.getChunkProvider();
//
//                        List<ChunkCoordIntPair> toUnload = new ArrayList<ChunkCoordIntPair>();
//                        for (Object obj : chunkServer.loadedChunks) {
//                            Chunk chunk = (Chunk) obj;
//                            toUnload.add(chunk.getChunkCoordIntPair());
//                        }
//
//                        for (ChunkCoordIntPair pair : toUnload) {
//                        }

                        Chunk oldChunk = world.getChunkFromChunkCoords(player.chunkCoordX, player.chunkCoordZ);
                        WorldServer worldServer = (WorldServer) world;
                        ChunkProviderServer chunkProviderServer = worldServer.theChunkProviderServer;
//                IChunkProvider chunkProviderGenerate = ObfuscationReflectionHelper.getPrivateValue(ChunkProviderServer.class, chunkProviderServer, "d", "field_73246_d");
                        IChunkProvider chunkProviderGenerate = chunkProviderServer.currentChunkProvider;

                        Chunk newChunk = chunkProviderGenerate.provideChunk(oldChunk.xPosition, oldChunk.zPosition);

                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                for (int y = 0; y < world.getHeight(); y++) {
                                    Block blockID = newChunk.getBlock(x, y, z);
                                    int metadata = newChunk.getBlockMetadata(x, y, z);
                                    worldServer.setBlock(x + oldChunk.xPosition * 16, y, z + oldChunk.zPosition * 16, blockID, metadata, 2);
                                    TileEntity tileEntity = newChunk.getTileEntityUnsafe(x, y, z);
                                    if (tileEntity != null) {
                                        worldServer.setTileEntity(x + oldChunk.xPosition * 16, y, z + oldChunk.zPosition * 16, tileEntity);
                                    }
                                }
                            }
                        }

                        oldChunk.isTerrainPopulated = false;
                        chunkProviderGenerate.populate(chunkProviderGenerate, oldChunk.xPosition, oldChunk.zPosition);
                        oldChunk.needsSaving(true);
                        sender.addChatMessage(new ChatComponentTranslation("chunkregen.done"));
                    } catch (Exception e) {
                        sender.addChatMessage(new ChatComponentTranslation("chunkregen.error"));
                        e.printStackTrace();
                    }
                }
                return;
            }

            if (argString[0].equals("iteminfo")) {

                if (!(sender instanceof EntityPlayer)) {
                    return;
                }

                EntityPlayer player = (EntityPlayer) sender;
                ItemStack is = player.getHeldItem();

                if (Objects.isNull(is)) {
                    return;
                }

                String name1 = "Unknown";
                try {
                    name1 = is.getUnlocalizedName();
                } catch (NoClassDefFoundError e) {
                    Logger.error("Error while getUnlocalizedName(), itemstack: " + is);
                }
                String name2 = "Unknown";
                try {
                    name2 = is.getDisplayName();
                } catch (NoClassDefFoundError e) {
                    Logger.error("Error while getDisplayNameVb(), itemstack: " + is);
                }
                int id = Item.getIdFromItem(is.getItem());
                int data = is.getItem().getDamage(is);

                sender.addChatMessage(new ChatComponentTranslation(String.format("ItemInfo: %s (%s) (#%s:%s)", name2, name1, id, data)));
                sender.addChatMessage(new ChatComponentTranslation(String.format("Count: %s", is.stackSize)));
                sender.addChatMessage(new ChatComponentTranslation(String.format("NBT: %s", is.stackTagCompound)));

                return;
            }

            if (argString[0].equals("time")) {

                WorldServer worldServer = (WorldServer) world;

                sender.addChatMessage(new ChatComponentText(String.format(
                        "```" +
                                getWeatherString(worldServer) + ". " +
                                getTimeString(worldServer) + "." +
                                "```"
                )));
                return;
            }

            if (argString[0].equals("ping")) {

                String address = "www.google.com";
                Socket s = new Socket();
                try {
                    SocketAddress a = new InetSocketAddress(address, 80);
                    int timeoutMillis = 2000;
                    long start = System.currentTimeMillis();
                    s.connect(a, timeoutMillis);
                    long stop = System.currentTimeMillis();
                    long latency = stop - start;

                    sender.addChatMessage(new ChatComponentText(String.format(
                            "```" +
                                    "Пинг " + address + ": задержка " + latency + "мс." +
                                    "```"
                    )));
                } catch (Exception e) {
                    sender.addChatMessage(new ChatComponentText(String.format(
                            "```" +
                                    "Пинг " + address + ": Ошибка!" +
                                    "```"
                    )));
                } finally {
                    try {
                        s.close();
                    } catch (IOException e) {
                        Logger.error("Error on closing socket: " + ExceptionUtils.getRootCauseMessage(e));
                    }
                }

                return;
            }

            if (argString[0].equals("story")) {

                WorldServer worldServer = (WorldServer) world;

                sender.addChatMessage(new ChatComponentText(String.format(
                                "```" +
                                        getWeatherString(worldServer) + ". " +
                                        getTimeString(worldServer) + "\n\n" +
                                        "..." + getEventString() + "\n\n" +
                                        "..." + getDeathString() +
                        "```"
                )));
                return;
            }

            if (argString[0].equals("stat")) {

                sender.addChatMessage(new ChatComponentText(String.format(
                        "```" +
                                "Вот что случилось за последнее время: " +
                                getAllEventsString() +
                                "при этом из травмпункта сообщают что " +
                                getAllDeathsString() +
                                "и никто не знает, что случится в следующую минуту." +
                                "```"
                )));
                return;
            }

            if (argString[0].equals("automessage")) {
                if (argString.length == 1) {
                    sender.addChatMessage(new ChatComponentText("/atools automessage reload"));
                    return;
                }

                if (argString[1].equals("reload")) {
                    ConfigHelper.reloadConfig();
                    sender.addChatMessage(new ChatComponentTranslation("automessage.reload.done"));
                }
            }
        }
    }

    public static String getEventString() {
        if (AdminTools.handler.playerMobsMap.entrySet().size() == 0) {
            return "пока ничего не произошло.";
        }
        try {
            int number = random.nextInt(AdminTools.handler.playerMobsMap.entrySet().size());
            Map.Entry entry = (Map.Entry) AdminTools.handler.playerMobsMap.entrySet().toArray()[number];
            Map<String, Integer> mobsMap = (Map<String, Integer>) entry.getValue();
            number = random.nextInt(mobsMap.entrySet().size());
            Map.Entry mobEntry = (Map.Entry) mobsMap.entrySet().toArray()[number];

            String playerString = (String) entry.getKey();
            String actionString = getRandomKillWord() + " " + mobEntry.getValue() + " " + mobEntry.getKey();

            actionString = processStringNames(actionString, playerString);

            return String.format(getRandomActionPhrase(), playerString, actionString);
        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка!";
        }
    }

    public static String getAllEventsString() {
        String string = "";
        for (Map.Entry entry : AdminTools.handler.playerMobsMap.entrySet()) {
            Map<String, Integer> mobsMap = (Map<String, Integer>) entry.getValue();
            string = string + entry.getKey() + " ";
            for (Map.Entry mobEntry : mobsMap.entrySet()) {
                string = string + getRandomKillWord() + " " + mobEntry.getValue() + " " + mobEntry.getKey() + ", ";
            }
        }
        if (StringUtils.isNullOrEmpty(string)) {
            string = "ничего не случилось, ";
        }
        return string;
    }

    public static String getDeathString() {
        if (AdminTools.handler.deathMap.entrySet().size() == 0) {
            return "пока никто не умер.";
        }
        try {
            int number = random.nextInt(AdminTools.handler.deathMap.entrySet().size());
            Map.Entry entry = (Map.Entry) AdminTools.handler.deathMap.entrySet().toArray()[number];

            String playerString = (String) entry.getKey();
            String countString = ((Integer) entry.getValue()).toString();
            String actionString = getRandomDeathWord();

            actionString = processStringNames(actionString, playerString);

            return String.format(getRandomDeathPhrase(), playerString, actionString, countString);
        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка!";
        }
    }

    private static String processStringNames(String actionString, String playerString) {
        if ("tetster".equals(playerString)) {
            actionString = "дюпал";
        }

        if ("zagrizer".equals(playerString)) {
            actionString = "ругался матом";
        }

        if ("Morgana".equals(playerString)) {
            actionString = "просила поставить Pam's Harvestcraft";
        }

        if ("DenPrime".equals(playerString)) {
            actionString = "ругал сервер легаси";
        }

        return actionString;
    }

    public static String getAllDeathsString() {
        String string = "";
        for (Map.Entry entry : AdminTools.handler.deathMap.entrySet()) {
            string = string + entry.getKey() + " " + getRandomDeathWord() + " " + entry.getValue() + " раз, ";
        }
        if (StringUtils.isNullOrEmpty(string)) {
            string = "никто не умер, ";
        }
        return string;
    }

    public static String getRandomActionPhrase() {
        int number = randomBetween(0, actionPhrases.size() - 1);
        return actionPhrases.get(number);
    }

    public static String getRandomDeathPhrase() {
        int number = randomBetween(0, deathPhrases.size() - 1);
        return deathPhrases.get(number);
    }

    public static String getRandomKillWord() {
        int number = randomBetween(0, killWords.size() - 1);
        return killWords.get(number);
    }

    public static String getRandomDeathWord() {
        int number = randomBetween(0, deathWords.size() - 1);
        return deathWords.get(number);
    }

    private static List<String> actionPhrases = Arrays.asList(
            "русалочья скала все так же прекрасна, даже несмотря на то что %s возле нее %s.",
            "таверна была бы не таверной, если бы %s не %s после очередной кружки эля.",
            "тихие воды Залива Мечей все еще помнят %s, который громко ругаясь %s.",
            "%s опять %s.",
            "что не день то праздник! %s сегодня %s. А вы?",
            "каждому необходимо вымещать на ком-то свою злость. Вот и  %s %s.",
            "%s %s за то что те мешали ему убивать ворон возле мельницы.",
            "%s %s проходя очередной данж.",
            "сервер сегодня так сильно лагал потому что %s опять %s.",
            "если бы %s не %s то сегодня случился бы конец света. Восхвалим же его!",
            "как и вчера, %s %s.",
            "%s всегда отличался мягким характером, но несмотря на это %s.",
            "прогуливаясь по 6 кругу ада, %s решил что здесь недостаточно жарко, после чего %s",
            "решив отдохнуть после долгого отдыха, %s %s",
            "вспомнив о том, что пора бы уже спасти мир %s решительно %s",
            "надышавшись собственной глупостью, %s %s",
            "после открытия в себе неземных сил %s не долго думая %s",
            "решив доблестью покорить мир %s пришел на рыцарский турнир где %s",
            "вспоминая былые деньки и победы, %s со скрипом %s",
            "шагая бодрым шагом по Эйренморскому лесу, %s внезапно для себя %s",
            "ваши поединки на арене ничто по сравнению с тем как %s %s"
    );

    private static List<String> deathPhrases = Arrays.asList(
            "невезучий %s гуляя по лесу %s %s раз.",
            "%s решил пойти на арену, результате чего %s %s раз.",
            "данж - это всегда приключение. Вот и %s так решил. Если бы после этого он не %s %s раз то думал бы так до сих пор.",
            "%s за последнее время %s %s раз.",
            "%s вроде бы не новичок, но %s %s раз.",
            "%s %s %s раз. Плохой выдался день.",
            "у всех бывает плохое настроение. Но не до такой же степени! %s сегодня %s %s раз.",
            "%s %s %s раз проходя очередной данж.",
            "не нужно винить лаги в том что %s %s за последнее время %s раз!",
            "драконы прилетают редко, но %s все равно %s %s раз.",
            "как и вчера, %s %s. %s раз.",
            "поговаривают что мир Эйренмора не так уж и опасен для странников, но %s все равно %s %s раз.",
            "море волнуется раз. море волнуется два. %s %s три.",
            "сегодня %s %s. Аж %s раз.",
            "%s %s %s раз, при этом каждый раз произнося какое-то заклинание. Кажется, оно заканчивалось на \"...ять\"",
            "обрыв Тролля это очень опасное место. Не зря %s там %s %s раз."
    );

    private static List<String> killWords = Arrays.asList(
            "побил",
            "разбил лицо",
            "уничтожил",
            "разобрал на части",
            "разрубил на куски",
            "очистил мир от",
            "решил разобраться с",
            "вступил в пьяную драку с",
            "испепелил взглядом",
            "испачкал меч кровью",
            "пошел в лес и нарубил там",
            "покрошил на салат",
            "устроил кару небесную",
            "отправил в могилу",
            "победил в поединке с",
            "помог отбросить копыта",
            "расщепил на атомы",
            "вынес",
            "превратил в пепел",
            "аннигилировал",
            "отправил к праотцам",
            "замариновал",
            "снял скальп с",
            "загрыз",
            "украл душу у",
            "стер с лица земли",
            "отутюжил",
            "вскрыл череп",
            "отправил возрождаться",
            "устроил диалог с небом для",
            "прибил",
            "кинул с прогиба",
            "разрубил пополам",
            "выпотрошил"
    );

    private static List<String> deathWords = Arrays.asList(
            "помер",
            "распрощался с жизнью",
            "отправился на тот свет",
            "покинул этот мир",
            "респавнился",
            "увидел свет в конце тоннеля",
            "умер",
            "был побежден",
            "попрощался c этим миром",
            "ушел на покой",
            "отбросил копыта",
            "улетел на небо",
            "свалился в ад",
            "покинул грешную землю",
            "отставил бренный мир",
            "отправился к праотцам",
            "встретился с богами",
            "перешел не на тот свет",
            "умудрился умереть"
    );

    public static int randomBetween(int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    public static String getTimeString(World world) {
        return "Текущее время " + addZero(getWorldHours(world)) + ":" + addZero(getWorldMinutes(world));
    }

    public static String addZero(Integer integer) {
        String string = String.valueOf(integer);
        if (string.length() == 1) {
            string = "0" + string;
        }
        return string;
    }

    public static String getWeatherString(World world) {
        if (world.isDaytime() && !world.isRaining()) return "На сервере сейчас чудесный солнечный день";
        if (world.isDaytime() && world.isRaining()) return "Солнце на сервере сейчас прячется за облаками, идет дождь";
        if (!world.isDaytime() && !world.isRaining()) return "На сервере сейчас звездная ночь";
        if (!world.isDaytime() && world.isRaining()) return "Ночное небо на сервере сейчас затянуто тучами";
        return "";
    }

    public static int getWorldMinutes(World world) {
        int time = (int) Math.abs((world.getWorldTime() + 6000) % 24000);
        return (time % 1000) * 6 / 100;
    }

    public static int getWorldHours(World world) {
        int time = (int)Math.abs((world.getWorldTime()+ 6000) % 24000);
        return (int)((float)time / 1000F);
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
