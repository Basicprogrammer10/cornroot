package com.connorcode.cornroot.commands;

import com.connorcode.cornroot.Cornroot;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class GlobalKeyAdd implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("[Cornroot] You are not OP (:o)"));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("[Cornroot] Invalid argument count"));
            return true;
        }

        UUID uuid;
        @NotNull OfflinePlayer player;
        try {
            uuid = UUID.fromString(args[0]);
            player = Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ignored) {
            sender.sendMessage(Component.text("[Cornroot] Invalid UUID"));
            return true;
        }

        try {
            PreparedStatement stmt = Cornroot.database.connection.prepareStatement(
                    "INSERT OR IGNORE INTO users (uuid, perms, date) VALUES (?1, 1, strftime('%s','now'))");
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String resp = "[Cornroot] Added player";
        if (player.getPlayer() != null) resp = String.format("[Cornroot] Added player `%s`", player.getName());

        sender.sendMessage(Component.text(resp));
        return true;
    }
}
