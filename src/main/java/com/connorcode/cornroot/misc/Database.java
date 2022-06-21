package com.connorcode.cornroot.misc;

import com.connorcode.cornroot.Cornroot;

import java.io.File;
import java.sql.*;

import static org.bukkit.plugin.java.JavaPlugin.getPlugin;

public class Database {
    public Connection connection;

    public Database(String path) {
        // Init database
        try {
            this.connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + getPlugin(Cornroot.class).getDataFolder() + File.separator + path);

            // Init tables
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("PRAGMA synchronous = NORMAL");
            stmt.executeUpdate("PRAGMA journal_mode = WAL");

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users (uuid TEXT NOT NULL, keys INTEGER NOT NULL, mute INTEGER NOT NULL, date INTEGER NOT NULL, UNIQUE(uuid))");
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS storage (id INTEGER PRIMARY KEY CHECK (id = 0), totalPlays INTEGER NOT NULL, globalPlays  INTEGER NOT NULL)");
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS plays (trackName TEXT NOT NULL, player TEXT NOT NULL, global INTEGER NOT NULL)");
            stmt.executeUpdate("INSERT OR IGNORE  INTO storage VALUES (0, 0, 0)");
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
