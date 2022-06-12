package com.connorcode.cornroot;

import com.connorcode.cornroot.misc.MutInt;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

public class Song {
    String name;
    String author;
    float tempo;
    short length;

    Song(File file) throws Exception {
        byte[] bytes = Files.readAllBytes(file.toPath());
        MutInt i = new MutInt(0);

        // Version check
        assert readShort(i, bytes) == 0;

        // Get song length
        i.increment(2);
        this.length = readShort(i, bytes);

        // Get song name
        i.increment(4);
        this.name = readString(i, bytes);
        this.author = readString(i, bytes);

        // Get tempo
        skipStrings(i, bytes, 2);
        this.tempo = readShort(i, bytes) / 10f;
    }

    static short readShort(MutInt i, byte[] data) {
        int a = data[i.value] & 255;
        int b = data[i.value + 1] & 255;

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
}
