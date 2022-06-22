package com.connorcode.cornroot.events;

import com.connorcode.cornroot.Config;
import com.connorcode.cornroot.Cornroot;
import com.connorcode.cornroot.Song;
import com.connorcode.cornroot.misc.ItemMetaEditor;
import com.connorcode.cornroot.misc.QueueItem;
import com.connorcode.cornroot.misc.Util;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
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
    static Style baseStyle = Style.style()
            .color(TextColor.color(NamedTextColor.GRAY))
            .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            .build();
    static Material[] musicDisks = new Material[]{Material.MUSIC_DISC_13, Material.MUSIC_DISC_CAT, Material.MUSIC_DISC_BLOCKS, Material.MUSIC_DISC_CHIRP, Material.MUSIC_DISC_FAR, Material.MUSIC_DISC_MALL, Material.MUSIC_DISC_MELLOHI, Material.MUSIC_DISC_STAL, Material.MUSIC_DISC_STRAD, Material.MUSIC_DISC_WARD, Material.MUSIC_DISC_11, Material.MUSIC_DISC_WAIT,};
    static NamespacedKey nextKey = new NamespacedKey(Cornroot.getPlugin(Cornroot.class), "next");
    static NamespacedKey idKey = new NamespacedKey(Cornroot.getPlugin(Cornroot.class), "id");

    @EventHandler
    void PlayerJukeboxInteractEvent(PlayerInteractEvent e) {
        if ((e.getAction() != Action.RIGHT_CLICK_BLOCK && !e.getPlayer()
                .isSneaking()) || !Cornroot.jukeboxes.contains(Objects.requireNonNull(e.getClickedBlock())
                .getLocation())) return;
        e.setCancelled(true);


        Inventory inv = getServer().createInventory(null, 36, Component.text("Jukebox"));
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
                uuid) || e.getClickedInventory() != inventory.get(uuid).inv) return;
        e.setCancelled(true);

        JukeboxInventory inv = inventory.get(uuid);

        // Process page change
        try {
            if (inv.pageType == PageType.Home && (e.getSlot() == 32 || e.getSlot() == 30)) {
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
        if (inv.pageType == PageType.OutOfKeys && e.getSlot() == 35) {
            inv.updateInventory(inv.page);
            return;
        }

        // Process toast print
        if (inv.pageType == PageType.OutOfKeys && e.getSlot() == 13) {
            e.getWhoClicked()
                    .sendMessage(Identity.nil(), Component.join(Component.newline(),
                            Component.text("Webstore link:", baseStyle.color(TextColor.color(NamedTextColor.AQUA))),
                            Component.text(String.format(" %s", Config.purchaseLink), Style.style()
                                    .color(TextColor.color(NamedTextColor.GOLD))
                                    .decorate(TextDecoration.UNDERLINED)
                                    .clickEvent(ClickEvent.openUrl(Config.fullPurchaseLink))
                                    .build())), MessageType.SYSTEM);
            inv.inv.close();
            return;
        }

        // Process mute
        if (inv.pageType == PageType.Home && e.getSlot() == 28) {
            inv.mute ^= true;
            muteCache.put(inv.player.getUniqueId(), inv.mute);
            inv.updateInventory(inv.page);
            return;
        }

        // Process confirm
        if (inv.pageType == PageType.Confirm) {
            // Accept
            if (e.getSlot() == 15) {
                inv.inv.close();
                if (!Util.bypassKeyCheck((Player) e.getWhoClicked())) {
                    // Remove a global music key
                    try {
                        PreparedStatement stmt = Cornroot.database.connection.prepareStatement(
                                "UPDATE users SET keys = keys - 1 WHERE uuid = ?1");
                        stmt.setString(1, String.valueOf(e.getWhoClicked()
                                .getUniqueId()));
                        stmt.executeUpdate();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }

                int id = Objects.requireNonNull(inv.inv.getStorageContents()[e.getSlot()].getItemMeta()
                        .getPersistentDataContainer()
                        .get(idKey, PersistentDataType.INTEGER));

                if (Cornroot.nowPlaying != null) {
                    Cornroot.queue.add(new QueueItem(id, (Player) e.getWhoClicked()));
                    e.getWhoClicked()
                            .sendActionBar(Component.text(
                                    String.format("Song added to queue: %s", Cornroot.songs.get(id).name),
                                    TextColor.color(NamedTextColor.GOLD)));
                    return;
                }

                // Play song
                Cornroot.nowPlaying = new QueueItem(id, (Player) e.getWhoClicked());
                Song song = Cornroot.songs.get(id);

                // Start playback
                song.playSong();
                return;
            }

            // Decline
            if (e.getSlot() == 11) {
                inv.pageType = PageType.Home;
                inv.updateInventory(inv.page);
                return;
            }
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
                    inv.showInventoryOutOfKeys("You dont have any global music keys", m -> m.lore(
                            Collections.singletonList(Component.text("Click for link to the webstore",
                                    baseStyle.color(NamedTextColor.GREEN)))));
                    return;
                }
            }

            inv.pageType = PageType.Confirm;
            inv.showInventorySongConfirm(id);
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
        Home, OutOfKeys, Confirm
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

        public void clearInventoryBut(int... slot) {
            for (int i = 0; i < 36; i++) {
                int finalI = i;
                if (Arrays.stream(slot)
                        .anyMatch(d -> d == finalI)) continue;
                inv.setItem(i, new ItemStack(Material.AIR));
            }
        }

        public void showInventorySongConfirm(int songIndex) {
            if (this.pageType != PageType.Confirm) return;

            int keys = 0;
            try {
                PreparedStatement stmt = Cornroot.database.connection.prepareStatement(
                        "SELECT keys FROM users WHERE uuid = ?1");
                stmt.setString(1, String.valueOf(player.getUniqueId()));
                keys = stmt.executeQuery()
                        .getInt(1);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            int finalKeys = keys;
            Song song = Cornroot.songs.get(songIndex);
            inv.setItem(4, Util.cleanItemStack(musicDisks[new Random(songIndex).nextInt(musicDisks.length)], 1, m -> {
                m.displayName(Component.text(song.name, baseStyle.color(TextColor.color(NamedTextColor.AQUA))));
                ArrayList<Component> components = new ArrayList<>();
                components.add(Component.text(String.format("Artist: %s", song.author), baseStyle));
                components.add(
                        Component.text(String.format("Length: %s", Util.songLength(song.secLength())), baseStyle));
                m.lore(components);
            }));

            inv.setItem(11, Util.cleanItemStack(Material.RED_STAINED_GLASS_PANE, 1,
                    m -> m.displayName(Component.text("Deny", baseStyle.color(NamedTextColor.RED)))));

            inv.setItem(15, Util.cleanItemStack(Material.LIME_STAINED_GLASS_PANE, 1, m -> {
                m.displayName(Component.text("Confirm", baseStyle.color(NamedTextColor.GREEN)));
                m.getPersistentDataContainer()
                        .set(idKey, PersistentDataType.INTEGER, songIndex);
            }));

            inv.setItem(22, Util.cleanItemStack(Material.TRIPWIRE_HOOK, 1, m -> m.displayName(
                    Component.join(Component.text(" "),
                            Component.text("Your keys:", baseStyle.color(NamedTextColor.YELLOW)),
                            Component.text(finalKeys, baseStyle)))));

            this.clearInventoryBut(4, 11, 15, 22);
        }

        public void showInventoryOutOfKeys(String msg, ItemMetaEditor itemMetaEditor) {
            this.pageType = PageType.OutOfKeys;

            inv.setItem(13, Util.cleanItemStack(Material.MAGENTA_STAINED_GLASS_PANE, 1, m -> {
                m.displayName(Component.text(msg, baseStyle));
                itemMetaEditor.run(m);
            }));

            inv.setItem(35,
                    Util.cleanItemStack(Material.BARRIER, 1, m -> m.displayName(Component.text("Back", baseStyle))));

            this.clearInventoryBut(13, 35);
        }

        public void updateInventory(int page) {
            this.pageType = PageType.Home;

            // Add disks for page
            for (int i = 0; i < 18; i++) {
                int realIndex = page * 18 + i;
                if (realIndex >= Cornroot.songs.size()) {
                    this.clearInventory(i);
                    continue;
                }
                Song song = Cornroot.songs.get(realIndex);

                inv.setItem(i,
                        Util.cleanItemStack(musicDisks[new Random(realIndex).nextInt(musicDisks.length)], 1, m -> {
                            ArrayList<Component> components = new ArrayList<>();
                            components.add(Component.text(String.format("Artist: %s", song.author), baseStyle));
                            components.add(
                                    Component.text(String.format("Length: %s", Util.songLength(song.secLength())),
                                            baseStyle));
                            m.lore(components);
                            m.getPersistentDataContainer()
                                    .set(idKey, PersistentDataType.INTEGER, realIndex);
                            m.displayName(
                                    Component.text(song.name, baseStyle.color(TextColor.color(NamedTextColor.AQUA))));
                        }));
            }

            // Page break
            for (int i = 18; i < 27; i++)
                inv.setItem(i,
                        Util.cleanItemStack(Material.RED_STAINED_GLASS_PANE, 1, m -> m.displayName(Component.empty())));

            // Add page switchers
            if (page > 0) inv.setItem(30, Util.cleanItemStack(Material.BOOK, 1, m -> {
                m.displayName(Component.text("Previous Page", baseStyle.color(TextColor.color(NamedTextColor.YELLOW))));
                m.getPersistentDataContainer()
                        .set(nextKey, PersistentDataType.INTEGER, page - 1);
            }));
            else this.clearInventory(30);

            if (Cornroot.songs.size() > (page + 1) * 18) inv.setItem(32, Util.cleanItemStack(Material.BOOK, 1, m -> {
                m.displayName(Component.text("Next Page", baseStyle.color(TextColor.color(NamedTextColor.YELLOW))));
                m.getPersistentDataContainer()
                        .set(nextKey, PersistentDataType.INTEGER, page + 1);
            }));
            else this.clearInventory(32);

            // Add mute button
            if (mute) inv.setItem(28, Util.cleanItemStack(Material.RED_WOOL, 1, m -> m.displayName(
                    Component.text("MUSIC OFF", baseStyle.color(TextColor.color(NamedTextColor.RED))))));
            else inv.setItem(28, Util.cleanItemStack(Material.LIME_WOOL, 1, m -> m.displayName(
                    Component.text("MUSIC ON", baseStyle.color(TextColor.color(NamedTextColor.GREEN))))));

            // Add page number
            inv.setItem(31, Util.cleanItemStack(Material.BLACK_STAINED_GLASS_PANE, 1, m -> m.displayName(
                    Component.text(String.format("%d/%d", page + 1, Cornroot.songs.size() / 18 + 1),
                            baseStyle.color(TextColor.color(NamedTextColor.YELLOW))))));

            // Update queue and info
            this.updateQueueInfo();

            // Add user info
            this.updateUserInfo();

            // Clear Inventory
            this.clearInventory(29, 33);
        }

        public void updateQueueInfo() {
            if (pageType != PageType.Home) return;

            // Gen info
//            int totalPlays = 0;
            int globalPlays = 0;
            try {
                ResultSet res = Cornroot.database.connection.prepareStatement(
                                "SELECT totalPlays, globalPlays FROM storage LIMIT 1")
                        .executeQuery();
//                totalPlays = res.getInt(1);
                globalPlays = res.getInt(2);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            ArrayList<String[]> info = new ArrayList<>();
            info.add(new String[]{"Creator", "Sigma#8214"});
            info.add(new String[]{"Version", "0.0.1"});
            // info.add(new String[]{"Jukeboxes", String.valueOf(Cornroot.jukeboxes.size())});
            info.add(new String[]{"------------------", ""});
            info.add(new String[]{"Songs", String.valueOf(Cornroot.songs.size())});
            // info.add(new String[]{"Total Plays", String.valueOf(totalPlays)});
            info.add(new String[]{"Global Plays", String.valueOf(globalPlays)});

            // Add Info
            inv.setItem(35, Util.cleanItemStack(Material.HEART_OF_THE_SEA, 1, m -> {
                m.displayName(Component.text("Info", baseStyle.color(TextColor.color(NamedTextColor.YELLOW))));

                ArrayList<Component> components = new ArrayList<>();
                for (String[] i : info) {
                    if (i[1].isEmpty())
                        components.add(Component.text(i[0], baseStyle.color(TextColor.color(NamedTextColor.RED))));
                    else components.add(Component.text(String.format("%s: %s", i[0], i[1]), baseStyle));
                }
                m.lore(components);
            }));

            // Add queue
            inv.setItem(34, Util.cleanItemStack(Material.ENDER_PEARL, 1, m -> {
                m.displayName(Component.text("Queue", baseStyle.color(TextColor.color(NamedTextColor.YELLOW))));

                ArrayList<Component> components = new ArrayList<>();
                if (Cornroot.nowPlaying != null) components.add(Component.text(
                        String.format("Now Playing: %s", Cornroot.songs.get(Cornroot.nowPlaying.songIndex).name),
                        baseStyle));

                for (int i = 0; i < Cornroot.queue.size(); i++) {
                    if (i > 9) {
                        components.add(Component.text(String.format("%d more", Cornroot.queue.size() - 10), baseStyle));
                        break;
                    }
                    components.add(Component.text(
                            String.format("#%d. %s", i + 1, Cornroot.songs.get(Cornroot.queue.get(i).songIndex).name),
                            baseStyle));
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
            inv.setItem(27, Util.cleanItemStack(Material.PLAYER_HEAD, 1, m -> {
                m.displayName(Component.text("User Info", baseStyle.color(TextColor.color(NamedTextColor.YELLOW))));
                ((SkullMeta) m).setOwningPlayer(player);

                List<Component> lore = new ArrayList<>();
                if (Util.bypassKeyCheck(player)) lore.add(Component.text("*Key bypass*", baseStyle));
                lore.add(Component.text(String.format("Name: %s", player.getName()), baseStyle));
                lore.add(Component.text(String.format("Your keys: %d", finalKeys), baseStyle));
                lore.add(Component.text(String.format("Times played: %d", finalPlays), baseStyle));
                m.lore(lore);
            }));
        }
    }
}
