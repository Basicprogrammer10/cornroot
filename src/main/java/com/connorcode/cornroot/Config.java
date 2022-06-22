package com.connorcode.cornroot;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

public class Config {
    private static final FileConfiguration cfg = Cornroot.getPlugin(Cornroot.class)
            .getConfig();
    public static final float baseVolume = (float) cfg.getDouble("baseVolume");
    public static final String purchaseLink = Objects.requireNonNull(cfg.getString("displayPurchaseLink"));
    public static final String fullPurchaseLink = Objects.requireNonNull(cfg.getString("fullPurchaseLink"));
}
