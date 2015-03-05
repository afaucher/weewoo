package com.beanfarmergames.weewoo;

import java.util.TreeMap;

public class FrequencyDomain {
    final int sampleRate;
    final int sampleCount;
    final TreeMap<Double, Double> fft;
    final FrequencyRange range;
    public FrequencyDomain(int sampleRate, int sampleCount, TreeMap<Double, Double> fft, FrequencyRange range) {
        super();
        this.sampleRate = sampleRate;
        this.sampleCount = sampleCount;
        this.fft = fft;
        this.range = range;
    }
}