package org.swarg.mc.tools;

import java.util.Objects;
import java.util.Comparator;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * 16-02-21
 * @author Swarg
 */
public class ItemEntry {
    private Item item;
    private int meta; //HasSubtypes
    private boolean hasNbt;
    private final boolean immutable;

    private int hash;
    //??
    private int count;


    private ItemEntry() {
        this.immutable = false;
    }
    
    public static ItemEntry newMutableEntry() {
        return new ItemEntry();
    }

    //immutable
    public ItemEntry(ItemStack is) {
        this.item = is.getItem();
        Objects.requireNonNull(this.item);
        this.meta = is.getHasSubtypes() ? is.getItemDamage() : 0;
        this.hasNbt = is.hasTagCompound();
        this.immutable = true;
    }

    @Override
    public int hashCode() {
        if (this.hash == 0) {
            int h = 3;
            h = 23 * h + Objects.hashCode(this.item);
            h = 23 * h + this.meta;
            if (this.hasNbt) {
                h = 23 * h + 1;
            }
            this.hash = h;
        }
        return this.hash;
    }

    /**
     * count не учавствует в проверках на равенство!
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ItemEntry other = (ItemEntry) obj;
        return other.item == this.item && other.meta == this.meta && other.hasNbt == this.hasNbt;
    }

    //Experimental
    public ItemEntry set(ItemStack is) {
        if (!this.immutable) {
            this.hash = 0;//reset
            this.item = is.getItem();
            Objects.requireNonNull(this.item);//??
            this.meta = is.getHasSubtypes() ? is.getItemDamage() : 0;
            this.hasNbt = is.hasTagCompound();
            //
            this.count = is.stackSize;
        } else {
            /*DEBUG*/throw new IllegalStateException("immutable!");
        }
        return this;
    }

    /**
     *@param meta = item.getHasSubtypes() ? damage : 0;
     * @param hasNbt is.hasTagCompound()
     */
    public ItemEntry set(Item item, int meta/*damage*/, int stackSize, boolean hasNbt) {
        this.hash = 0;//reset;
        this.item = item;
        Objects.requireNonNull(this.item);//?
        this.meta = meta;
        this.hasNbt = hasNbt;
        //
        this.count = stackSize;
        return this;
    }


    public ItemEntry inc(int aCount) {//ItemStack is) {
        this.count += aCount;//is.stackSize;
        return this;
    }
    
    //is.stackSize
    public int getCount() {
        return count;
    }

    public int getMeta() {
        return meta;
    }

    
    public ItemEntry copy() {
        ItemEntry c = new ItemEntry();
        c.item = this.item;
        c.meta = this.meta;
        c.hasNbt = this.hasNbt;
        c.count = this.count;
        c.hash = this.hash;
        return c;
    }
    
    public void clear() {
        this.hash = 0;
        this.meta = 0;
        this.count = 0;
        this.hasNbt = false;
        this.item = null;//air?
    }

    public static int getDigitCharsCountForNumber(long n) {
        if (n < 0) n = Math.abs(n);
        return (int) Math.log10(n) + 1;
    }

    //count #id:meta [NBT] unlocName
    public StringBuilder appendTo(StringBuilder sb, int maxDigitChars, boolean aUnlockName, ItemStack tmpItemStack) {//
        if (sb != null) {
            //simple format count
            if (maxDigitChars > 0) {
                final int remChars = maxDigitChars - getDigitCharsCountForNumber(count);
                if (remChars > 0) {
                    for (int i = 0; i < remChars; i++) {
                        sb.append(' ');
                    }
                }
            }
            sb.append(this.count);

            final int id = Item.getIdFromItem(item);
            sb.append(" #");
            //simple format
            if (id < 1000) sb.append('0');
            if (id < 100) sb.append('0');
            if (id < 10) sb.append('0');
            sb.append(id) //ITEM-ID выравнние под 4 знака
            /*META*/.append(':');
            if (this.meta < 10) sb.append('0'); //выравнивание под 2 знака
            sb.append(this.meta).append(' ');

            sb.append(this.hasNbt ? 'T' : '-');//T - [HasNBT] Tag

            if (aUnlockName && this.item != null) {
                //tmpItemStack.getItem()
                //is.setItemDamage(id); meta
                //String unlocname = item.getUnlocalizedName(new ItemStack(item, 1, meta));//todo opti
                String unlocname = tmpItemStack == null
                        ? item.getUnlocalizedName() : item.getUnlocalizedName(tmpItemStack);
                sb.append("  ").append(unlocname);
            }
        }
        return sb;
    }

    @Override
    public String toString() {
        return appendTo(new StringBuilder(), 0, true, null).toString();
    }

    public static final Comparator COMPARE_COUNT_DESCENT = new Comparator<ItemEntry>() {
        @Override
        public int compare(ItemEntry e1, ItemEntry e2) {
            int i1 = (e1 == null) ? 0: e1.getCount();
            int i2 = (e2 == null) ? 0: e2.getCount();
            return Integer.compare(i2, i1);
        }
    };

}
