package com.connorcode.cornroot.commands;

import com.connorcode.cornroot.Cornroot;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class GlobalKeyAdd implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("[Cornroot] You are not OP"));
            return false;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(args[0]);
        } catch (IllegalArgumentException ignored) {
            sender.sendMessage(Component.text("[Cornroot] Invalid UUID"));
            return false;
        }

        try {
            PreparedStatement stmt = Cornroot.database.connection.prepareStatement("INSERT INTO users (uuid, perms, date) VALUES (?, 1, strftime())");
            stmt.setString(1, uuid.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        sender.sendMessage(Component.text("[Cornroot] Success!"));
        return false;
    }
}
