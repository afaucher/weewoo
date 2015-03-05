package com.beanfarmergames.weewoo;

import java.util.Map;
import java.util.TreeMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;

public class SoundMap {
    public final short[] samples;
    public final WavInputStream wav;
    public final int sampleInterval;
    public final FrequencyRange range;
    /**
     * Time > (Magnitude > Frequency)
     * 
     * Samples: [Sample Range 1][Sample Range 2][Sample Range 3] T1 T2 T3
     * 
     * 
     */
    public final TreeMap<Double, FrequencyDomain> timeToPeakFrequencies;

    public SoundMap(short[] samples, FrequencyRange range, WavInputStream wav, TreeMap<Double, FrequencyDomain> timeToPeakFrequencies, int sampleInterval) {
        super();
        this.samples = samples;
        this.wav = wav;
        this.timeToPeakFrequencies = timeToPeakFrequencies;
        this.sampleInterval = sampleInterval;
        this.range = range;
    }

    public void play() {
        AudioDevice a = Gdx.audio.newAudioDevice(wav.sampleRate, true);
        a.writeSamples(samples, 0, samples.length);
        a.dispose();
    }
    
    public double getSeconds() {
        return (double)samples.length / wav.sampleRate;
    }
    
    public double getMapIntervalSeconds() {
        return (double)sampleInterval / wav.sampleRate;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("{");
        builder.append("\"Map\": ");
        builder.append("[");
        boolean firstTime = true;
        for (Map.Entry<Double, FrequencyDomain> e : timeToPeakFrequencies.entrySet()) {
            if (firstTime) {
                firstTime = false;
            } else {
                builder.append(",");
            }
            builder.append("{");
            builder.append("\"Time\": " + e.getKey() + ",");
            builder.append("\"Peak\": ");
            builder.append("[");
            boolean firstFreq = true;
            for (Map.Entry<Double, Double> f : e.getValue().fft.entrySet()) {
                if (firstFreq) {
                    firstFreq = false;
                } else {
                    builder.append(",");
                }
                builder.append("{");
                builder.append("\"Mag\": " + f.getKey() + ",");
                builder.append("\"Freq\": " + f.getValue() + "");
                builder.append("}");
            }
            builder.append("]");
            builder.append("}");
        }
        builder.append("]");
        builder.append("}");

        return builder.toString();
    }
}