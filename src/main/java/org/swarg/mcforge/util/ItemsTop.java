package org.swarg.mcforge.util;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import static net.minecraft.util.StringUtils.isNullOrEmpty;

/**
 * 18-02-21
 * @author Swarg
 */
public class ItemsTop {

    protected Map<ItemEntry, ItemEntry> itemsMap = new HashMap<ItemEntry, ItemEntry>();
    protected int processedItemsFromNBT;//кол-во прочитанных из нбт предметов
    protected int processedEmptyItemStacks;//обработано предметов с ненулевым количеством

    public boolean isEmpty() {
        return this.itemsMap.isEmpty();
    }
    
    public int size() {
        return this.itemsMap.size();
    }

    public Map<ItemEntry, ItemEntry> getItemsMap() {
        return itemsMap;
    }
    
    public void clear() {
        this.itemsMap.clear();
        this.processedItemsFromNBT = 0;
        this.processedEmptyItemStacks = 0;
    }


    //-----------------------------------------------------------------------//
    /*обёртка вокруг ItemStack для поика нужной записи в мапе, чтобы не создавать на каждый чих новый инстанс*/
    protected static final ItemEntry tmpItemEntry = ItemEntry.newMutableEntry();


    public int processInventory(IInventory inv) {
        int total = 0;
        if (inv != null) {
            final int sz = inv.getSizeInventory();
            for (int i = 0; i < sz; i++) {
                try {
                    ItemStack is = inv.getStackInSlot(i);
                    if (is != null && is.getItem() != null) {
                        tmpItemEntry.set(is);
                        if (is.stackSize > 0) {
                            total += updateItemsTop(tmpItemEntry);
                        } else {
                            this.processedEmptyItemStacks++;
                        }
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
        final int id = nbt.getShort("id");
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
                } else {
                    processedEmptyItemStacks++;
                }
            }
        }
        return 0;
    }


    public StringBuilder sortAndFormatItems(StringBuilder sb, Object filter) {
        int max = 0;
        int total = 0;
        List<ItemEntry> list = new ArrayList<ItemEntry>(this.itemsMap.size());
        for (ItemEntry entry : this.itemsMap.values()) {
            //todo apply filter
            list.add(entry);
            final int count = entry.getCount();
            total += count;
            if (count > max) {
                max = count;
            }
        }
        list.sort(ItemEntry.COMPARE_COUNT_DESCENT);

        //for unloc name
        ItemStack tmpStack = new ItemStack(Item.getItemById(1), 1, 0);//???Experimental
        //for format
        final int maxDigitChars = Math.max(3, ItemEntry.getDigitCharsCountForNumber(max) + 1);

        sb.append("Cnt #id:meta NBT UnlocalizeName\n");
        final int sz = list.size();
        for (int i = 0; i < sz; i++) {
            ItemEntry entry = list.get(i);
            /*хитрый способ без пересоздания инстансов это будет работать
            корретно для большинства, но не для всех предметов.
            Для передачи меты в метод выдающий полное unlocal-name для стака (напр.шерсть-цвет)*/
            tmpStack.setItemDamage(entry.getMeta());//count #id:meta [NBT] UnlocalName
            entry.appendTo(sb, maxDigitChars, true, tmpStack).append('\n'); //todo
        }

        sb.append("Unique Items  : ").append(sz).append('\n');
        sb.append("Total Items   : ").append(total).append('\n');
        if (this.processedItemsFromNBT > 0) {
            sb.append("Items from Nbt: ").append(processedItemsFromNBT).append('\n');
        }
        //скорее для отладки и интереса - это стаки число предметов в которорых меньше либо равно 0
        if (this.processedEmptyItemStacks > 0) {
            sb.append("EmptyItemStacks: ").append(this.processedEmptyItemStacks).append('\n');
        }
        return sb;
    }

    //============================= DEBUG-tools ================================

    public String processItemStack(ItemStack stack) {
        if (stack != null) {
            int items = 0;
            tmpItemEntry.set(stack);
            items += updateItemsTop(tmpItemEntry);
            if (stack.hasTagCompound()) {
                items += processedItemStackNBT(stack.stackTagCompound);
            }

            final int maxDigitChars = ItemEntry.getDigitCharsCountForNumber(items) + 1;
            StringBuilder sb = new StringBuilder("ItemStack: ");
            tmpItemEntry.set(stack).appendTo(sb, maxDigitChars, true, null);//count #id:meta UnlocalName
            sb.append(" Contains[").append(items).append("]:\n");
            sortAndFormatItems(sb, null);//report();
            clear();
            return sb.toString();
        }
        return "?";
    }

    //------------------------------------------------------------------------\\

    public String playerInventory(String name, NBTTagCompound playerDataNbt) {
        if (playerDataNbt == null && !isNullOrEmpty(name)) {
            playerDataNbt = XPlayer.getPlayerFullNBTData(name, false, null);//from online & offline
        }

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
            sb.append("--- Inventory of Player[").append(name).append("] ---\n");//Items[").append(items).append("]
            sortAndFormatItems(sb, null);
            clear();

            ///*DEBUG*/System.out.println(playerDataNbt);

            if (playerDataNbt.hasKey("ForgeData")) {
                NBTTagCompound customEntityData = playerDataNbt.getCompoundTag("ForgeData");
                if (customEntityData != null) {
                    sb.append("=== [CustomEntityData] ===\n");
                    Iterator iter = customEntityData.func_150296_c().iterator();
                    while (iter.hasNext()) {
                        String tagName = (String) iter.next();
                        NBTBase base = customEntityData.getTag(tagName);
                        //здесь бы неплохо было бы проверить на является ли это экстро-инвентарём
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
                                sb.append("--- [").append(tagName).append("] ---\n");//Items[").append(items).append("]
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
    }


    public String playerEnderChest(String name) {
        if (!isNullOrEmpty(name)) {
            NBTTagCompound playerDataNbt = XPlayer.getPlayerFullNBTData(name, false, null);//from online & offline
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

}
