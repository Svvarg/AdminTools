package org.swarg.mcforge.tools;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.Iterator;
import com.mojang.authlib.GameProfile;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTBase;
import org.swarg.mcforge.util.XPlayer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.swarg.mcforge.util.ItemsTop;
import static net.minecraft.util.StringUtils.isNullOrEmpty;


/**
 * 18-02-21
 * Открытие инвентаря оффлайн игрока путём копирования его содержимого в инвентарь оператора
 * С поддержкой расширенных инвентарей сохраняемых в CustomEntityData (например слот бочки в тфк)
 * [WARNING]
 * Обнаружен баг расширенного тфк-слота спины-щита-колчана во время gamemode-1
 * Если в gm-режиме изменить содержимое расширенного тфк слота (работающий через CustomEntityData)
 * то после выхода из игры его содержимое не изменяется! 
 * только в gm-0 корректно сохраняется и изменяется содержимое слота!
 * Т.е. если оператор находять в гм режиме открыв инвентарь другого игрока и 
 * изменит содержимое слота на спине то его содержимое не сохранит изменения!
 * Поэтому важно для редактирования расширеных слотов после операции сохранения
 * изменений инвентаря другого игрока проверить повторным открытием и быть в гм-0
 *
 * @author Swarg
 */
public class InventoryCommands extends CommandBase {
    private static final Logger logger = LogManager.getLogger();
    /*инвентарь указанного другого оффлайн-игрока может быть открыт только игроком-оператором с нустым инвентарём.
    Для сохранения изменений открытого инвентаря нужно указать команду inv save.
    Здесь хранятся данные о том чей инвентарь открыт конкретным оператором.
    На случай если несколько операторов одновременно пользуюся командой.
    Для предотвращения ошибочных сохранений не тому игроку. Все сохранения инвентарей логируются*/
    private Map<String, GameProfile> lastOpenedInventories = new HashMap<String, GameProfile>();

    @Override
    public String getCommandName() {
        return "inv";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "inv <open/close/save/clear-all-cache>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final String cmd = arg(args, 0);
        String response = "UKNOWN";

        if (isNullOrEmpty(cmd) || "help".endsWith(cmd)) {
            response =  getCommandUsage(sender);
        }
        //открыть инвентарь оффлайн игрока
        else if (isCmd(cmd, "open", "o")) {//get|load
            response = cmdInventoryOpen(sender, args);
        }
        //"закрыть" инвентарь без сохранения изменний
        else if (isCmd(cmd, "close", "c")) {
            response = cmdInventoryClose(sender);
        }
        //сохранить инвентарь оффлайн игрока перенесенный в инвентарь оператора обратно его владельцу
        else if (isCmd(cmd, "save", "s")) {
            response = cmdInventorySaveToOwner(sender, args);//save
        }
        //очистить мапу хранящую "сессии" операторов открывших инвентари других игроков
        else if (isCmd(cmd, "clear-all-cache", "cac")) {
            this.lastOpenedInventories.clear();
            response = "done";
        }
        toSender(sender, response);
    }

    /**
     * base - это NBTTagList в который были сохранены стаки через стандартное
     * ItemStack.writeToNBT()
     * @param base
     * @return
     */
    public static boolean isItemStackNBTList(NBTBase base) {
        if (base != null) {
            if (base.getId() == (byte)9 && base instanceof NBTTagList) {//taglist
                NBTTagList list = (NBTTagList)base;
                final int sz = list.tagCount();
                if (sz > 0) {
                    NBTTagCompound e = list.getCompoundTagAt(0);
                    if (e != null && /*e.hasKey("Slot", 1) &&*/ e.hasKey("id",2) && e.hasKey("Count",1) && e.hasKey("Damage",2) ) {
                        return true;
                    }
                }
            }
        }               
        return false;
    }


    /**
     * Offline Only!
     * Получить стандартный и расширенный инвентарь заданного offline-игрока
     * поместив его копию в до того пустой инвентарь оператора
     * либо если команда из консоли - вывести текстовой вид содержимого инвентаря
     * Для сохранения изменений используй inventory-save
     * @param sender
     * @param args
     */
    public String cmdInventoryOpen(ICommandSender sender, String[] args) {
        int i = 1;
        String name = arg(args, i++);
        String response = null;
        if (isNullOrEmpty(name) || isCmd(name, "help", "h")) {
            response = "(playername) [-text]";
            //-text - флаг для player-op для возможность получить текстовое содержимое инвентаря другого игрока
            //по умолчанию копирует всё содержимое инвентаря указанного игрока в инвентарь оператора(только если он у него пустой(Защита))
        }
        else {
            
            EntityPlayer onlinePlayer = MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);
            if (onlinePlayer != null) {
                response = "Player " + name + " Online!";
            } else {
                UUID[] box = new UUID[1];
                NBTTagCompound data = XPlayer.getPlayerFullNBTData(name, true, box);//offline
                //EntityPlayerMP other = XPlayer.getEntityPlayerMPFromDisk(otherName);//offline
                if (data == null) {
                    response = "Not found player " + name;
                }
                else if (box[0] == null) {
                    response = "UUID not found for " + name;
                }
                else {
                    final boolean rawText = "-text".equals(arg(args, i)) && i++ > 0;
                    //GUI for op-player
                    if (!rawText && sender instanceof EntityPlayerMP) {
                        //gui
                        EntityPlayerMP op = (EntityPlayerMP)sender;
                        if (XPlayer.isOpWithEmptyInventory(op)) {
                            //this.lastOpenedInventories.remove(sender.getCommandSenderName());//todo уже открыт ??

                            //ExtraInventory befor Standart!! иначе может не обновиться расширенный иневентарь!
                            int foundExtraInventories = 0;
                            NBTTagCompound customEntityData = data.getCompoundTag("ForgeData");
                            if (customEntityData != null) {
                                //scan for ExtraInventories
                                Iterator iter = customEntityData.func_150296_c().iterator();
                                while (iter.hasNext()) {
                                    Object key = iter.next();
                                    if (key instanceof String) {
                                        String tagname = (String)key;
                                        NBTBase base = customEntityData.getTag(tagname);
                                        if (isItemStackNBTList(base)) {
                                            op.getEntityData().setTag(tagname, base);
                                            foundExtraInventories++;
                                        }
                                    }
                                }
                            }
                            //Standart
                            NBTTagList nbtInventory = data.getTagList("Inventory", 10);
                            op.inventory.readFromNBT(nbtInventory);

                            
                            GameProfile gp = new GameProfile(box[0], name);
                            this.lastOpenedInventories.put(sender.getCommandSenderName(), gp);//todo gameprof
                            //todo Extension Inventory
                            response = "This is inventory of the player: " + name +" found ExtraInventories: "+ foundExtraInventories;
                        }
                        //or not op or not empty inventory
                        else {
                            response = "only for op with empty inventory";
                        }
                    }
                    //cmd from console RawText instead GUI   [-text]
                    else {//atools item-scanner player-inv (name)
                        //text list Standart+ExtraFrom CustomEntityData
                        ItemsTop it = new ItemsTop();
                        response = it.playerInventory(name, data);
                        it.clear();
                        
                        //InventoryPlayer invp = new InventoryPlayer(null);//??
                        //NBTTagList nbtInventory = data.getTagList("Inventory", 10);
                        //invp.readFromNBT(nbtInventory);
                        //final int szmi = invp.mainInventory == null ? 0 : invp.mainInventory.length;
                        //final int szai = invp.armorInventory == null ? 0 : invp.armorInventory.length;
                        //StringBuilder sb = new StringBuilder();
                        //sb.append("---------[").append(name).append("]---------\n");
                        //sb.append(" = MAIN INVENTORY [").append(szmi).append("] =\n");
                        //appendAItemStack(sb, invp.mainInventory);
                        //sb.append(" = ARMOR INVENTORY [").append(szai).append("] =\n");
                        //appendAItemStack(sb, invp.armorInventory);
                        ////Container не сохраняется на диск только инвентари + customEntityData
                        ////расширеные инвентари модов могут сохранятся в Entity.customEntityData (например так слот бочки в тфк)
                        //sb.append("To inspect extended inventory use cmd 'atools player-custom-data'");
                        //response = sb.toString();
                    }
                }
            }
        }
        return response;
    }

    private String cmdInventoryClose(ICommandSender sender) {
        String response;
        if (sender instanceof EntityPlayerMP) {
            if (lastOpenedInventories.containsKey(sender.getCommandSenderName())){
                EntityPlayerMP op = (EntityPlayerMP)sender;
                //ExtraInv
                NBTTagCompound customEntityData = op.getEntityData();//.getCompoundTag("ForgeData");
                if (customEntityData != null) {
                    //scan for ExtraInventories
                    Iterator iter = customEntityData.func_150296_c().iterator();
                    while (iter.hasNext()) {
                        Object key = iter.next();
                        if (key instanceof String) {
                            String tagname = (String)key;
                            NBTBase base = customEntityData.getTag(tagname);
                            if (isItemStackNBTList(base)) {
                                op.getEntityData().setTag(tagname, new NBTTagCompound());//???? проверить корректно ли это очистить расширенный слот
                            }
                        }
                    }
                }
                //Standart
                op.inventory.clearInventory(null, -1);
                op.inventoryContainer.detectAndSendChanges();
                op.updateHeldItem();

                GameProfile gp = this.lastOpenedInventories.remove(sender.getCommandSenderName());
                response = "closed " + (gp == null ? "?"  : gp.getName());
            } else {
                response = "nothing to close";
            }
        } else {
            response = "for op oply!";
        }
        return response;
    }

    /**
     * Сохранение ранее открутого инвентаря и помещенного в оператора инвентаря
     * и CustomEntityData его offline-игроку-владельцу
     * защитой от случайного изменения CustomEntityData у игрока на данные
     * от оперетора. Сохранять инвентарь можно только в "открытого" ранее игрока
     * это проверяется через мапу lastOpenedInventories
     * @param sender
     * @param args
     */
    private String cmdInventorySaveToOwner(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) {
            return "only for op-player";
        }
        int i = 1;
        GameProfile gp = this.lastOpenedInventories.get(sender.getCommandSenderName());
        if (gp == null) {
            return "Open inventory first";
        }
        if (isNullOrEmpty(gp.getName()) || gp.getId() == null) {
            return "Errornes gameprofile!";
        }
        String invOwnerName = gp.getName();//arg(args, i++);//имя offline-игрока которому будет сохранён инвентарь оператора
        String response = null;
        EntityPlayer onlinePlayer = MinecraftServer.getServer().getConfigurationManager().func_152612_a(invOwnerName);
        if (onlinePlayer != null) {
            response = "Can not save inventory - the Player " + invOwnerName + " is online!"; //use invsee
        }
        else {
            //EntityPlayerMP other = XPlayer.getEntityPlayerMPFromDisk(invOwnerName);//offline
            NBTTagCompound data = XPlayer.getPlayerFullNBTData(gp.getId().toString(), true, null);//offline //важно тут вместо имени передаётся uuid для облегчения поиска
            if (data == null) {
                response = "Not found player " + invOwnerName;
            }
            else {
                EntityPlayerMP op = (EntityPlayerMP)sender;
                int foundCEDExtraInv = 0;
                //Standart здесь порядок важен обратный от открытия сначала пишется стандартный инвентарь в нбт это обновит и данные игрока в EntityData
                //Например если в моде идёт наследование от InventoryPlayer то метод будет переопределён и сделает свои сохранения
                data.setTag("Inventory", op.inventory.writeToNBT(new NBTTagList()));

                //ExtraInventory=
                NBTTagCompound opCustomEntityData = op.getEntityData();
                NBTTagCompound ced = data.getCompoundTag("ForgeData");
                if (opCustomEntityData != null && ced != null) {
                    //scan for ExtraInventories
                    Iterator iter = opCustomEntityData.func_150296_c().iterator();
                    while (iter.hasNext()) {
                        Object key = iter.next();
                        if (key instanceof String) {
                            String tagname = (String)key;
                            NBTBase base = opCustomEntityData.getTag(tagname);
                            if (isItemStackNBTList(base)) {
                                /*DEBUG*/System.out.println("OP CED." + tagname + ":\n" + base);
                                ced.setTag(tagname, base);
                                foundCEDExtraInv++;
                            }
                        }
                    }
                }

                //todo ExtraInventory
                boolean saved = XPlayer.writePlayerData(gp.getId(), data);
                //boolean copied = XPlayer.copyInvenotryAndCustomData(op, other, copyCed);
                //boolean saved = copied && XPlayer.savePlayerDataToDisk(other);
                response = "Op: [" + sender.getCommandSenderName() +
                        "] change&save inventory of " + gp.getName() +
                        /*DEBUG UUID*/": " + gp.getId() +
                        " saved:" + (saved?'+':'-') +
                        (foundCEDExtraInv > 0 ? " [CEDExtraInv:"+foundCEDExtraInv+"]" : " [StandartInvOnly]");
                //copyCed - флаг того, что от оператора были перенесены ced-данные к игроку
                //todo в тфк расширенный инвентарь пишется в NBTListTag если так же и в других модах,
                //то можно поставить перенос только NBTListTag полей ced-данных
                logger.info(response);
                /*защита от случаев когда был сохранен инвентарь его владельцу далее оператор
                делает clear своего инвентаря и по ошибке опять сохраняет "свой" пустой инвентарь владельцу
                можно сохранить инвентарь только один раз, после чего инвентарь оператора очищается*/
                //clear op inventory
                op.inventory.clearInventory(null, -1);
                op.inventoryContainer.detectAndSendChanges();
                op.updateHeldItem();
                this.lastOpenedInventories.remove(sender.getCommandSenderName());
            }
        }
        return response;
    }


    //-----------------------------utils----------------------------------------
    public static StringBuilder appendAItemStack(StringBuilder sb, ItemStack[] ais) {
        if (sb != null && ais != null) {
            final int sz = ais.length;
            if (sz > 0) {
                for (int j = 0; j < sz; j++) {
                    appendItemStackShortInfo(sb, j, ais[j]);
                }
            } else {
                sb.append("Empty\n");
            }
        }
        return sb;
    }

    public static StringBuilder appendItemStackShortInfo(StringBuilder sb, int slot, ItemStack is) {
        if (is != null && is.getItem() != null) {
            final Item item = is.getItem();
            final int itemId = Item.getIdFromItem(item);
            sb.append("Slot: ").append(slot).append(" id #").append(itemId);//.append(":").append(is.getItemDamage());
            final int damage = is.getItemDamage();
            if (is.getHasSubtypes() || damage != 0) {
                sb.append(":").append(damage);
            }
            if (is.stackSize != 1) {
                sb.append(" x").append(is.stackSize);
            }
            if (is.hasTagCompound()) {
                sb.append(" [NBT]");
            }
            sb.append('\n');
        }
        return sb;
    }



    //----------cmd4j-----------------------------------------------------------
    public static boolean isCmd(String in, String cmd, String cmd2) {
        return (!isNullOrEmpty(in)) && ( cmd != null && in.equalsIgnoreCase(cmd) || cmd2 != null && in.equalsIgnoreCase(cmd2) );
    }

    public static String arg(String[] args, int i) {
        return args == null || i >= args.length ? null : args[i];
    }
    //--------------------------------------------------------------------------

    //mclib
    public static void toSender(ICommandSender sender, String response) {
        if (response != null && sender != null) {
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

}
