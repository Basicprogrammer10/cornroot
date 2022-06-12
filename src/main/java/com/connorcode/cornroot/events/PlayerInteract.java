package com.connorcode.cornroot.events;

import com.connorcode.cornroot.Cornroot;
import com.connorcode.cornroot.Song;
import com.connorcode.cornroot.misc.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.bukkit.Bukkit.getServer;

public class PlayerInteract implements Listener {
    HashMap<UUID, Inventory> inventory = new HashMap<>();
    Material[] musicDisks = new Material[]{Material.MUSIC_DISC_13, Material.MUSIC_DISC_CAT, Material.MUSIC_DISC_BLOCKS, Material.MUSIC_DISC_CHIRP, Material.MUSIC_DISC_FAR, Material.MUSIC_DISC_MALL, Material.MUSIC_DISC_MELLOHI, Material.MUSIC_DISC_STAL, Material.MUSIC_DISC_STRAD, Material.MUSIC_DISC_WARD, Material.MUSIC_DISC_11, Material.MUSIC_DISC_WAIT,};
    NamespacedKey next = new NamespacedKey(Cornroot.getPlugin(Cornroot.class), "next");

    @EventHandler
    void PlayerJukeboxInteractEvent(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && !e.getPlayer()
                .isSneaking() || !Cornroot.jukeboxes.contains(Objects.requireNonNull(e.getClickedBlock())
                .getLocation())) return;
        e.setCancelled(true);


        Inventory inv = getServer().createInventory(null, 18, Component.text("Jukebox"));
        updateInventory(inv, 0);
        inventory.put(e.getPlayer()
                .getUniqueId(), inv);
        e.getPlayer()
                .openInventory(inv);
    }

    @EventHandler
    void PlayerInventoryInteractEvent(InventoryClickEvent e) {
        UUID uuid = e.getWhoClicked()
                .getUniqueId();
        if (e.getAction() == InventoryAction.NOTHING || !inventory.containsKey(
                uuid) || e.getInventory() != inventory.get(uuid)) return;
        e.setCancelled(true);

        Inventory inv = inventory.get(uuid);
        try {
            if (e.getSlot() == 14 || e.getSlot() == 12) {
                Integer storageContent = Objects.requireNonNull(
                        inv.getStorageContents()[e.getSlot()].getItemMeta()
                                .getPersistentDataContainer()
                                .get(next, PersistentDataType.INTEGER));
                updateInventory(inv, storageContent);
                return;
            }
        } catch (NullPointerException | IndexOutOfBoundsException ignored) {
            return;
        }

        // Get music id (if any)
        try {
            int id = Integer.parseInt(((TextComponent) Objects.requireNonNull(Objects.requireNonNull(e.getCurrentItem())
                            .lore())
                    .get(1)).content()
                    .split(":")[1].trim());

            // Increment stats
            try {
                Cornroot.database.connection.prepareStatement("UPDATE storage SET totalPlays = totalPlays+1")
                        .executeUpdate();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            // TMP - Play chirp on client
            if (id != 1) return;
            ((Player) e.getWhoClicked()).playSound(e.getWhoClicked()
                    .getLocation(), Sound.MUSIC_DISC_CHIRP.key()
                    .asString(), 1f, 1f);
            e.getWhoClicked()
                    .sendActionBar(Component.text(String.format("Now Playing: %s", Cornroot.songs.get(id).name)));
            return;
        } catch (NumberFormatException | NullPointerException ignored) {
            return;
        }
    }

    @EventHandler
    void PlayerInventoryCloseEvent(InventoryCloseEvent e) {
        if (!inventory.containsKey(e.getPlayer().getUniqueId())) return;
        inventory.remove(e.getPlayer().getUniqueId());
    }

    void updateInventory(Inventory inv, int page) {
        // Gen info
        int totalPlays = 0;
        int globalPlays = 0;
        try {
            ResultSet res = Cornroot.database.connection.prepareStatement(
                            "SELECT totalPlays, globalPlays FROM storage LIMIT 1")
                    .executeQuery();
            totalPlays = res.getInt(1);
            globalPlays = res.getInt(2);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        String[][] info = new String[][]{{"Creator", "Sigma#8214"}, {"Version", "0.0.1"}, {"Jukeboxes", String.valueOf(
                Cornroot.jukeboxes.size())}, {"Total Plays", String.valueOf(
                totalPlays)}, {"Global Plays", String.valueOf(globalPlays)}};

        // Add disks for page
        for (int i = 0; i < 9; i++) {
            int realIndex = page * 9 + i;

            if (realIndex >= Cornroot.songs.size()) {
                inv.setItem(i, new ItemStack(Material.AIR));
                continue;
            }
            Song song = Cornroot.songs.get(realIndex);

            inv.setItem(i, Util.cleanItemStack(musicDisks[new Random(i).nextInt(musicDisks.length)], 1, m -> {
                ArrayList<Component> components = new ArrayList<>();
                components.add(
                        Component.text(String.format("Artist: %s", song.author), TextColor.color(255, 255, 255)));
                components.add(Component.text(String.format("ID: %d", realIndex), TextColor.color(255, 255, 255)));
                m.lore(components);
                m.displayName(Component.text(song.name));
            }));
        }

        // Add Info
        inv.setItem(17, Util.cleanItemStack(Material.HEART_OF_THE_SEA, 1, m -> {
            m.displayName(Component.text("Info"));

            ArrayList<Component> components = new ArrayList<>();
            for (String[] i : info)
                components.add(Component.text(String.format("%s: %s", i[0], i[1]), TextColor.color(255, 255, 255)));
            m.lore(components);
        }));

        // Add page switchers
        inv.setItem(12, Util.cleanItemStack(Material.EMERALD, 1, m -> {
            m.displayName(Component.text("Previous Page"));
            if (page <= 0) return;
            m.getPersistentDataContainer()
                    .set(next, PersistentDataType.INTEGER, page - 1);
            m.addEnchant(Enchantment.DURABILITY, 1, false);
            m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }));

        inv.setItem(14, Util.cleanItemStack(Material.EMERALD, 1, m -> {
            m.displayName(Component.text("Next Page"));
            if (Cornroot.songs.size() < (page + 1) * 9) return;
            m.getPersistentDataContainer()
                    .set(next, PersistentDataType.INTEGER, page + 1);
            m.addEnchant(Enchantment.DURABILITY, 1, false);
            m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }));
    }
}
