package com.connorcode.cornroot;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class Cornroot extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().log(Level.ALL, "Starting");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
