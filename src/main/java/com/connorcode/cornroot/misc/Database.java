package com.connorcode.cornroot.misc;

import com.connorcode.cornroot.Cornroot;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.bukkit.plugin.java.JavaPlugin.getPlugin;

public class Database {
    public Connection database;

    public Database(String path) {
        // Init database
        try {
            this.database = DriverManager.getConnection(
                    "jdbc:sqlite:" + getPlugin(Cornroot.class).getDataFolder() + File.separator + path);

            // Init tables
            Statement stmt = database.createStatement();
            stmt.executeUpdate("PRAGMA synchronous = NORMAL");
            stmt.executeUpdate("PRAGMA journal_mode = WAL");

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users (uuid BLOB NOT NULL, perms INTEGER NOT NULL, date INTEGER NOT NULL, UNIQUE(uuid))");
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS storage (totalPlays INTEGER NOT NULL, globalPlays  INTEGER NOT NULL)");
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS trackStats (trackName TEXT NOT NULL, playCount INTEGER NOT NULL, UNIQUE(trackName))");
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
