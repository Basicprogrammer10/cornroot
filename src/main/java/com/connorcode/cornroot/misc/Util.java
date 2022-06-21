package com.connorcode.cornroot.misc;

import com.connorcode.cornroot.Cornroot;
import com.connorcode.cornroot.events.PlayerInteract;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

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

    public static void refreshMuteCache() {
        try {
            HashMap<UUID, Boolean> muteCache = new HashMap<>();
            PreparedStatement stmt = Cornroot.database.connection.prepareStatement("SELECT uuid, mute FROM users");
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) muteCache.put(UUID.fromString(resultSet.getString(1)), resultSet.getInt(2) == 1);
            PlayerInteract.muteCache = muteCache;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveMuteCache() {
        try {
            Cornroot.database.connection.setAutoCommit(false);
            for (UUID i : PlayerInteract.muteCache.keySet()) {
                int mute = 0;
                if (PlayerInteract.muteCache.get(i)) mute = 1;

                PreparedStatement stmt = Cornroot.database.connection.prepareStatement(
                        "INSERT OR IGNORE INTO users (uuid, keys, mute, date) VALUES (?1, 0, ?2, strftime('%s','now'))");
                PreparedStatement stmt2 = Cornroot.database.connection.prepareStatement(
                        "UPDATE users SET mute = ?2 WHERE uuid = ?1");

                stmt.setString(1, String.valueOf(i));
                stmt.setInt(2, mute);
                stmt2.setString(1, String.valueOf(i));
                stmt2.setInt(2, mute);

                stmt.executeUpdate();
                stmt2.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            Cornroot.database.connection.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean bypassKeyCheck(Player p) {
        return p.isOp() || p.getUniqueId() == UUID.fromString("3c358264-b456-4bde-ab1e-fe1023db6679");
    }
}
