package org.swarg.mc.tools;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
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
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.WorldServer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.DimensionManager;
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
    private int processedItemsFromNBT;//кол-во прочитанных из нбт предметов
    //todo обработано предметов с ненулевым количеством
    private Map<ItemEntry, ItemEntry> itemsMap = new HashMap<ItemEntry, ItemEntry>();
    //optional inventory
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

    public String status() {
        if (this.starttime == 0 && this.donetime == 0) {
            //first time
            if (itemsMap.size() == 0 && starttime == 0 && cursor == 0) {
                return "[PREPARE] QueuedChunks: " + this.chanksQueue.size();
            } 
            //errornes
            else {
                return (this.running ? "[RUN]" : "[PAUSE]") +
                        " UniqueItemsTypes Recognized: " + this.itemsMap.size() + " ChunksQueue: " + cursor + "/" + this.chanksQueue.size() +
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
            sb.append(" top-item-size: ").append(itemsMap.size());
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
        if (cx1 == cx2 && cz1 == cz2) {
            chanksQueue.add(new ChunkCoordIntPair(cx1, cz1));
            count++;
        } else {
            for (int cx = cx1; cx < cx2; cx++) {
                for (int cz = cz1; cz < cz2; cz++) {
                    ChunkCoordIntPair ccip = new ChunkCoordIntPair(cx,cz);
                    chanksQueue.add(ccip);
                    count++;
                    if (count > MAX_CHUNKS) {
                        return "Limit "+ MAX_CHUNKS + " in cx:" + cx + " cz:" + cz;
                    }
                }
            }
        }
        return "DIM[" + worldDim + "] Added chunksCoords: " + count + " AllChunksQuequed: "+ this.chanksQueue.size();
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
        this.itemsMap.clear();
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
        this.processedItemsFromNBT = 0;
        this.chanksQueue.clear();
        this.itemsMap.clear();
        this.invTiles.clear();
        this.notInvTiles.clear();
        this.subscriber = null;
        this.tmpItemEntry.clear();
        this.worldDIM = 0;
        //MinecraftForge.EVENT_BUS.unregister(this);
        FMLCommonHandler.instance().bus().unregister(this);
        return "Cleaned";
    }
    
    private StringBuilder sortAndFormatItems(StringBuilder sb, Object filter) {
        int max = 0;
        List<ItemEntry> list = new ArrayList<ItemEntry>(this.itemsMap.size());
        for (ItemEntry entry : this.itemsMap.values()) {
            //todo apply filter
            list.add(entry);
            if (entry.getCount() > max) {
                max = entry.getCount();
            }
        }
        list.sort(ItemEntry.COMPARE_COUNT_DESCENT);

        //for unloc name
        ItemStack tmpStack = new ItemStack(Item.getItemById(1), 1, 0);//???Experimental
        //for format
        final int maxDigitChars = Math.max(3, ItemEntry.getDigitCharsCountForNumber(max) + 1);

        sb.append("Cnt #id:meta NBT UnlocalizeName\n");
        for (int i = 0; i < list.size(); i++) {
            ItemEntry entry = list.get(i);
            /*хитрый способ без пересоздания инстансов это будет работать
            корретно для большинства, но не для всех предметов.
            Для передачи меты в метод выдающий полное unlocal-name для стака (напр.шерсть-цвет)*/
            tmpStack.setItemDamage(entry.getMeta());//count #id:meta [NBT] UnlocalName
            entry.appendTo(sb, maxDigitChars, true, tmpStack).append('\n'); //todo
        }
        if (this.processedItemsFromNBT > 0) {
            sb.append("Items found in nbt: ").append(processedItemsFromNBT).append('\n');
        }
        return sb;
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
                sortAndFormatItems(sb, /*filter*/null);
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
                                    final int items = processInventory((IInventory)te);
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

    public int processInventory(IInventory inv) {
        int total = 0;
        if (inv != null) {
            final int sz = inv.getSizeInventory();
            for (int i = 0; i < sz; i++) {
                try {
                    ItemStack is = inv.getStackInSlot(i);
                    if (is != null && is.getItem() != null) {
                        tmpItemEntry.set(is);
                        total += updateItemsTop(tmpItemEntry);
                        if (is.hasTagCompound()) {
                            final int foundInNbt = processedItemStackNBT(is.stackTagCompound);
                            processedItemsFromNBT += foundInNbt;
                            total += foundInNbt;
                        }
                    }
                }
                catch (Throwable e) {
                    /*DEBUG*/e.printStackTrace();
                }
            }
        }
        return total;
    }

    /**
     * Ищет по обёртке данные для уникальной вещи
     * если находит - инкремирует на текущее количество предметов
     * если НЕ находит нужную запись - создаёт её в мапе на сонове копии
     * текущего tmpUItem
     * @param tmpUItem
     * @return Возвращает количество предметов в стаке
     */
    public int updateItemsTop(ItemEntry tmpUItem) {
        int count = tmpUItem.getCount();//is.stackSize;
        if (count > 0) {
            ItemEntry entry = this.itemsMap.get(tmpUItem);
            if (entry == null) {
                //если такой записи нет - её копия будет помещена в мапу
                entry = tmpUItem.copy();
                this.itemsMap.put(entry, entry);
            } else {
                entry.inc( count ); //is.stackSize
            }
        }
        return count;
    }

    /**
     * Поиск внутри nbt других предметов
     * и помещение их в this.topmap + рекурсивный поиск внутри подтагов обнаруженых предметов
     * @param nbt
     */
    public int processedItemStackNBT(NBTTagCompound nbt) {
        if (nbt != null) {            
            NBTTagList list = null;
            NBTBase base = (NBTBase) nbt.getTag("Items");
            if (base != null && base.getId() == (byte)9) {
                list = (NBTTagList)base;
            } else {
            }
            //todo если есть нбт проверить instnaceof NBTTagList? на структуру id-Count-Damage

            if (list != null) {
                int items = 0;
                final int sz = list.tagCount();
                for (int i = 0; i < sz; i++) {
                    NBTTagCompound nbt0 = list.getCompoundTagAt(i);
                    if (nbt0 != null) {
                        items += ItemStack_readFromNBT(nbt0);
                    }
                }
                return items;
            }
        }
        return 0;
    }
    
    public int ItemStack_readFromNBT(NBTTagCompound nbt) {
        Short id = nbt.getShort("id");
        if (id > 0) {
            Item item = Item.getItemById(id); 
            if (item != null) {
                int stackSize = nbt.getByte("Count");
                //проверить исчезнет ли предмет у которого количетсво - 0
                //можно ли в таком хранить подпредметы ? в теге tag??
                if (stackSize > 0) {
                    int items = 0;
                    //интересует только мета - если это не мета а урон - игнорировать(0)
                    int meta = item.getHasSubtypes() ? nbt.getShort("Damage") : 0;
                    if (meta < 0) {
                        meta = 0;
                    }
                    final boolean hasNbt = nbt.hasKey("tag", 10);
                    tmpItemEntry.set(item, meta, stackSize, hasNbt);
                    items += updateItemsTop(tmpItemEntry);
                    //can be tmpUItem.clear there

                    if (hasNbt) {
                        NBTTagCompound tag = nbt.getCompoundTag("tag");
                        if (tag != null) {
                            //recursive
                            final int foundInNbt = processedItemStackNBT(tag);
                            processedItemsFromNBT += foundInNbt;
                            items += foundInNbt;
                        }
                    }
                    return items;
                }
            }
        }
        return 0;
    }
    //============================= DEBUG-tools ================================
    private String checkItem(ItemStack stack) {
        if (stack != null) {
            if (this.starttime == 0 && !this.running && this.itemsMap.isEmpty()) {
                int items = 0;
                this.starttime = System.currentTimeMillis();
                tmpItemEntry.set(stack);
                items += updateItemsTop(tmpItemEntry);
                if (stack.hasTagCompound()) {
                    items += processedItemStackNBT(stack.stackTagCompound);
                }
                this.donetime = System.currentTimeMillis();

                final int maxDigitChars = ItemEntry.getDigitCharsCountForNumber(items) + 1;
                StringBuilder sb = new StringBuilder("ItemStack: ");
                tmpItemEntry.set(stack).appendTo(sb, maxDigitChars, true, null);//count #id:meta UnlocalName
                sb.append(" Contains[").append(items).append("]:\n");
                sortAndFormatItems(sb, null);//report();
                clear();
                return sb.toString();
            }
            else {
                return "ItemScanWorker is busy";
            }
        }
        return "?";
    }
    
    private String playerInventory(String name) {
        if (!isNullOrEmpty(name)) {
            NBTTagCompound playerDataNbt = XPlayer.getPlayerNbtData(name, false);//from online & offline
            if (playerDataNbt != null) {
                NBTTagList list = playerDataNbt.getTagList("Inventory", 10);
                final int sz = list == null ? 0 : list.tagCount();
                int items = 0;
                for (int i = 0; i < sz; i++) {
                    NBTTagCompound nbt0 = list.getCompoundTagAt(i);
                    if (nbt0 != null) {
                        items += ItemStack_readFromNBT(nbt0);
                    }
                }
                StringBuilder sb = new StringBuilder();
                sb.append("--- Inventory of Player[").append(name).append("] Items[").append(items).append("] ---\n");
                sortAndFormatItems(sb, null);
                clear();

                /*DEBUG*/System.out.println(playerDataNbt);
                
                if (playerDataNbt.hasKey("ForgeData")) {
                    NBTTagCompound customEntityData = playerDataNbt.getCompoundTag("ForgeData");
                    if (customEntityData != null) {
                        sb.append("=== [CustomEntityData] ===\n");
                        Iterator iter = customEntityData.func_150296_c().iterator();
                        while (iter.hasNext()) {
                            String tagName = (String) iter.next();
                            NBTBase base = customEntityData.getTag(tagName);
                            if (base instanceof NBTTagList) {
                                NBTTagList list0 = (NBTTagList) base;
                                final int sz0 = list0 == null ? 0 : list0.tagCount();
                                if (sz > 0) {
                                    for (int i = 0; i < sz0; i++) {
                                        NBTTagCompound nbt0 = list0.getCompoundTagAt(i);
                                        if (nbt0 != null) {
                                            items += ItemStack_readFromNBT(nbt0);
                                        }
                                    }
                                    sb.append("--- [").append(tagName).append("] Items[").append(items).append("]---\n");
                                    sortAndFormatItems(sb, null);
                                    clear();
                                }
                            }
                        }
                    }
                }

                return sb.toString();
            } else {
                return "Not Found " + name;
            }
        } else
            return "no name";
    }
    
    private String playerEnderChest(String name) {
        if (!isNullOrEmpty(name)) {
            NBTTagCompound playerDataNbt = XPlayer.getPlayerNbtData(name, false);//from online & offline
            if (playerDataNbt != null) {
                if (playerDataNbt.hasKey("EnderItems", 9)) {
                    NBTTagList list = playerDataNbt.getTagList("EnderItems", 10);
                    final int sz = list == null ? 0 : list.tagCount();
                    int items = 0;
                    for (int i = 0; i < sz; i++) {
                        NBTTagCompound nbt0 = list.getCompoundTagAt(i);
                        if (nbt0 != null) {
                            items += ItemStack_readFromNBT(nbt0);
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("--- EnderChest of Player[").append(name).append("] Items[").append(items).append("] ---\n");
                    sortAndFormatItems(sb, null);
                    clear();
                    return sb.toString();
                } else {
                    return "Not Found EnderItem for " + name;
                }
            } else {
                return "Not Found for" + name;
            }
        } else
            return "no name";
    }




    //==========================================================================

    public void cmdItemScanner(ICommandSender sender, String[] args) {
        int i = 1;
        final int sz = args == null ? 0 : args.length;
        final String cmd = i >= sz ? null : args[i++];
        String response = "?";
        if (isNullOrEmpty(cmd) || "help".equals(cmd)) {
            response = "item-scanner <status/add/remove/chunks/start/stop/report/item-status/clear/check-item/player-inv/player-enderchest>";
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
                        ItemEntry entry = this.itemsMap.get(tmpItemEntry);
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
                response = (stack == null) ? "take the item in hands" : this.checkItem(stack);
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
                response = this.playerInventory(name);
            }
        }
        else if (isCmd(cmd, "player-enderchest", "pec")) {
            String name = arg(args, i++);
            if (isNullOrEmpty(name) || isCmd(name, "help", "h")) {
                response = "player-inv (player-name)";
            } else {
                response = this.playerEnderChest(name);
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
