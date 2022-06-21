package com.connorcode.cornroot.commands;

import com.connorcode.cornroot.events.PlayerInteract;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import static org.bukkit.Bukkit.getServer;

public class Music implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player e = (Player) sender;
        Inventory inv = getServer().createInventory(null, 18, Component.text("Jukebox"));
        PlayerInteract.JukeboxInventory jukeboxInventory = new PlayerInteract.JukeboxInventory(inv, e);
        jukeboxInventory.updateInventory(0);
        PlayerInteract.inventory.put(e.getUniqueId(), jukeboxInventory);
        e.openInventory(inv);
        return true;
    }
}
