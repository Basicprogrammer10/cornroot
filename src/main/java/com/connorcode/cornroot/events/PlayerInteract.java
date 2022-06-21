package com.connorcode.cornroot.events;

import com.connorcode.cornroot.Config;
import com.connorcode.cornroot.Cornroot;
import com.connorcode.cornroot.Song;
import com.connorcode.cornroot.misc.ItemMetaEditor;
import com.connorcode.cornroot.misc.QueueItem;
import com.connorcode.cornroot.misc.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.bukkit.Bukkit.getServer;

public class PlayerInteract implements Listener {
    public static HashMap<UUID, JukeboxInventory> inventory = new HashMap<>();
    public static HashMap<UUID, Boolean> muteCache = new HashMap<>();
    static Material[] musicDisks = new Material[]{Material.MUSIC_DISC_13, Material.MUSIC_DISC_CAT, Material.MUSIC_DISC_BLOCKS, Material.MUSIC_DISC_CHIRP, Material.MUSIC_DISC_FAR, Material.MUSIC_DISC_MALL, Material.MUSIC_DISC_MELLOHI, Material.MUSIC_DISC_STAL, Material.MUSIC_DISC_STRAD, Material.MUSIC_DISC_WARD, Material.MUSIC_DISC_11, Material.MUSIC_DISC_WAIT,};
    static NamespacedKey nextKey = new NamespacedKey(Cornroot.getPlugin(Cornroot.class), "next");
    static NamespacedKey idKey = new NamespacedKey(Cornroot.getPlugin(Cornroot.class), "id");

    @EventHandler
    void PlayerJukeboxInteractEvent(PlayerInteractEvent e) {
        if ((e.getAction() != Action.RIGHT_CLICK_BLOCK && !e.getPlayer()
                .isSneaking()) || !Cornroot.jukeboxes.contains(Objects.requireNonNull(e.getClickedBlock())
                .getLocation())) return;
        e.setCancelled(true);


        Inventory inv = getServer().createInventory(null, 18, Component.text("Jukebox"));
        JukeboxInventory jukeboxInventory = new JukeboxInventory(inv, e.getPlayer());
        jukeboxInventory.updateInventory(0);
        inventory.put(e.getPlayer()
                .getUniqueId(), jukeboxInventory);
        e.getPlayer()
                .openInventory(inv);
    }

    @EventHandler
    void PlayerInventoryInteractEvent(InventoryClickEvent e) {
        UUID uuid = e.getWhoClicked()
                .getUniqueId();
        if (e.getAction() == InventoryAction.NOTHING || !inventory.containsKey(
                uuid) || e.getInventory() != inventory.get(uuid).inv) return;
        e.setCancelled(true);

        JukeboxInventory inv = inventory.get(uuid);

        // Process page change
        try {
            if (inv.pageType == PageType.Home && (e.getSlot() == 14 || e.getSlot() == 12)) {
                Integer storageContent = Objects.requireNonNull(inv.inv.getStorageContents()[e.getSlot()].getItemMeta()
                        .getPersistentDataContainer()
                        .get(nextKey, PersistentDataType.INTEGER));
                inv.page = storageContent;
                inv.updateInventory(storageContent);
                return;
            }
        } catch (NullPointerException | IndexOutOfBoundsException ignored) {
            return;
        }

        // Process Toast exit
        if (inv.pageType == PageType.Toast && e.getSlot() == 17) {
            inv.updateInventory(inv.page);
            return;
        }

        // Process mute
        if (inv.pageType == PageType.Home && e.getSlot() == 10) {
            inv.mute ^= true;
            muteCache.put(inv.player.getUniqueId(), inv.mute);
            inv.updateInventory(inv.page);
        }

        // Get music id (if any)
        try {
            int id = Objects.requireNonNull(inv.inv.getStorageContents()[e.getSlot()].getItemMeta()
                    .getPersistentDataContainer()
                    .get(idKey, PersistentDataType.INTEGER));

            // Check keys for global play
            if (!Util.bypassKeyCheck((Player) e.getWhoClicked())) {
                PreparedStatement stmt = Cornroot.database.connection.prepareStatement(
                        "SELECT Count(*) FROM users WHERE uuid = ?1 AND keys >= 1");
                stmt.setString(1, String.valueOf(e.getWhoClicked()
                        .getUniqueId()));
                ResultSet res = stmt.executeQuery();
                if (!res.next() || res.getInt(1) < 1) {
                    inv.showInventoryToast("You dont have any global music keys", m -> m.lore(Collections.singletonList(
                            Component.join(Component.text(" "), Component.text("Purchase a global music key at"),
                                    Component.text(Config.purchaseLink,
                                            Style.style(TextColor.color(0, 170, 170), TextDecoration.UNDERLINED))))));
                    return;
                }

                // Remove a global music key
                PreparedStatement stmt2 = Cornroot.database.connection.prepareStatement(
                        "UPDATE users SET keys = keys - 1 WHERE uuid = ?1");
                stmt2.setString(1, String.valueOf(e.getWhoClicked()
                        .getUniqueId()));
                stmt2.executeUpdate();
                inv.updateInventory(inv.page);
            }

            if (Cornroot.nowPlaying != null) {
                Cornroot.queue.add(new QueueItem(id, (Player) e.getWhoClicked()));
                e.getWhoClicked()
                        .sendActionBar(
                                Component.text(String.format("Song added to queue: %s", Cornroot.songs.get(id).name)));
                inv.updateInventory(inv.page);
                return;
            }

            // Play song
            Cornroot.nowPlaying = new QueueItem(id, (Player) e.getWhoClicked());
            Song song = Cornroot.songs.get(id);

            // Broadcast play
            for (Player i : getServer().getOnlinePlayers()) {
                if (muteCache.containsKey(i.getUniqueId()) && muteCache.get(i.getUniqueId())) continue;
                i.sendActionBar(Component.text(String.format("Now Playing: %s", song.name)));
            }

            // Start playback
            song.playSong();
        } catch (NumberFormatException | NullPointerException | SQLException ignored) {
        }
    }

    @EventHandler
    void PlayerInventoryCloseEvent(InventoryCloseEvent e) {
        if (!inventory.containsKey(e.getPlayer()
                .getUniqueId())) return;
        inventory.remove(e.getPlayer()
                .getUniqueId());
    }

    public enum PageType {
        Home, Toast
    }

    public static class JukeboxInventory {
        public Player player;
        public Inventory inv;
        public PageType pageType;
        public int page;
        public boolean mute;

        public JukeboxInventory(Inventory inv, Player player) {
            this.inv = inv;
            this.player = player;
            this.page = 0;
            this.pageType = PageType.Home;
            this.mute = muteCache.containsKey(player.getUniqueId()) && muteCache.get(player.getUniqueId());
        }

        public void clearInventory(int... slot) {
            for (int i : slot) {
                inv.setItem(i, new ItemStack(Material.AIR));
            }
        }

        public void showInventoryToast(String msg, ItemMetaEditor itemMetaEditor) {
            this.pageType = PageType.Toast;

            inv.setItem(4, Util.cleanItemStack(Material.MAGENTA_STAINED_GLASS_PANE, 1, m -> {
                m.displayName(Component.text(msg));
                itemMetaEditor.run(m);
            }));

            inv.setItem(17, Util.cleanItemStack(Material.BARRIER, 1, m -> {
                m.displayName(Component.text("Back"));
                m.addEnchant(Enchantment.DURABILITY, 1, false);
                m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }));

            this.clearInventory(0, 1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
        }

        public void updateInventory(int page) {
            this.pageType = PageType.Home;

            // Add disks for page
            for (int i = 0; i < 9; i++) {
                int realIndex = page * 9 + i;
                if (realIndex >= Cornroot.songs.size()) {
                    continue;
                }
                Song song = Cornroot.songs.get(realIndex);

                inv.setItem(i,
                        Util.cleanItemStack(musicDisks[new Random(realIndex).nextInt(musicDisks.length)], 1, m -> {
                            ArrayList<Component> components = new ArrayList<>();
                            components.add(Component.text(String.format("Artist: %s", song.author),
                                    TextColor.color(255, 255, 255)));
                            components.add(
                                    Component.text(String.format("Length: %s", Util.songLength(song.secLength())),
                                            TextColor.color(255, 255, 255)));
                            m.lore(components);
                            m.getPersistentDataContainer()
                                    .set(idKey, PersistentDataType.INTEGER, realIndex);
                            m.displayName(Component.text(song.name));
                        }));
            }

            // Update queue and info
            this.updateQueueInfo();

            // Add page switchers
            inv.setItem(12, Util.cleanItemStack(Material.EMERALD, 1, m -> {
                m.displayName(Component.text("Previous Page"));
                if (page <= 0) return;
                m.getPersistentDataContainer()
                        .set(nextKey, PersistentDataType.INTEGER, page - 1);
                m.addEnchant(Enchantment.DURABILITY, 1, false);
                m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }));

            inv.setItem(14, Util.cleanItemStack(Material.EMERALD, 1, m -> {
                m.displayName(Component.text("Next Page"));
                if (Cornroot.songs.size() <= (page + 1) * 9) return;
                m.getPersistentDataContainer()
                        .set(nextKey, PersistentDataType.INTEGER, page + 1);
                m.addEnchant(Enchantment.DURABILITY, 1, false);
                m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }));

            // Add user info
            this.updateUserInfo();

            // Add mute button
            if (mute) inv.setItem(10, Util.cleanItemStack(Material.BARRIER, 1, m -> {
                m.displayName(Component.text("Unmute"));
                m.addEnchant(Enchantment.DURABILITY, 1, false);
                m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }));
            else inv.setItem(10, Util.cleanItemStack(Material.JUNGLE_SIGN, 1, m -> {
                m.displayName(Component.text("Mute"));
                m.addEnchant(Enchantment.DURABILITY, 1, false);
                m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }));

            // Add page number
            inv.setItem(13, Util.cleanItemStack(Material.GRAY_STAINED_GLASS_PANE, 1, m -> m.displayName(
                    Component.text(String.format("%d/%d", page + 1, Cornroot.songs.size() / 9 + 1)))));

            // Clear Inventory
            this.clearInventory(11, 15);
        }

        public void updateQueueInfo() {
            if (pageType != PageType.Home) return;

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

            ArrayList<String[]> info = new ArrayList<>();
            info.add(new String[]{"Creator", "Sigma#8214"});
            info.add(new String[]{"Version", "0.0.1"});
            info.add(new String[]{"Jukeboxes", String.valueOf(Cornroot.jukeboxes.size())});
            if (Cornroot.nowPlaying != null)
                info.add(new String[]{"Now Playing", Cornroot.songs.get(Cornroot.nowPlaying.songIndex).name});
            info.add(new String[]{"Total Plays", String.valueOf(totalPlays)});
            info.add(new String[]{"Global Plays", String.valueOf(globalPlays)});

            // Add Info
            inv.setItem(17, Util.cleanItemStack(Material.HEART_OF_THE_SEA, 1, m -> {
                m.displayName(Component.text("Info"));

                ArrayList<Component> components = new ArrayList<>();
                for (String[] i : info)
                    components.add(Component.text(String.format("%s: %s", i[0], i[1]), TextColor.color(255, 255, 255)));
                m.lore(components);
            }));

            // Add queue
            inv.setItem(16, Util.cleanItemStack(Material.ENDER_PEARL, 1, m -> {
                m.displayName(Component.text("Queue"));

                ArrayList<Component> components = new ArrayList<>();
                if (Cornroot.nowPlaying != null) components.add(Component.text(
                        String.format("Now Playing: %s", Cornroot.songs.get(Cornroot.nowPlaying.songIndex).name),
                        TextColor.color(255, 255, 255)));

                for (int i = 0; i < Cornroot.queue.size(); i++) {
                    if (i > 9) {
                        components.add(Component.text(String.format("%d more", Cornroot.queue.size() - 10),
                                TextColor.color(255, 255, 255)));
                        break;
                    }
                    components.add(Component.text(
                            String.format("#%d. %s", i + 1, Cornroot.songs.get(Cornroot.queue.get(i).songIndex).name),
                            TextColor.color(255, 255, 255)));
                }
                m.lore(components);
            }));
        }

        public void updateUserInfo() {
            if (pageType != PageType.Home) return;

            int keys = 0;
            int plays = 0;
            try {
                PreparedStatement stmt = Cornroot.database.connection.prepareStatement(
                        "SELECT keys FROM users WHERE uuid = ?1");
                PreparedStatement stmt2 = Cornroot.database.connection.prepareStatement(
                        "SELECT COUNT(*) FROM plays WHERE player = ?1");

                stmt.setString(1, String.valueOf(player.getUniqueId()));
                stmt2.setString(1, String.valueOf(player.getUniqueId()));

                keys = stmt.executeQuery()
                        .getInt(1);
                plays = stmt2.executeQuery()
                        .getInt(1);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            int finalKeys = keys;
            int finalPlays = plays;
            inv.setItem(9, Util.cleanItemStack(Material.PLAYER_HEAD, 1, m -> {
                m.displayName(Component.text("User Info"));
                ((SkullMeta) m).setOwningPlayer(player);

                List<Component> lore = new ArrayList<>();
                if (Util.bypassKeyCheck(player))
                    lore.add(Component.text("*Key bypass*", TextColor.color(255, 255, 255)));
                lore.add(Component.text(String.format("Name: %s", player.getName()), TextColor.color(255, 255, 255)));
                lore.add(Component.text(String.format("Keys: %d", finalKeys), TextColor.color(255, 255, 255)));
                lore.add(Component.text(String.format("Plays: %d", finalPlays), TextColor.color(255, 255, 255)));
                m.lore(lore);
            }));
        }
    }
}
