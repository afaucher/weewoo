package com.beanfarmergames.weewoo.audio;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.jtransforms.fft.DoubleFFT_1D;

public class AudioAnalyzer {

    public static FrequencyDomain calculateFFT(double[] signal, int sampleRate) {
        // Stolen from stack overflow
        // https://stackoverflow.com/questions/23867867/android-fundamental-frequency
        final int mNumberOfFFTPoints = 1024;

        double[] magnitude = new double[mNumberOfFFTPoints / 2];
        DoubleFFT_1D fft = new DoubleFFT_1D(mNumberOfFFTPoints);
        double[] fftData = new double[mNumberOfFFTPoints * 2];
        double max_magnitude = -1;

        for (int i = 0; i < mNumberOfFFTPoints; i++) {

            fftData[2 * i] = signal[i]; // da controllare
            fftData[2 * i + 1] = 0;

        }
        fft.complexForward(fftData);

        TreeMap<Double, Double> magnitudeToFrequency = new TreeMap<Double, Double>();

        float maximumMagnitude = 0;
        for (int i = 0; i < mNumberOfFFTPoints / 2; i++) {

            double a = (fftData[2 * i] * fftData[2 * i]) + (fftData[2 * i + 1] * fftData[2 * i + 1]);
            double mag = Math.sqrt(a);
            magnitude[i] = mag;
            maximumMagnitude = (float) Math.max(maximumMagnitude, mag);

            double frequency = sampleRate * (double) i / (double) mNumberOfFFTPoints;
            magnitudeToFrequency.put(magnitude[i], frequency);
        }
        float minimumFrequency = sampleRate / signal.length;// TODO: Determined by samples, rate & fft
                                   // points?
        float maximumFrequency = sampleRate / 2.0f; // TODO, also limited by fft
                                                    // points?
        float minimumMagnitude = 0;
        FrequencyRange range = new FrequencyRange(minimumFrequency, maximumFrequency, minimumMagnitude,
                maximumMagnitude);

        return new FrequencyDomain(sampleRate, signal.length, magnitudeToFrequency, range);
    }

    /**
     * Convert raw audio samples to frequency space.
     * 
     * @param samples
     * @return
     */
    public static FrequencyDomain computeFrequencyDomainFromSamples(short[] samples, int samplingRate) {
        double[] doubleSamples = shortArrayToDoubleArray(samples);
        return calculateFFT(doubleSamples, samplingRate);
    }

    /**
     * Convert raw audio samples to frequency space. Sort by magnitude and do
     * low/high pass for voice range.
     * 
     * @param samples
     * @return
     */
    public static FrequencyDomain computeFilteredFrequencyDomain(FrequencyDomain originalDomain,
            FrequencyRange filterRange, int peakCount) {
        TreeMap<Double, Double> filteredFft = new TreeMap<Double, Double>();

        FrequencyRange originalRange = originalDomain.range;
        Map.Entry<Double, Double> next = originalDomain.fft.lastEntry();
        for (int i = 0; i < peakCount && next != null;) {
            double frequency = next.getValue();
            double mag = next.getKey();
            if (frequency >= filterRange.minimumFrequency && frequency <= filterRange.maximumFrequency
                    && mag >= filterRange.minimumMagnitude && mag <= filterRange.maximumMagnitude) {
                filteredFft.put(mag, frequency);
                i++;
            }

            next = originalDomain.fft.lowerEntry(next.getKey());
        }

        float minimumFrequency = Math.max(filterRange.minimumFrequency, originalRange.minimumFrequency);
        float maximumFrequency = Math.min(filterRange.maximumFrequency, originalRange.maximumMagnitude);
        float minimumMagnitude = Math.max(filterRange.minimumMagnitude, originalRange.minimumMagnitude);
        float maximumMagnitude = Math.min(filterRange.maximumMagnitude, originalRange.maximumMagnitude);
        FrequencyRange range = new FrequencyRange(minimumFrequency, maximumFrequency, minimumMagnitude,
                maximumMagnitude);
        return new FrequencyDomain(originalDomain.sampleRate, originalDomain.sampleCount, filteredFft, range);
    }

    public static double[] shortArrayToDoubleArray(short[] samples) {
        double[] doubleSamples = new double[samples.length];
        for (int i = 0; i < samples.length; i++) {
            doubleSamples[i] = (double) samples[i];
        }
        return doubleSamples;
    }

    public static Histogram bucketFrequencyBySampleCount(FrequencyDomain source, int numberBuckets, String name) {

        name = "bucketFrequencyBySampleCount: " + name;

        int[] buckets = new int[numberBuckets];

        for (Double frequency : source.fft.values()) {
            float ratio = getFrequencyRatioInRange(source.range, (float) frequency.floatValue());
            int bucket = Math.min((int) Math.floor(ratio * numberBuckets), numberBuckets - 1);
            buckets[bucket] += 1;
        }

        return new Histogram(buckets, name);
    }

    public static Histogram bucketFrequencyByMaxMagnitude(FrequencyDomain source, int numberBuckets, String name) {

        name = "bucketFrequencyByMaxMagnitude: " + name;

        int[] buckets = new int[numberBuckets];

        for (Entry<Double, Double> e : source.fft.entrySet()) {
            float mag = (float) e.getKey().doubleValue();
            float frequency = (float) e.getValue().doubleValue();
            float ratio = getFrequencyRatioInRange(source.range, frequency);
            int bucket = Math.min((int) Math.floor(ratio * numberBuckets), numberBuckets - 1);
            buckets[bucket] = (int) Math.max(buckets[bucket], mag);
        }

        return new Histogram(buckets, name);
    }

    public static float getFrequencyRatioInRange(FrequencyRange range, float frequency) {
        float ratio = (frequency - range.minimumFrequency) / (range.maximumFrequency - range.minimumFrequency);
        ratio = Math.max(ratio, 0);
        ratio = Math.min(ratio, 1);
        return ratio;
    }

    public static float getMagnitudeRatioInRange(FrequencyRange range, float mag) {
        float ratio = (mag - range.minimumMagnitude) / (range.maximumMagnitude - range.minimumMagnitude);
        ratio = Math.max(ratio, 0);
        ratio = Math.min(ratio, 1);
        return ratio;
    }

    public static SoundMap buildSoundMap(short[] samples, int sampleRate, FrequencyRange range, int peakCount,
            int sampleInterval) {

        TreeMap<Double, FrequencyDomain> timeToPeakFrequencies = new TreeMap<Double, FrequencyDomain>();
        for (int offset = 0; offset < samples.length; offset += sampleInterval) {
            double offsetSeconds = (double) offset / sampleRate;

            short[] sampleWindow = Arrays.copyOfRange(samples, offset, offset + sampleInterval);
            FrequencyDomain sourceDomain = AudioAnalyzer.computeFrequencyDomainFromSamples(sampleWindow, sampleRate);
            FrequencyDomain filteredDomain = AudioAnalyzer.computeFilteredFrequencyDomain(sourceDomain, range,
                    sampleInterval);

            Entry<Double, FrequencyDomain> last = timeToPeakFrequencies.floorEntry(offsetSeconds);
            if (last != null && last.getValue().fft.size() == 0 && filteredDomain.fft.size() == 0) {
                // Skip continue silence
                continue;
            }
            timeToPeakFrequencies.put(offsetSeconds, filteredDomain);
        }

        SoundMap map = new SoundMap(samples, range, sampleRate, timeToPeakFrequencies, sampleInterval);

        return map;
    }
}
