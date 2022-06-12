package com.connorcode.cornroot.misc;

public class MutInt {
    public int value;

    public MutInt(int value) {
        this.value = value;
    }

    public void increment(int i) {
        value += i;
    }
}
