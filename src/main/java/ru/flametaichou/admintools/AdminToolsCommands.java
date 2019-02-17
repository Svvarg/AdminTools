package ru.flametaichou.admintools;

import java.util.*;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StringUtils;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;

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
        return "/atools <mobclear/chestclear>";
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
                sender.addChatMessage(new ChatComponentText("/admin <mobclear (mob, range) / chestclear (range)>"));
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
                                count++;
                            }
                        }
                        sender.addChatMessage(new ChatComponentTranslation("mobclear.done", count));
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
                        sender.addChatMessage(new ChatComponentTranslation("chunkregen.done"));
                    } catch (Exception e) {
                        sender.addChatMessage(new ChatComponentTranslation("chunkregen.error"));
                        e.printStackTrace();
                    }
                }
                return;
            }

            if (argString[0].equals("story")) {

                WorldServer worldServer = (WorldServer) world;

                sender.addChatMessage(new ChatComponentText(String.format(
                                "```" +
                                        getWeatherString(worldServer) + ". " +
                                        getTimeString(worldServer) + ". " +
                                        "Вот что случилось за последнее время: " +
                                        getEventString() +
                                        "при этом из травмпункта сообщают что " +
                                        getDeathString() +
                                        "и никто не знает, что случится в следующую минуту. " +
                                        "Спасибо что обратились в наш информационный центр, свои предложения вы можете оставить по адресу https://ordinary-minecraft.ru/ " +
                                        "а вот жалобы лучше не оставляйте нигде. Хорошего вам дня!" +
                                "```"
                )));
                return;
            }
        }
    }

    public static String getEventString() {
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
        String string = "";
        for (Map.Entry entry : AdminTools.handler.deathMap.entrySet()) {
            string = string + entry.getKey() + " " + getRandomDeathWord() + " " + entry.getValue() + " раз, ";
        }
        if (StringUtils.isNullOrEmpty(string)) {
            string = "никто не умер, ";
        }
        return string;
    }

    public static String getRandomKillWord() {
        int number = random.nextInt(11);
        switch (number) {
            case 0:
                return "побил";
            case 1:
                return "разбил лицо";
            case 2:
                return "уничтожил";
            case 3:
                return "разобрал на части";
            case 4:
                return "разрубил на куски";
            case 5:
                return "очистил мир от";
            case 6:
                return "решил разобраться с";
            case 7:
                return "вступил в пьяную драку с";
            case 8:
                return "испепелил взглядом";
            case 9:
                return "испачкал меч кровью";
            case 10:
                return "пошел в лес и нарубил там";
            case 11:
                return "вынес";
            default:
                return "убил";
        }
    }

    public static String getRandomDeathWord() {
        int number = random.nextInt(3);
        switch (number) {
            case 0:
                return "помер";
            case 1:
                return "распрощался с жизнью";
            case 2:
                return "отправился на тот свет";
            case 3:
                return "покинул этот мир";
            default:
                return "умер";
        }
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
