package com.connorcode.cornroot.events;

import com.connorcode.cornroot.Cornroot;
import com.connorcode.cornroot.misc.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import static org.bukkit.Bukkit.getServer;

public class PlayerInteract implements Listener {
    Inventory inv = getServer().createInventory(null, 18, Component.text("Jukebox"));

    @EventHandler
    void PlayerJukeboxInteractEvent(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && !e.getPlayer()
                .isSneaking() || !Cornroot.jukeboxes.contains(Objects.requireNonNull(e.getClickedBlock())
                .getLocation())) return;
        e.setCancelled(true);

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

        inv.setItem(0, Util.cleanItemStack(Material.MUSIC_DISC_CHIRP, 1,
                m -> m.lore(Collections.singletonList(Component.text("ID: 23")))));
        inv.setItem(17, Util.cleanItemStack(Material.HEART_OF_THE_SEA, 1, m -> {
            m.displayName(Component.text("Info"));

            ArrayList<Component> components = new ArrayList<>();
            for (String[] i : info)
                components.add(Component.text(String.format("%s: %s", i[0], i[1]), TextColor.color(255, 255, 255)));
            m.lore(components);
        }));

        e.getPlayer()
                .openInventory(inv);
    }

    @EventHandler
    void PlayerInventoryInteractEvent(InventoryClickEvent e) {
        if (e.getAction() == InventoryAction.NOTHING || e.getInventory() != inv) return;
        e.setCancelled(true);

        // Get music id (if any)
        try {
            int id = Integer.parseInt(((TextComponent) Objects.requireNonNull(Objects.requireNonNull(e.getCurrentItem())
                            .lore())
                    .get(0)).content()
                    .split(":")[1].trim());

            // Increment stats
            try {
                Cornroot.database.connection.prepareStatement("UPDATE storage SET totalPlays = totalPlays+1")
                        .executeUpdate();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            // TMP - Play chirp on client
            if (id != 23) return;
            ((Player) e.getWhoClicked()).playSound(e.getWhoClicked()
                    .getLocation(), Sound.MUSIC_DISC_CHIRP.key()
                    .asString(), 1f, 1f);
            e.getWhoClicked()
                    .showTitle(Title.title(Component.empty(), Component.text("Now Playing: CoolBeanMusic")));
        } catch (NumberFormatException | NullPointerException ignored) {
        }
    }
}
