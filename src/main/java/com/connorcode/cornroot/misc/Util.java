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
}
