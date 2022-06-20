package com.connorcode.cornroot;

import com.connorcode.cornroot.misc.MutInt;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
                value += layerJumpTicks;
                if (layerJumpTicks == 0) break;

                Instrument instrument = readInstrument(i, bytes).orElse(Instrument.Piano);
                byte key = readByte(i, bytes);
                i.increment(2);
                short pitch = readShort(i, bytes, true);

                notes.add(new Note(noteJumpTicks, layerJumpTicks, instrument, key, pitch));
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

    public float secLength() {
        return this.length / this.tempo;
    }

    public enum Instrument {
        Piano, DoubleBass, BassDrum, SnareDrum, Click, Guitar, Flute, Bell, Chime, Xylophone, IronXylophone, CowBell, Didgeridoo, Bit, Banjo, Pling
    }

    public static class Note {
        public short noteJumpTicks;
        public short layerJumpTicks;
        public Instrument instrument;
        public byte key;
        public short pitch;

        Note(short noteJumpTicks, short layerJumpTicks, Instrument instrument, byte key, short pitch) {
            this.noteJumpTicks = noteJumpTicks;
            this.layerJumpTicks = layerJumpTicks;
            this.instrument = instrument;
            this.key = key;
            this.pitch = pitch;
        }

        @Override
        public String toString() {
            return "Note{" +
                    "noteJumpTicks=" + noteJumpTicks +
                    ", layerJumpTicks=" + layerJumpTicks +
                    ", instrument=" + instrument +
                    ", key=" + key +
                    ", pitch=" + pitch +
                    '}';
        }
    }
}
