package com.connorcode.cornroot.misc;

import org.bukkit.entity.Player;

public class QueueItem {
    public final int songIndex;
    public final Player player;

    public QueueItem(int songIndex, Player player) {
        this.songIndex = songIndex;
        this.player = player;
    }
}
