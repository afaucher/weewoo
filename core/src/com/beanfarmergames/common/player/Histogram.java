package com.beanfarmergames.common.player;

class Histogram {
    public final int[] buckets;
    public final String name;
    
    public Histogram(int[] buckets, String name) {
        this.buckets = buckets;
        this.name = name;
    }
}