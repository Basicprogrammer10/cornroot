package com.connorcode.cornroot.misc;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Util {
    public static ItemStack cleanItemStack(Material material, int count, ItemMetaEditor itemMetaEditor) {
        ItemStack item = new ItemStack(material, count);
        ItemMeta itemMeta = item.getItemMeta();
        itemMetaEditor.run(itemMeta);

        item.setItemMeta(itemMeta);
        return item;
    }

    public static String songLength(float secs) {
        int min = (int) (secs / 60);
        int sec = (int) (secs - (min * 60));
        return String.format("%d:%02d", min, sec);
    }
}
