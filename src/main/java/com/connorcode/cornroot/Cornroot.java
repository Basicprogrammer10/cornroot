package com.connorcode.cornroot;

import com.connorcode.cornroot.events.PlayerInteract;
import com.connorcode.cornroot.misc.Database;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class Cornroot extends JavaPlugin {
    File config = new File(getDataFolder() + File.separator + "config.yml");
    public static Database database;
    public static List<Location> jukeboxes = new ArrayList<>();

    @Override
    public void onEnable() {
        // Init config file and database
        if (!config.exists()) saveDefaultConfig();
        database = new Database("data.db");

        // Load jukebox positions
        List<String> rawJukeboxes = getConfig().getStringList("jukeboxes");
        for (String i : rawJukeboxes) {
            String[] stringParts = i.split(",");
            jukeboxes.add(new Location(getServer().getWorld(stringParts[0]), Double.parseDouble(stringParts[1]),
                    Double.parseDouble(stringParts[3]), Double.parseDouble(stringParts[2])));
        }

        // Init Event Handlers
        getServer().getPluginManager().registerEvents(new PlayerInteract(), this);
    }

    @Override
    public void onDisable() { }
}
