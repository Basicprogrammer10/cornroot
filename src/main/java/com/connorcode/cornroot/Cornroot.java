package com.connorcode.cornroot;

import com.connorcode.cornroot.commands.*;
import com.connorcode.cornroot.events.PlayerInteract;
import com.connorcode.cornroot.events.WorldSave;
import com.connorcode.cornroot.misc.Database;
import com.connorcode.cornroot.misc.QueueItem;
import com.connorcode.cornroot.misc.Util;
import org.bukkit.Location;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Level;

public final class Cornroot extends JavaPlugin {
    public static final List<Location> jukeboxes = new ArrayList<>();
    public static final List<Song> songs = new ArrayList<>();
    public static final List<QueueItem> queue = new ArrayList<>();
    public static final String version = "1.0.0";
    public static Database database;
    public static QueueItem nowPlaying = null;
    final File configFile = new File(getDataFolder() + File.separator + "config.yml");
    final File songFolder = new File(getDataFolder() + File.separator + "songs");

    @Override
    public void onEnable() {
        // Init config file and database
        if (!configFile.exists()) saveDefaultConfig();
        assert songFolder.exists() || songFolder.mkdir();
        database = new Database("data.db");
        Util.refreshMuteCache();

        // Load jukebox positions
        List<String> rawJukeboxes = getConfig().getStringList("jukeboxes");
        for (String i : rawJukeboxes) {
            String[] stringParts = i.split(",");
            jukeboxes.add(new Location(getServer().getWorld(stringParts[0]), Double.parseDouble(stringParts[1]),
                    Double.parseDouble(stringParts[3]), Double.parseDouble(stringParts[2])));
        }

        // Load songs
        for (File i : Objects.requireNonNull(songFolder.listFiles())) {
            if (!i.isFile() || !i.getName()
                    .endsWith(".nbs")) continue;
            getLogger().log(Level.INFO, String.format("Loading song `%s`", i.getName()));
            try {
                songs.add(new Song(i));
            } catch (Exception e) {
                getLogger().log(Level.INFO, String.format("Error loading `%s`:", i.getName()));
                e.printStackTrace();
            }
        }

        // Init Event Handlers
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerInteract(), this);
        pm.registerEvents(new WorldSave(), this);

        // Init Commands
        Objects.requireNonNull(getServer().getPluginCommand("globalkeyadd"))
                .setExecutor(new GlobalKeyAdd());
        Objects.requireNonNull(getServer().getPluginCommand("globalkeyremove"))
                .setExecutor(new GlobalKeyRemove());
        Objects.requireNonNull(getServer().getPluginCommand("globalkeylist"))
                .setExecutor(new GlobalKeyList());
        Objects.requireNonNull(getServer().getPluginCommand("music"))
                .setExecutor(new Music());

        Objects.requireNonNull(getServer().getPluginCommand("globalkeyadd"))
                .setTabCompleter(new GlobalKeyCompletor());
        Objects.requireNonNull(getServer().getPluginCommand("globalkeyremove"))
                .setTabCompleter(new GlobalKeyCompletor());

        // Version check
        try {
            checkVersion();
        } catch (IOException ignore) {
        }
    }

    void checkVersion() throws IOException {
        URL url = new URL("https://version.connorcode.com/cornroot/status?code=yVFq6y88wDa3Ye5Rn3Pc");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("accept", "text/plain");
        InputStream responseStream = connection.getInputStream();

        Scanner scanner = new Scanner(responseStream, StandardCharsets.UTF_8.name());
        String version = scanner.useDelimiter("\\A")
                .next()
                .split(",")[0];

        if (version.equals(Cornroot.version)) return;
        getLogger().log(Level.WARNING, String.format("Version Outdated! (%s > %s)", version, Cornroot.version));
        getLogger().log(Level.WARNING, String.format(
                "Download new version at https://version.connorcode.com/cornroot/file/%s?code=yVFq6y88wDa3Ye5Rn3Pc",
                version));
    }

    @Override
    public void onDisable() {
        Util.saveMuteCache();
    }
}
