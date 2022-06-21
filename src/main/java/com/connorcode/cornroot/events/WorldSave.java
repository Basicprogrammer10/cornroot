package com.connorcode.cornroot.events;

import com.connorcode.cornroot.misc.Util;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;

public class WorldSave implements Listener {
    private static long lastSave = System.currentTimeMillis();

    @EventHandler
    void worldSave(WorldSaveEvent e) {
        if (System.currentTimeMillis() - lastSave < 1000 * 60) return;
        Util.saveMuteCache();
        lastSave = System.currentTimeMillis();
    }
}
