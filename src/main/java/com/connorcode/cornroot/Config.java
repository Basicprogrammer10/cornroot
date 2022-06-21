package com.connorcode.cornroot;

import org.bukkit.configuration.file.FileConfiguration;

public class Config {
    private static final FileConfiguration cfg = Cornroot.getPlugin(Cornroot.class).getConfig();
    public static final float baseVolume = (float) cfg.getDouble("baseVolume");
    public static final String purchaseLink = cfg.getString("purchaseLink");
}
