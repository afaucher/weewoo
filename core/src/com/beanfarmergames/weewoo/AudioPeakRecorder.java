package com.beanfarmergames.weewoo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioRecorder;
import com.beanfarmergames.weewoo.audio.AudioAnalyzer;
import com.beanfarmergames.weewoo.audio.FrequencyDomain;
import com.beanfarmergames.weewoo.audio.FrequencyRange;

public class AudioPeakRecorder implements Runnable {

    private static final int MIC_SAMPLING_RATE = 22050;
    private static final int AUDIO_SAMPLES = 4048;
    private final short[] samples = new short[AUDIO_SAMPLES];

    private final AudioRecorder recorder;

    private final FrequencyRange range;
    private final int peakCount;

    // Synchronized on this
    private final Thread thread;
    private boolean shouldRun = false;
    private FrequencyDomain lastMicDomain;
    private FrequencyDomain lastFilteredDomain;
    // End Synchronized

    public AudioPeakRecorder(FrequencyRange range, int peakCount) {
        this.range = range;
        this.peakCount = peakCount;

        recorder = Gdx.audio.newAudioRecorder(MIC_SAMPLING_RATE, true);
        thread = new Thread(this);
    }

    public void start() {
        synchronized (this) {
            if (!thread.isAlive()) {
                shouldRun = true;
                thread.start();
            }
        }
    }
    
    public void end() {
        synchronized (this) {
            shouldRun = false;
        }
    }

    @Override
    public void run() {
        while (true) {

            recorder.read(samples, 0, samples.length);

            FrequencyDomain micDomain = AudioAnalyzer.computeFrequencyDomainFromSamples(samples, MIC_SAMPLING_RATE);

            FrequencyDomain filteredDomain = AudioAnalyzer.computeFilteredFrequencyDomain(micDomain, range, peakCount);
            synchronized (this) {
                this.lastMicDomain = micDomain;
                this.lastFilteredDomain = filteredDomain;
                if (!shouldRun) {
                    return;
                }
            }
        }
    }
    
    public FrequencyDomain getLastFilteredDomain() {
        synchronized (this) {
            return this.lastFilteredDomain;
        }
    }
    
    public FrequencyDomain getLastMicDomain() {
        synchronized (this) {
            return this.lastMicDomain;
        }
    }
}
