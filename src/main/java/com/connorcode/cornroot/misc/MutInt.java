package com.connorcode.cornroot.misc;

public class MutInt {
    public int value;

    public MutInt(int value) {
        this.value = value;
    }

    public int increment(int i) {value+=i;return value;}
}
