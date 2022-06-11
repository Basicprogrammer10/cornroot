package com.connorcode.cornroot.events;

import com.connorcode.cornroot.Cornroot;
import com.connorcode.cornroot.misc.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Objects;

import static org.bukkit.Bukkit.getServer;

public class PlayerInteract implements Listener {
    Inventory inv = getServer().createInventory(null, 18, Component.text("Jukebox"));
    String[][] info = new String[][]{{"Creator", "Sigma#8214"}, {"Version", "0.0.1"}, {"Jukeboxes", String.valueOf(
            Cornroot.jukeboxes.size())}};

    @EventHandler
    void PlayerJukeboxInteractEvent(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && !e.getPlayer()
                .isSneaking() || !Cornroot.jukeboxes.contains(Objects.requireNonNull(e.getClickedBlock())
                .getLocation())) return;
        e.setCancelled(true);


        inv.setItem(0, new ItemStack(Material.MUSIC_DISC_CHIRP));
        inv.setItem(17, Util.cleanItemStack(Material.HEART_OF_THE_SEA, 1, m -> {
            m.displayName(Component.text("Info"));

            ArrayList<Component> components = new ArrayList<>();
            for (String[] i : info)
                components.add(Component.text(String.format("%s: %s", i[0], i[1]), TextColor.color(255, 255, 255)));
            m.lore(components);
        }));

        e.getPlayer()
                .openInventory(inv);

        e.getPlayer()
                .playSound(Objects.requireNonNull(e.getClickedBlock())
                        .getLocation(), Sound.MUSIC_DISC_CHIRP.key()
                        .asString(), 1f, 1f);
    }

    @EventHandler
    void PlayerInventoryInteractEvent(InventoryClickEvent e) {
        if (e.getAction() == InventoryAction.NOTHING || e.getInventory() != inv)
            return;
        e.setCancelled(true);
    }
}
