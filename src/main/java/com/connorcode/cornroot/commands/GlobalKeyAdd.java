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
        //noinspection DuplicatedCode
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("[Cornroot] You are not OP (:o)"));
            return true;
        }

        if (args.length != 1 && args.length != 2) {
            sender.sendMessage(Component.text("[Cornroot] Invalid argument count"));
            return true;
        }

        int count = 1;
        if (args.length == 2) count = Integer.parseInt(args[1]);

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
                    "INSERT OR IGNORE INTO users (uuid, keys, mute, date) VALUES (?1, 0, 0, strftime('%s','now'));");
            PreparedStatement stmt2 = Cornroot.database.connection.prepareStatement(
                    "UPDATE users SET keys = keys + ?2 WHERE uuid = ?1;");

            stmt.setString(1, uuid.toString());
            stmt2.setString(1, uuid.toString());
            stmt2.setInt(2, count);

            stmt.executeUpdate();
            stmt2.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String resp = "[Cornroot] Added key to player";
        if (player.getPlayer() != null) resp = String.format("[Cornroot] Added key to player `%s`", player.getName());

        sender.sendMessage(Component.text(resp));
        return true;
    }
}
