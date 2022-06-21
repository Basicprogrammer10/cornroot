package com.connorcode.cornroot.commands;

import com.connorcode.cornroot.Cornroot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class GlobalKeyList implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("[Cornroot] You are not OP (:o)"));
            return true;
        }

        StringBuilder out = new StringBuilder();
        try {
            PreparedStatement stmt = Cornroot.database.connection.prepareStatement(
                    "SELECT uuid, keys FROM users WHERE keys >= 1");
            ResultSet res = stmt.executeQuery();

            while (res.next()) {
                UUID uuid = UUID.fromString(res.getString(1));
                int keys = res.getInt(2);
                Player player = Bukkit.getOfflinePlayer(uuid)
                        .getPlayer();
                if (player != null) out.append(String.format("%s %d\n", player.getName(), keys));
                else out.append(String.format("%s %d\n", uuid, keys));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (out.length() < 1) sender.sendMessage(Component.join(Component.text(" "), Component.text("[Cornroot]"),
                Component.text("*nobody*", Style.style(TextDecoration.ITALIC))));
        else sender.sendMessage(Component.join(Component.text(" "), Component.text("[Cornroot]"),
                Component.text(out.substring(0, out.length() - 1))));
        return true;
    }
}
