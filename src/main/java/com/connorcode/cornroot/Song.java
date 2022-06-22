package com.connorcode.cornroot;

import com.connorcode.cornroot.events.PlayerInteract;
import com.connorcode.cornroot.misc.MutInt;
import com.connorcode.cornroot.misc.QueueItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.bukkit.Bukkit.getServer;

public class Song {
    public String name;
    public String author;
    public float tempo;
    public short length;
    public List<Note> notes;


    Song(File file) throws Exception {
        byte[] bytes = Files.readAllBytes(file.toPath());
        MutInt i = new MutInt(0);

        // Version check
        assert readShort(i, bytes) == 0;

        // Get song length
        i.increment(4);
        this.length = readShort(i, bytes);

        // Get song name
        i.increment(2);
        this.name = readString(i, bytes);
        this.author = readString(i, bytes);

        // Get tempo
        skipStrings(i, bytes, 2);
        this.tempo = readShort(i, bytes) / 100f;

        i.increment(23);
        skipStrings(i, bytes, 1);
        i.increment(4);

        // Read notes
        ArrayList<Note> notes = new ArrayList<>();
        int value = -1;
        while (true) {
            short noteJumpTicks = readShort(i, bytes);
            value += noteJumpTicks;
            if (noteJumpTicks == 0) break;

            while (true) {
                short layerJumpTicks = readShort(i, bytes);
//                value += layerJumpTicks;
                if (layerJumpTicks == 0) break;

                Instrument instrument = readInstrument(i, bytes).orElse(Instrument.Piano);
                byte key = readByte(i, bytes);
                i.increment(2);
                short pitch = readShort(i, bytes, true);

                notes.add(new Note(value, instrument, key, pitch));
            }
        }
        this.notes = notes;
    }

    static byte readByte(MutInt i, byte[] data) {
        i.increment(1);
        return data[i.value - 1];
    }

    static short readShort(MutInt i, byte[] data) {
        return readShort(i, data, false);
    }

    static short readShort(MutInt i, byte[] data, boolean signed) {
        int a = data[i.value];
        int b = data[i.value + 1];
        if (!signed) {
            a &= 255;
            b &= 255;
        }

        i.increment(2);
        return (short) (a + (b << 8));
    }

    static int readInt(MutInt i, byte[] data) {
        int a = data[i.value];
        int b = data[i.value + 1];
        int c = data[i.value + 2];
        int d = data[i.value + 3];

        i.increment(4);
        return ((d << 24) + (c << 16) + (b << 8) + a);
    }

    static String readString(MutInt i, byte[] data) {
        int length = readInt(i, data);
        int startIndex = i.value;
        i.increment(length);

        return new String(Arrays.copyOfRange(data, startIndex, startIndex + length));
    }

    static void skipStrings(MutInt i, byte[] data, int strings) {
        for (int j = 0; j < strings; j++) readString(i, data);
    }

    static Optional<Instrument> readInstrument(MutInt i, byte[] data) {
        byte rawIns = data[i.value];
        i.increment(1);

        switch (rawIns) {
            case 0:
                return Optional.of(Instrument.Piano);
            case 1:
                return Optional.of(Instrument.DoubleBass);
            case 2:
                return Optional.of(Instrument.BassDrum);
            case 3:
                return Optional.of(Instrument.SnareDrum);
            case 4:
                return Optional.of(Instrument.Click);
            case 5:
                return Optional.of(Instrument.Guitar);
            case 6:
                return Optional.of(Instrument.Flute);
            case 7:
                return Optional.of(Instrument.Bell);
            case 8:
                return Optional.of(Instrument.Chime);
            case 9:
                return Optional.of(Instrument.Xylophone);
            case 10:
                return Optional.of(Instrument.IronXylophone);
            case 11:
                return Optional.of(Instrument.CowBell);
            case 12:
                return Optional.of(Instrument.Didgeridoo);
            case 13:
                return Optional.of(Instrument.Bit);
            case 14:
                return Optional.of(Instrument.Banjo);
            case 15:
                return Optional.of(Instrument.Pling);
        }
        return Optional.empty();
    }

    public void playSong() {
        Bukkit.getScheduler()
                .runTaskAsynchronously(Cornroot.getPlugin(Cornroot.class), () -> {
                    Bukkit.getScheduler()
                            .runTaskAsynchronously(Cornroot.getPlugin(Cornroot.class), () -> {
                                // Broadcast play
                                for (Player i : getServer().getOnlinePlayers()) {
                                    if (PlayerInteract.muteCache.containsKey(
                                            i.getUniqueId()) && PlayerInteract.muteCache.get(i.getUniqueId())) continue;
                                    i.sendActionBar(Component.text(String.format("Now Playing: %s",
                                                    Cornroot.songs.get(Cornroot.nowPlaying.songIndex).name),
                                            TextColor.color(NamedTextColor.GOLD)));
                                }

                                // Increment stats
                                try {
                                    Cornroot.database.connection.prepareStatement(
                                                    "UPDATE storage SET totalPlays = totalPlays + 1, globalPlays = globalPlays + 1")
                                            .executeUpdate();

                                    PreparedStatement stmt = Cornroot.database.connection.prepareStatement(
                                            "INSERT INTO plays (trackName, player, global) VALUES (?1, ?2, 1)");
                                    stmt.setString(1, Cornroot.songs.get(Cornroot.nowPlaying.songIndex).name);
                                    stmt.setString(2, String.valueOf(Cornroot.nowPlaying.player.getUniqueId()));
                                    stmt.executeUpdate();
                                } catch (SQLException ex) {
                                    ex.printStackTrace();
                                }

                                // Update jukebox inventorys
                                PlayerInteract.inventory.values()
                                        .forEach(PlayerInteract.JukeboxInventory::updateQueueInfo);
                                PlayerInteract.inventory.values()
                                        .forEach(PlayerInteract.JukeboxInventory::updateUserInfo);
                            });

                    int lastTick = 0;
                    for (Song.Note i : this.notes) {
                        try {
                            if (i.tick - lastTick != 0)
                                Thread.sleep((long) ((float) (i.tick - lastTick) / (this.tempo / 1000)));
                            lastTick = i.tick;
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        for (Player p : getServer().getOnlinePlayers()) {
                            if (PlayerInteract.muteCache.containsKey(p.getUniqueId()) && PlayerInteract.muteCache.get(
                                    p.getUniqueId())) continue;
                            p.playSound(p.getEyeLocation(), i.getSound(), Config.baseVolume, i.getPitch());
                        }
                    }

                    if (Cornroot.queue.isEmpty()) {
                        Cornroot.nowPlaying = null;
                        return;
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    QueueItem queueItem = Cornroot.queue.remove(0);
                    Cornroot.nowPlaying = queueItem;

                    Bukkit.getScheduler()
                            .runTaskAsynchronously(Cornroot.getPlugin(Cornroot.class),
                                    () -> PlayerInteract.inventory.values()
                                            .forEach(PlayerInteract.JukeboxInventory::updateQueueInfo));
                    Cornroot.songs.get(queueItem.songIndex)
                            .playSong();
                });
    }

    public float secLength() {
        return this.length / this.tempo;
    }

    public enum Instrument {
        Piano, DoubleBass, BassDrum, SnareDrum, Click, Guitar, Flute, Bell, Chime, Xylophone, IronXylophone, CowBell, Didgeridoo, Bit, Banjo, Pling
    }

    public static class Note {
        public int tick;
        public Instrument instrument;
        public byte key;
        public short pitch;

        Note(int tick, Instrument instrument, byte key, short pitch) {
            this.tick = tick;
            this.instrument = instrument;
            this.key = key;
            this.pitch = pitch;
        }

        public float getPitch() {
            return (float) Math.pow(2, (-12 + this.key - 33) / 12f);
        }

        public Sound getSound() {
            switch (instrument) {
                case Piano:
                    return Sound.BLOCK_NOTE_BLOCK_HARP;
                case DoubleBass:
                    return Sound.BLOCK_NOTE_BLOCK_BASS;
                case BassDrum:
                    return Sound.BLOCK_NOTE_BLOCK_BASEDRUM;
                case SnareDrum:
                    return Sound.BLOCK_NOTE_BLOCK_SNARE;
                case Click:
                    return Sound.BLOCK_NOTE_BLOCK_HAT;
                case Guitar:
                    return Sound.BLOCK_NOTE_BLOCK_GUITAR;
                case Flute:
                    return Sound.BLOCK_NOTE_BLOCK_FLUTE;
                case Bell:
                    return Sound.BLOCK_NOTE_BLOCK_BELL;
                case Chime:
                    return Sound.BLOCK_NOTE_BLOCK_CHIME;
                case Xylophone:
                    return Sound.BLOCK_NOTE_BLOCK_XYLOPHONE;
                case IronXylophone:
                    return Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE;
                case CowBell:
                    return Sound.BLOCK_NOTE_BLOCK_COW_BELL;
                case Didgeridoo:
                    return Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO;
                case Bit:
                    return Sound.BLOCK_NOTE_BLOCK_BIT;
                case Banjo:
                    return Sound.BLOCK_NOTE_BLOCK_BANJO;
                case Pling:
                    return Sound.BLOCK_NOTE_BLOCK_PLING;
            }
            return null;
        }

        @Override
        public String toString() {
            return "Note{" + "tick=" + tick + ", instrument=" + instrument + ", key=" + key + ", pitch=" + pitch + '}';
        }
    }
}
