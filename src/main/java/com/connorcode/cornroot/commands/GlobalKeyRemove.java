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

public class GlobalKeyRemove implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("[Cornroot] You are not OP"));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("[Cornroot] Invalid argument count"));
            return true;
        }

        @NotNull OfflinePlayer player;
        UUID uuid;
        try {
            uuid = UUID.fromString(args[0]);
            player = Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ignored) {
            sender.sendMessage(Component.text("[Cornroot] Invalid UUID"));
            return true;
        }

        try {
            PreparedStatement stmt = Cornroot.database.connection.prepareStatement("DELETE FROM users WHERE uuid = ?1");
            stmt.setString(1, uuid.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        sender.sendMessage(Component.text(String.format("[Cornroot] Removed user `%s`", player.getName())));
        return true;
    }
}
