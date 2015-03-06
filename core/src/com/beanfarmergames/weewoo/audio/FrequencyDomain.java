package com.beanfarmergames.weewoo.audio;

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

    public int getSampleRate() {
        return sampleRate;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public TreeMap<Double, Double> getFft() {
        return fft;
    }

    public FrequencyRange getRange() {
        return range;
    }

}