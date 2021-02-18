package org.swarg.mcforge.tools;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.WorldServer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.DimensionManager;
import org.swarg.mcforge.util.ItemEntry;
import org.swarg.mcforge.util.ItemsTop;
import static net.minecraft.util.StringUtils.isNullOrEmpty;


/**
 * 15-02-21
 * todo Entity-Storage рамки телеги и прочее в сканируемых чанках тфк - мулы с сундуками...
 * -RegularEntityWithCapturedDrop? Насколько безопасно например хранить в поднятом дропе враждебного существа с биркой(чтобы не исчезало)
 *
 * -ключ для force загрузки указанных чанков? на время сканирования? пока что для скана берёт только чанки из памяти.
 * @author Swarg
 */
public class ItemScanWorker {
    private static ItemScanWorker INSTANCE;

    private static final int MAX_CHUNKS = 100;//??
    private int worldDIM;
    private List<ChunkCoordIntPair> chanksQueue = new ArrayList<ChunkCoordIntPair>();
    private int cursor;
    private int processedChunks;
    private int processedTiles;//InvenotryTiles

    private ItemsTop itemsMap = new ItemsTop();//
    private Map<Class, int[]> invTiles = new HashMap<Class, int[]>();
    private Map<Class, int[]> notInvTiles = new HashMap<Class, int[]>();
    private boolean collectTilesClasses = true;

    private long starttime;
    private long donetime;
    private boolean running;
    private String subscriber;
    private static int tickRate = 10;



    public static ItemScanWorker instance() {
        if (INSTANCE == null) {
            INSTANCE = new ItemScanWorker();
        }
        return INSTANCE;
    }

//    public boolean isBusy() {
//        return this.itemsMap.getItemsMap().size() > 0;
//    }
//
//    public boolean isWork() {
//        return this.starttime > 0 && this.running && this.donetime == 0;
//    }

    public String status() {
        if (this.starttime == 0 && this.donetime == 0) {
            //first time
            if (this.itemsMap.isEmpty() && starttime == 0 && cursor == 0) {
                return "[PREPARE] QueuedChunks: " + this.chanksQueue.size();
            } 
            //errornes
            else {
                return (this.running ? "[RUN]" : "[PAUSE]") +
                        " UniqueItemsTypes Recognized: " + this.itemsMap.getItemsMap().size() + " ChunksQueue: " + cursor + "/" + this.chanksQueue.size() +
                     "\nProcessed: chunks: " + this.processedChunks+" tiles: " + this.processedTiles;
            }
        }
        else {
            StringBuilder sb = new StringBuilder();
            if (this.donetime > this.starttime && this.donetime > 0) {
                sb.append("[DONE] Ready to Reporting\n");
                sb.append("Spent ") .append(donetime - starttime).append(" ms");
            } else {
                sb.append(this.running ? "[RUNNING]" : "[PAUSE]");
                final long now = System.currentTimeMillis();
                sb.append(" Work ") .append((now - starttime) / 1000L).append(" s");
            }
            final int sz = chanksQueue.size();
            final int remainedChunks = sz - cursor;
            sb.append(" World[").append( worldDIM ).append("] ");
            sb.append(" Chunks: Queued ").append(sz).append(" Remained:").append(remainedChunks);
            sb.append(" top-item-size: ").append(this.itemsMap.getItemsMap().size());
            sb.append(" processed-Tiles: ").append(processedTiles);
            sb.append(" processed-Chunks: ").append(processedChunks);
            return sb.toString();
        }
    }


    /**
     * Add chunks in specified poly
     */
    public String addChunks(int worldDim, int cx1, int cz1, int cx2, int cz2) {
        int count = 0;
        this.worldDIM = worldDim; //todo check dim to secondary poly adding
        boolean limited = false;
        if (cx1 == cx2 && cz1 == cz2) {
            ChunkCoordIntPair c = new ChunkCoordIntPair(cx1, cz1);
            if (!chanksQueue.contains(c)) {
                chanksQueue.add(c);
                count++;
            } else {
                return "Alredy contains " + c;
            }
        } else {
            count = makePoly(cx1, cz1, cx2, cz2);
            if (count > MAX_CHUNKS) {
                limited = true;
            }
        }
        return "DIM[" + worldDim + "] Added chunksCoords: " + count + " AllChunksQuequed: "+ this.chanksQueue.size() +
                (limited ? " [LIMITED]" : "");
    }

    private int makePoly(int x1, int z1, int x2, int z2) {
        int minX, maxX, minZ, maxZ;
        if (x1 < x2) { minX = x1; maxX = x2;} else { minX = x2; maxX = x1;}
        if (z1 < z2) { minZ = z1; maxZ = z2;} else { minZ = z2; maxZ = z1;}
        int count = 0;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                ChunkCoordIntPair ccip = new ChunkCoordIntPair(cx, cz);
                if (!chanksQueue.contains(ccip)) {
                    chanksQueue.add(ccip);
                    count++;
                }
                if (count > MAX_CHUNKS) {
                    return count;//"Limit "+ MAX_CHUNKS + " in cx:" + cx + " cz:" + cz;
                }
            }
        }
        return count;
    }

    private String makePoly() {
        if (this.starttime == 0 && !this.running) {
             if (this.chanksQueue.size() == 2) {
                 //make
                 ChunkCoordIntPair c1 = this.chanksQueue.get(0);
                 ChunkCoordIntPair c2 = this.chanksQueue.get(1);
                 final int added = this.makePoly(c1.chunkXPos, c1.chunkZPos, c2.chunkXPos, c2.chunkZPos);
                 return "Chunks Quequed: " + added;
             } else {
                 return "need two chunk-coordinates poits. use 'add -here' first";
             }
        } else {
            return "Not supported in runing time";
        }
    }
    
    public void subscribe(String name) {
        this.subscriber = name;
    }

    /**
     * Убрать кусок заданного пространства (координаты чанков) из ранее добавленого
     * --
     * ----
     * ----
     */
    public String removeChunks(int cx1, int cz1, int cx2, int cz2) {
        if (chanksQueue.size() == 0) {
            return "empty";
        }
        if (cursor > 0) {
            //либо сделать редактирование курсора т.к. может сбится корректность сканирования
            return "not supported at running";
        }

        int count = 0;
        if (cx1 == cx2 && cz1 == cz2) {
            ChunkCoordIntPair rc = new ChunkCoordIntPair(cx1, cz1);
            if (chanksQueue.remove(rc)) {
                count++;
            }
        } else {
            for (int cx = cx1; cx < cx2; cx++) {
                for (int cz = cz1; cz < cz2; cz++) {
                    ChunkCoordIntPair ccip = new ChunkCoordIntPair(cx,cz);
                    if (chanksQueue.remove(ccip)) {
                        count++;
                    }
                }
            }
        }
        return "Removed chunksCoords: " + count + " AllChunksQuequed: "+ chanksQueue.size();
    }


    public String start() {
        if (starttime == 0 && !running) {
            init();
        } else {
            this.running = true;
        }
        return "running: " + this.running;
    }

    private void init() {
        this.itemsMap.clear();//this.itemsMap.clear();
        this.processedChunks = 0;
        this.processedTiles = 0;
        this.starttime = 0;
        this.donetime = 0;
        this.cursor = 0;
        this.running = false;
        if (this.chanksQueue.size() > 0) {
            //MinecraftForge.EVENT_BUS.register(this);
            FMLCommonHandler.instance().bus().register(this);
            this.starttime = System.currentTimeMillis();
            this.running = true;
        }        
    }

    //aka pause
    public String stop() {
        if (this.running) {
            this.running = false;
            return "pause";
        }
        return "not running";
    }

    public String clear() {
        this.cursor = 0;
        this.running = false;
        this.starttime = 0;
        this.donetime = 0;
        this.processedTiles = 0;
        this.processedChunks = 0;
        //this.processedItemsFromNBT = 0;
        this.itemsMap.clear();
        this.chanksQueue.clear();
        //this.itemsMap.clear();
        this.invTiles.clear();
        this.notInvTiles.clear();
        this.subscriber = null;
        this.tmpItemEntry.clear();
        this.worldDIM = 0;
        //MinecraftForge.EVENT_BUS.unregister(this);
        FMLCommonHandler.instance().bus().unregister(this);
        return "Cleaned";
    }
    


    public String report() {
        if (this.starttime == 0) {
            return "Not Started";
        }
        else if (this.donetime != 0 && this.running == false) {
            if (this.itemsMap.size() == 0) {
                return " Not found any items in chunks " + this.chanksQueue.size();
                //todo координаты всех чанков?
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("---------- Chunks[").append(this.chanksQueue.size()).append("] ------------\n");
                sb.append("Spent Time: ").append(this.donetime - this.starttime).append("ms\n");
                //------------------------------------
                this.itemsMap.sortAndFormatItems(sb, /*filter*/null);
                //------------------------------------
                sb.append("Processed chunks: ").append(this.processedChunks);
                sb.append(" InvTiles: ").append(this.processedTiles).append('\n');

                //Extension Tiles Inspection
                if (this.invTiles.size() > 0) {
                    sb.append("--= [Inventory Tiles] =--\n");
                    sb.append("itemsCnt TECnt Class\n");//количество единиц предметов в данном классе TE, кол-во классов
                    for (Map.Entry<Class, int[]> entry : this.invTiles.entrySet()) {
                        Class clazz = entry.getKey();
                        int[] box = entry.getValue();
                        if (clazz != null && box != null) {                            
                            sb.append( String.format("% ,8d % ,5d  %s\n", box[1], box[0], clazz.getName()));
                            //sb.append(box[1]).append("  ").append(box[0]).append("  ").append(clazz.getName()).append('\n');
                        }
                    }
                }
                if (this.notInvTiles.size() > 0) {
                    sb.append("--= [Not Inventory Tiles] =--\n");
                    sb.append("TECnt Class\n");//количество единиц предметов в данном классе TE
                    for (Map.Entry<Class, int[]> entry : this.notInvTiles.entrySet()) {
                        Class clazz = entry.getKey();
                        int[] box = entry.getValue();
                        if (clazz != null && box != null) {
                            sb.append( String.format("% ,5d  %s\n", box[0], clazz.getName()));
                            //sb.append(box[0]).append("  ").append(clazz.getName()).append('\n');
                        }
                    }

                }
                return sb.toString();
            }
        }
        return "processing...";
    }


    // ------------------- Worker: load balancing ------------------------- \\
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (this.running && event.side == Side.SERVER && event.phase == TickEvent.Phase.END) {
            try {
                final int tick = MinecraftServer.getServer().getTickCounter();
                if (tickRate <= 0 || tick % tickRate == 0) {
                    
                    if (cursor < this.chanksQueue.size()) {
                        final ChunkCoordIntPair c = this.chanksQueue.get(cursor++);
                        processChunk(c);
                    }
                    //done
                    else {
                        FMLCommonHandler.instance().bus().unregister(this);//??
                        this.donetime = System.currentTimeMillis();
                        this.running = false;
                        //send notification
                        if (!isNullOrEmpty(subscriber)) {
                            EntityPlayer player = MinecraftServer.getServer().getConfigurationManager().func_152612_a(this.subscriber);
                            if (player != null) {
                                player.addChatMessage(new ChatComponentText("LootTop-Worker: job done"));
                            }
                        }                        
                    }
                }
            } catch (Throwable t) {
                /*DEBUG*/t.printStackTrace();
            }
        }
    }

    //-----------------------------------------------------------------------//
    /*обёртка вокруг ItemStack для поика нужной записи в мапе, чтобы не создавать на каждый чих новый инстанс*/
    private static final ItemEntry tmpItemEntry = ItemEntry.newMutableEntry();

    //TODO Entity-WithContainers(Frames) RegularEntityWithCapturedDrop?
    private void processChunk(ChunkCoordIntPair c) {
        WorldServer ws = DimensionManager.getWorld(this.worldDIM);
        if (ws != null && c != null) {
            Chunk chunk = (Chunk)ws.theChunkProviderServer.loadedChunkHashMap.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(c.chunkXPos, c.chunkZPos));
            if (chunk != null) {
                if (chunk.chunkTileEntityMap != null && chunk.chunkTileEntityMap.size() > 0) {
                    for (Object o : chunk.chunkTileEntityMap.values()) {
                        if (o instanceof TileEntity) {
                            TileEntity te = ((TileEntity)o);
                            if (!te.isInvalid()) {
                                if (te instanceof IInventory) {
                                    final int items = this.itemsMap.processInventory((IInventory)te);
                                    this.processedTiles++;
                                    if (this.collectTilesClasses) {
                                        //--ext stat---
                                        Class clazz = te.getClass();
                                        int[] counter = this.invTiles.get(clazz);
                                        if (counter == null) {
                                            counter = new int[2];
                                            counter[0] = 1;     //количество обнаруженых TE данного класса
                                            counter[1] = items; //сколько предметов всего хранится в данном классе TE
                                            this.invTiles.put(clazz, counter);
                                        } else {
                                            counter[0]++;
                                            counter[1] += items;
                                        }
                                    }
                                }
                                //extended statistics NotInvTE
                                else if (this.collectTilesClasses) {
                                    Class clazz = te.getClass();
                                    int[] counter = this.notInvTiles.get(clazz);
                                    if (counter == null) {
                                        counter = new int[1];
                                        counter[0] = 1;
                                        this.notInvTiles.put(clazz, counter);
                                    } else {
                                        counter[0]++;
                                    }
                                }
                            }
                        }
                    }
                }
                this.processedChunks++;
            }
        }
    }



    //==========================================================================

    public void cmdItemScanner(ICommandSender sender, String[] args) {
        int i = 1;
        final int sz = args == null ? 0 : args.length;
        final String cmd = i >= sz ? null : args[i++];
        String response = "?";
        if (isNullOrEmpty(cmd) || "help".equals(cmd)) {
            response = "item-scanner <status/add/remove/make-poly/chunks/start/stop/report/item-status/clear/check-item/player-inv/player-enderchest>";
        }
        else if (isCmd(cmd, "status", "st")) {
            response = status();//LootTopWorker.instance().status()
        }
        //build chanks to check
        else if (isCmd(cmd, "add", "a")) {
            response = "<add> cx1 cz1 cx2 cz2 [dimensionId] | -here";
            int dim = sender.getEntityWorld().provider.dimensionId;
            this.subscribe(sender.getCommandSenderName());
            if ("-here".equals(arg(args,i)) && i++ > 0 && sender instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer)sender;
                response = this.addChunks(dim, player.chunkCoordX, player.chunkCoordZ, player.chunkCoordX, player.chunkCoordZ);
            } else {
            ///*DEBUG*/ System.out.println("sz:"+sz+"i:"+i+" rem:"+(sz-i));
                if (sz - i >= 4) {
                    final int cx1 = argI(args, i++, Integer.MIN_VALUE);
                    final int cz1 = argI(args, i++, Integer.MIN_VALUE);
                    final int cx2 = argI(args, i++, Integer.MIN_VALUE);
                    final int cz2 = argI(args, i++, Integer.MIN_VALUE);
                    dim = argI(args, i++, sender.getEntityWorld().provider.dimensionId );

                    if (cx1 != Integer.MIN_VALUE && cz1 != Integer.MIN_VALUE &&
                        cx2 != Integer.MIN_VALUE && cz2 != Integer.MIN_VALUE) {
                        response = this.addChunks(dim, cx1, cz1, cx2, cz2);
                    }
                }
            }
        }
        else if (isCmd(cmd, "make-poly", "mp")) {
            response = this.makePoly();
        }
        else if (isCmd(cmd, "remove", "rm")) {
            response = "<remove> cx1 cz1 [cx2 cz2] | -here";
            if ("-here".equals(arg(args,i)) && i++ > 0 && sender instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer)sender;
                response = this.removeChunks(player.chunkCoordX, player.chunkCoordZ, player.chunkCoordX, player.chunkCoordZ);
            } else {
                if (sz - i >= 2) {
                    final int cx1 = argI(args, i++, Integer.MIN_VALUE);
                    final int cz1 = argI(args, i++, Integer.MIN_VALUE);
                    final int cx2 = argI(args, i++, cx1);
                    final int cz2 = argI(args, i++, cz1);
                    if (cx1 != Integer.MIN_VALUE && cz1 != Integer.MIN_VALUE) {
                        response = this.removeChunks(cx1, cz1, cx2, cz2);
                    }
                }
            }
        }
        else if (isCmd(cmd, "chunks", "coords")) {
            response = this.chanksQueue + "\n Cursor: " + this.cursor + "/"+ this.chanksQueue.size();
        }
        else if (isCmd(cmd, "start", "go")) {
            response = this.start();
        }
        else if (isCmd(cmd, "stop", "pause")) {
            response = this.stop();
        }
        //clear created report
        else if (isCmd(cmd, "clear", "clean")) {
            response = this.clear();
        }
        else if (isCmd(cmd, "report", "rp")) {
            response = this.report();
        }
        //после выполнения сканирования получить данные об конкретной интересующей вещи
        //заданной id meta hasNBT. На случай когда интересует только один конкретный предмет а собраные данные о предметах огромны
        else if (isCmd(cmd, "item-status", "is")) {
            final String sId = arg(args,i++);

            if (isNullOrEmpty(sId) || isCmd(sId,"help","h")) {
                response = "id [meta] [hasNbt]";
            } else {
                if (this.itemsMap.isEmpty()) {
                    response = "The scan was not started or the items were not detected";
                } else {
                    final int itemId = Integer.parseInt(sId);
                    final String sMeta = arg(args,i++);
                    final int meta = isNullOrEmpty(sMeta) ? 0 : Integer.parseInt(sMeta);
                    final String sNbt = arg(args, i++);
                    final boolean hasNBT = "hasnbt".equalsIgnoreCase(sNbt) || "T".equalsIgnoreCase(sNbt) || "nbt".equalsIgnoreCase(sNbt);
                    Item item = Item.getItemById(itemId);
                    if (item == null) {
                        response = "Not found Registered Item for id:" + itemId;
                    } else {
                        tmpItemEntry.set(item, meta, 0, hasNBT);
                        ItemEntry entry = this.itemsMap.getItemsMap().get(tmpItemEntry);
                        if (entry == null) {
                            response = "Not Found ItemEntry in Map["+this.itemsMap.size()+"]";
                        } else {
                            response = entry.appendTo(new StringBuilder(), 8, true, null).toString();
                        }
                    }
                }
            }
        }
        //debug-tool
        //проверить работоспособность механики распознования вещей из нбт на предмете из рук оператора
        else if (isCmd(cmd, "check-item", "ci")) {
            if (sender instanceof EntityPlayer) {
                ItemStack stack = ((EntityPlayer)sender).getHeldItem();
                if (stack != null) {
                    //todo внутри текущего инстанса а не нового по флагу
                    ItemsTop it = new ItemsTop();
                    response = it.processItemStack(stack);
                    it.clear();
                } else {
                    response = "take the item in hands";
                }
            } else {
                response = "only for op-player";
            }
        }
        //составить список предметов в инвентаре игрока
        //todo player-enderchest pe
        else if (isCmd(cmd, "player-inv", "pi")) {
            String name = arg(args, i++);
            if (isNullOrEmpty(name) || isCmd(name, "help", "h")) {
                response = "player-inv (player-name)";
            } else {
                ItemsTop it = new ItemsTop();
                response = it.playerInventory(name, null);
                it.clear();
            }
        }
        else if (isCmd(cmd, "player-enderchest", "pec")) {
            String name = arg(args, i++);
            if (isNullOrEmpty(name) || isCmd(name, "help", "h")) {
                response = "player-inv (player-name)";
            } else {
                ItemsTop it = new ItemsTop();
                response = it.playerEnderChest(name);
                it.clear();
            }
        }

        //-----output-------
        if (response != null) {
            if (response.contains("\n")) {
                String[] a = response.split("\n");
                for (int j = 0; j < a.length; j++) {
                    sender.addChatMessage(new ChatComponentText(a[j]));
                }
            } else {
                sender.addChatMessage(new ChatComponentText(response));
            }
        }
    }


    //cmd4j
    public static boolean isCmd(String in, String cmd, String cmd2) {
        return (!isNullOrEmpty(in)) && ( cmd != null && in.equalsIgnoreCase(cmd) || cmd2 != null && in.equalsIgnoreCase(cmd2) );
    }

    public static String arg(String[] args, int i) {
        return args == null || i >= args.length ? null : args[i];
    }

    public static int argI(String[] args, int i, int def) {
        int val = def;
        try {
            val = Integer.parseInt(arg(args,i));
        } catch (Exception e) {
        }
        return val;
    }

    //=========================================================================



}
