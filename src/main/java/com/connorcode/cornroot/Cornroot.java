package com.connorcode.cornroot;

import com.connorcode.cornroot.events.PlayerInteract;
import com.connorcode.cornroot.misc.Database;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public final class Cornroot extends JavaPlugin {
    File config = new File(getDataFolder() + File.separator + "config.yml");
    File songFolder = new File(getDataFolder() + File.separator + "songs");
    public static Database database;
    public static List<Location> jukeboxes = new ArrayList<>();
    public static List<Song> songs = new ArrayList<>();

    @Override
    public void onEnable() {
        // Init config file and database
        if (!config.exists()) saveDefaultConfig();
        assert songFolder.exists() || songFolder.mkdir();
        database = new Database("data.db");

        // Load jukebox positions
        List<String> rawJukeboxes = getConfig().getStringList("jukeboxes");
        for (String i : rawJukeboxes) {
            String[] stringParts = i.split(",");
            jukeboxes.add(new Location(getServer().getWorld(stringParts[0]), Double.parseDouble(stringParts[1]),
                    Double.parseDouble(stringParts[3]), Double.parseDouble(stringParts[2])));
        }

        // Load songs
        for (File i: Objects.requireNonNull(songFolder.listFiles())) {
            if (!i.isFile() || !i.getName().endsWith(".nbs")) continue;
            getLogger().log(Level.INFO, String.format("Loading song `%s`", i.getName()));
            try {
                songs.add(new Song(i));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Init Event Handlers
        getServer().getPluginManager().registerEvents(new PlayerInteract(), this);
    }

    @Override
    public void onDisable() { }
}
