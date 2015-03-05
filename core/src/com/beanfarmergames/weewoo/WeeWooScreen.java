package com.beanfarmergames.weewoo;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.jtransforms.fft.DoubleFFT_1D;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.AudioRecorder;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

public class WeeWooScreen implements Screen {
    private static final String TAG = WeeWooScreen.class.getName();

    private AudioRecorder recorder;
    private ShapeRenderer renderer = null;

    private static final int MIC_SAMPLING_RATE = 22050;
    private static final int AUDIO_SAMPLES = 4048;
    private short[] samples = new short[AUDIO_SAMPLES];
    private static final int FREQ_COUNT = 8;
    // Completely made this up, we should use a lower min but make it dynamic
    private static final int MIN_MAGNITUDE = 500000;
    private static final int SOUND_MAP_SAMPLE_INTERVAL_IN_SAMPLES = AUDIO_SAMPLES;
    
    private static FrequencyRange humanVoiceFF = new FrequencyRange(85, 255, MIN_MAGNITUDE, Float.MAX_VALUE);
    private static FrequencyRange humanVoice = new FrequencyRange(300, 3400, MIN_MAGNITUDE, Float.MAX_VALUE);
    private static FrequencyRange weeWoo = new FrequencyRange(85, 700, MIN_MAGNITUDE, Float.MAX_VALUE);
    

    //private SoundMap mapOne;
    private float playbackTimestamp = 0;
    private float globalClock = 0;
    private float CLOCK_MULTILPIER = 2.0f;
    private float BASE_FREQUENCY = 400;
    private float FREQUENCY_RANGE = 50;

    public WeeWooScreen() {
        recorder = Gdx.audio.newAudioRecorder(MIC_SAMPLING_RATE, true);
        renderer = new ShapeRenderer(500);
        Gdx.app.setLogLevel(Gdx.app.LOG_DEBUG);

        //mapOne = buildSoundMap("sounds/PoliceSiren2-SoundBible.com-2063505282.wav",
        //        SOUND_MAP_SAMPLE_INTERVAL_IN_SAMPLES);
        //mapOne.play();
        //Gdx.app.debug(TAG, mapOne.toString());

        
        
    }

    public static SoundMap buildSoundMap(String filename, int sampleInterval) {
        try {
            FileHandle weeWoo = Gdx.files.internal(filename);
            WavInputStream wav = new WavInputStream(weeWoo);
            short[] samples = wav.getSamples();

            TreeMap<Double, FrequencyDomain> timeToPeakFrequencies = new TreeMap<Double, FrequencyDomain>();
            for (int offset = 0; offset < samples.length; offset += sampleInterval) {
                double offsetSeconds = (double) offset / wav.sampleRate;

                short[] sampleWindow = Arrays.copyOfRange(samples, offset, offset + sampleInterval);
                FrequencyDomain sourceDomain = computeFrequencyDomainFromSamples(sampleWindow, wav.sampleRate);
                FrequencyDomain filteredDomain = computeFilteredFrequencyDomain(sourceDomain, humanVoice);

                Entry<Double, FrequencyDomain> last = timeToPeakFrequencies.floorEntry(offsetSeconds);
                if (last != null && last.getValue().fft.size() == 0 && filteredDomain.fft.size() == 0) {
                    // Skip continue silence
                    continue;
                }
                timeToPeakFrequencies.put(offsetSeconds, filteredDomain);
            }

            SoundMap map = new SoundMap(samples, humanVoice, wav, timeToPeakFrequencies, sampleInterval);

            return map;
        } catch (IOException e) {
            throw new RuntimeException(":P");
        }
    }

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
            maximumMagnitude = (float)Math.max(maximumMagnitude, mag);

            double frequency = sampleRate * (double) i / (double) mNumberOfFFTPoints;
            magnitudeToFrequency.put(magnitude[i], frequency);
        }
        float minimumFrequency = 0;//TODO: Determined by samples, rate & fft points?
        float maximumFrequency = sampleRate / 2.0f; //TODO, also limited by fft points?
        float minimumMagnitude = 0;
        FrequencyRange range = new FrequencyRange(minimumFrequency, maximumFrequency, minimumMagnitude, maximumMagnitude);

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
    public static FrequencyDomain computeFilteredFrequencyDomain(FrequencyDomain originalDomain, FrequencyRange filterRange) {
        TreeMap<Double, Double> filteredFft = new TreeMap<Double, Double>();
        
        FrequencyRange originalRange = originalDomain.range;
        Map.Entry<Double, Double> next = originalDomain.fft.lastEntry();
        for (int i = 0; i < FREQ_COUNT && next != null;) {
            double frequency = next.getValue();
            double mag = next.getKey();
            if (frequency >= filterRange.minimumFrequency 
                    && frequency <= filterRange.maximumFrequency 
                    && mag >= filterRange.minimumMagnitude
                    && mag <= filterRange.maximumMagnitude) {
                filteredFft.put(mag, frequency);
                i++;
            }

            next = originalDomain.fft.lowerEntry(next.getKey());
        }
        
        float minimumFrequency = Math.max(filterRange.minimumFrequency, originalRange.minimumFrequency);  
        float maximumFrequency = Math.min(filterRange.maximumFrequency, originalRange.maximumMagnitude);
        float minimumMagnitude = Math.max(filterRange.minimumMagnitude, originalRange.minimumMagnitude);
        float maximumMagnitude = Math.min(filterRange.maximumMagnitude, originalRange.maximumMagnitude);
        FrequencyRange range = new FrequencyRange(minimumFrequency, maximumFrequency, minimumMagnitude, maximumMagnitude);
        return new FrequencyDomain(originalDomain.sampleRate, originalDomain.sampleCount, filteredFft, range);
    }

    private static double[] shortArrayToDoubleArray(short[] samples) {
        double[] doubleSamples = new double[samples.length];
        for (int i = 0; i < samples.length; i++) {
            doubleSamples[i] = (double) samples[i];
        }
        return doubleSamples;
    }
    
    private class Histogram {
        public final int[] buckets;
        public final String name;
        
        public Histogram(int[] buckets, String name) {
            this.buckets = buckets;
            this.name = name;
        }
    }
    
    private float getWeightedAverageFrequency(Histogram h, FrequencyRange sourceRange) {
        float weightedAverage = 0; 
        float sum = 0;
        float rangePerBucket = (float)(sourceRange.maximumFrequency - sourceRange.minimumFrequency) / h.buckets.length;
        for (int i = 0; i < h.buckets.length; i++) {
            //Take the midpoint
            float bucketFrequency = sourceRange.minimumFrequency + rangePerBucket * (i + 0.5f);
            weightedAverage += h.buckets[i] * bucketFrequency;
            sum += h.buckets[i];
        }
        weightedAverage /= sum;
        return weightedAverage;
    }
    
    private Histogram bucketFrequencyBySampleCount(FrequencyDomain source, int numberBuckets, String name) {
        
        name = "bucketFrequencyBySampleCount: " + name;
        
        int[] buckets = new int[numberBuckets];
        
        for (Double frequency : source.fft.values()) {
            float ratio = getFrequencyRatioInRange(source.range, (float)frequency.floatValue());
            int bucket = Math.min((int)Math.floor(ratio * numberBuckets), numberBuckets - 1);
            buckets[bucket] += 1;
        }
        
        return new Histogram(buckets, name);
    }
    
    private Histogram bucketFrequencyByMaxMagnitude(FrequencyDomain source, int numberBuckets, String name) {
        
        name = "bucketFrequencyByMaxMagnitude: " + name;
        
        int[] buckets = new int[numberBuckets];
        
        for (Entry<Double,Double> e : source.fft.entrySet()) {
            float mag = (float)e.getKey().doubleValue();
            float frequency = (float)e.getValue().doubleValue();
            float ratio = getFrequencyRatioInRange(source.range, frequency);
            int bucket = Math.min((int)Math.floor(ratio * numberBuckets), numberBuckets - 1);
            buckets[bucket] = (int)Math.max(buckets[bucket], mag);
        }
        
        return new Histogram(buckets, name);
    }
    
    @Override
    public void render(float delta) {
        // Update
        
        globalClock += delta;
        /*if (playbackTimestamp > mapOne.getSeconds()) {
            playbackTimestamp = 0;
        } else {
            playbackTimestamp += delta * 0.1;
        }*/
        
        //FrequencyDomain target = mapOne.timeToPeakFrequencies.floorEntry((double)playbackTimestamp).getValue();
        
        recorder.read(samples, 0, samples.length);

        FrequencyDomain micDomain = computeFrequencyDomainFromSamples(samples, MIC_SAMPLING_RATE);
        FrequencyDomain filteredMicDomainHumanVoice = computeFilteredFrequencyDomain(micDomain, humanVoice);
        FrequencyDomain filteredMicDomainHumanVoiceFF = computeFilteredFrequencyDomain(micDomain, humanVoiceFF);
        
        FrequencyDomain weeWooDomain = computeFilteredFrequencyDomain(micDomain, weeWoo);
        
        
        int[] bucketCount = {5,10,20,40,80};
        Histogram[] hFreqBySampleCount = new Histogram[bucketCount.length];
        for (int i = 0; i < bucketCount.length; i++) {
            hFreqBySampleCount[i] = bucketFrequencyBySampleCount(weeWooDomain, bucketCount[i], "Freq Peak Count");
        }
        Histogram[] hFreqByMaxMag = new Histogram[bucketCount.length];
        for (int i = 0; i < bucketCount.length; i++) {
            hFreqByMaxMag[i] = bucketFrequencyByMaxMagnitude(weeWooDomain, bucketCount[i], "Max Mag");
        }
        
        float target = (float)Math.sin(globalClock * CLOCK_MULTILPIER) * this.FREQUENCY_RANGE + this.BASE_FREQUENCY;
        float actualHistogram = getWeightedAverageFrequency(hFreqBySampleCount[hFreqBySampleCount.length-1],weeWoo);
        float actualClosest = getClosest(weeWooDomain, target);
        
        /*
        update(delta)
            targetClock += delta;
            targetFrequency = Sin(targetClock) * range + base.
            for players
                accuracy = (freqencyTolerance - Clamp(0,freqencyTolerance,abs(targetFrequency - actualFrequency))) / freqencyTolerance
                player.points += accuracy * pointsMultipler
                player.speed = accuracy * playerSpeedMultiplier
                targetClock += accuracy * clockSpeedupMultiplier
         */
        
        //match(target, filteredMicDomainHumanVoiceFF);

        // Draw
        renderer.getTransformMatrix().idt();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        

        int x = Gdx.app.getGraphics().getWidth();
        int y = Gdx.app.getGraphics().getHeight();

        renderer.setColor(Color.RED);
        float offset = 0;
        // MIC
        
        offset = renderVolumePeak(renderer, samples, x, 10);
        renderer.translate(0, offset, 0);
        offset = renderSamples(renderer, samples, x, 100);
        renderer.translate(0, offset, 0);

        //offset = renderPeakFFT(renderer, filteredMicDomainHumanVoice, micDomain.range, x, 200);
        //offset = renderPeakFFT(renderer, filteredMicDomainHumanVoiceFF, micDomain.range, x, 200);
        renderer.setColor(Color.WHITE);
        offset = renderTarget(renderer, micDomain.range, target, x, 200);
        renderer.setColor(Color.RED);
        offset = renderPeakFFT(renderer, weeWooDomain, micDomain.range, x, 200);
        
        
        // renderer.translate(0, offset, 0);
        renderer.setColor(Color.LIGHT_GRAY);
        offset = renderFFT(renderer, micDomain, x, 200);
        renderer.translate(0, offset, 0);
        
        renderer.setColor(Color.WHITE);
        renderTarget(renderer, weeWoo, target, x, hFreqBySampleCount.length * 25);
        renderer.setColor(Color.GREEN);
        renderTarget(renderer, weeWoo, actualHistogram, x, hFreqBySampleCount.length * 25);
        renderer.setColor(Color.RED);
        renderTarget(renderer, weeWoo, actualClosest, x, hFreqBySampleCount.length * 25);
        renderer.setColor(Color.MAGENTA);
        for (int i = 0; i < hFreqBySampleCount.length; i++) {
            offset = renderHistogram(renderer, hFreqBySampleCount[i], true, x, 25);
            renderer.translate(0, offset, 0);
        }
        
        renderer.setColor(Color.MAROON);
        for (int i = 0; i < hFreqByMaxMag.length; i++) {
            offset = renderHistogram(renderer, hFreqByMaxMag[i], true, x, 25);
            renderer.translate(0, offset, 0);
        }
        
        
        // Draw Sound Map
        //renderer.setColor(Color.BLUE);
        //offset = renderSoundMap(renderer, mapOne, playbackTimestamp, x, 200);
        //renderer.translate(0, offset, 0);

    }
    
    private float getClosest(FrequencyDomain weeWooDomain, float target) {
        double closestFreq = 0;
        double closestDist = Double.MAX_VALUE;
        for (Double actual : weeWooDomain.fft.values()) {
            double dist = Math.abs(target-actual);
            if (dist < closestDist) {
                closestFreq = actual;
                closestDist = dist;
            }
        }
        return (float)closestFreq;
    }

    private static float renderHistogram(ShapeRenderer renderer, Histogram h, boolean horiz, float width, float height) {
        final float bucketWidth = width / h.buckets.length;
        final float bucketHeight = height / h.buckets.length;
        int totalCount = 0;
        int max = 0;
        for (int i = 0; i < h.buckets.length; i++) {
            totalCount += h.buckets[i];
            max = Math.max(h.buckets[i], max);
        }
        renderer.begin(ShapeType.Line);
        for (int i = 0; i < h.buckets.length; i++) {
            if (horiz) {
                renderer.rect(i * bucketWidth, 0, i * bucketWidth + bucketWidth, height);
            } else {
                renderer.rect(0, i * bucketHeight, width, i * bucketHeight + bucketHeight);
            }
        }
        renderer.end();
        if (totalCount > 0) {
            renderer.begin(ShapeType.Filled);
            for (int i = 0; i < h.buckets.length; i++) {
                float ratio = (float)h.buckets[i] / (float)totalCount;
                if (horiz) {
                    renderer.rect(i * bucketWidth, 0, bucketWidth, height * ratio);
                } else {
                    renderer.rect(0, i * bucketHeight, 
                            width * ratio, bucketHeight);
                }
            }
            renderer.end();
        }
        return height;
    }

    private static float renderFFT(ShapeRenderer renderer, FrequencyDomain fd, float width, float height) {
        renderer.begin(ShapeType.Line);

        double maximumAmplitude = 1000000;
        for (Map.Entry<Double, Double> next : fd.fft.entrySet()) {
            double frequency = next.getValue();
            double magnitude = Math.min(Math.abs(next.getKey()), maximumAmplitude);
            float x = getFrequencyRatioInRange(fd.range, (float)frequency) * width;
            float y = (float) (height * magnitude / maximumAmplitude);
            renderer.line(x, 0, x, y);
        }
        renderer.end();
        return height;
    }
    
    private static float getFrequencyRatioInRange(FrequencyRange range, float frequency) {
        float ratio = (frequency - range.minimumFrequency) / (range.maximumFrequency - range.minimumFrequency);
        ratio = Math.max(ratio, 0);
        ratio = Math.min(ratio, 1);
        return ratio;
    }
    
    private static float getMagnitudeRatioInRange(FrequencyRange range, float mag) {
        float ratio = (mag - range.minimumMagnitude) / (range.maximumMagnitude - range.minimumMagnitude);
        ratio = Math.max(ratio, 0);
        ratio = Math.min(ratio, 1);
        return ratio;
    }

    private static float renderPeakFFT(ShapeRenderer renderer, FrequencyDomain dataDomain, FrequencyRange displayRange, float width, float height) {
        // Render the clamping range
        renderer.begin(ShapeType.Line);
        FrequencyRange dataRange = dataDomain.range;
        float minWidthRatio = getFrequencyRatioInRange(displayRange, dataRange.minimumFrequency);
        float maxWidthRatio = getFrequencyRatioInRange(displayRange, dataRange.maximumFrequency);
        //TODO: Something is fishy with trying to draw the magnitude range
        //float minMagHeightRatio = getMagnitudeRatioInRange(displayRange, dataRange.minimumMagnitude);
        //float maxMagHeightRatio = getMagnitudeRatioInRange(displayRange, dataRange.maximumMagnitude);
        float minFreqX = minWidthRatio * width;
        float maxFreqX = maxWidthRatio * width;
        //float minMagY = minMagHeightRatio * height;
        //float maxMagY = maxMagHeightRatio * height;
        //renderer.line(minFreqX, minMagY, minFreqX, maxMagY);
        //renderer.line(maxFreqX, minMagY, maxFreqX, maxMagY);
        renderer.line(minFreqX, 0, minFreqX, height);
        renderer.line(maxFreqX, 0, maxFreqX, height);
        renderer.end();
        renderer.begin(ShapeType.Filled);
        TreeMap<Double, Double> peakFrequnecies = dataDomain.fft;
        for (Map.Entry<Double, Double> next : peakFrequnecies.entrySet()) {
            float frequency = (float) next.getValue().doubleValue();
            float frequencyRatio = getFrequencyRatioInRange(displayRange, frequency);
            float freqX = frequencyRatio * width;
            renderer.rectLine(freqX, 0, freqX, height, 5);
            next = peakFrequnecies.lowerEntry(next.getKey());
        }
        renderer.end();
        return height;
    }
    
    private static float renderTarget(ShapeRenderer renderer, FrequencyRange displayRange, float target, float width, float height) {
        // Render the clamping range
        renderer.begin(ShapeType.Line);
        float targetRatio = getFrequencyRatioInRange(displayRange, target);
        renderer.rectLine(targetRatio * width, 0, targetRatio * width, height, 5);
        renderer.end();
        return height;
    }

    private static float renderSamples(ShapeRenderer renderer, short[] samples, float width, float height) {
        renderer.begin(ShapeType.Line);
        renderer.box(0, 0, 0, width, height, 0);
        final float dynamicRange = (float) Short.MAX_VALUE + (float) Math.abs(Short.MIN_VALUE);
        final float midpointY = -(float) Short.MIN_VALUE * height / dynamicRange;
        for (int i = 0; i < samples.length; i++) {
            float sample = (float) samples[i] - (float) Short.MIN_VALUE;

            float sampleHeight = (float) height * sample / dynamicRange;
            float x = (float) i * width / samples.length;
            renderer.line(x, midpointY, x, sampleHeight);
        }

        renderer.end();
        return height;
    }

    private static float renderVolumePeak(ShapeRenderer renderer, short[] samples, float width, float height) {
        renderer.begin(ShapeType.Filled);
        short peak = Short.MIN_VALUE;

        for (int i = 0; i < samples.length; i++) {
            peak = (short) Math.max(peak, Math.abs(samples[i]));
        }
        float x = (float) peak * width / Short.MAX_VALUE;

        renderer.box(0, 0, 0, x, height, 0);

        renderer.end();
        return height;
    }

    private static float renderSoundMap(ShapeRenderer renderer, SoundMap map, float playbackTimestamp, float width, float height) {
        renderer.begin(ShapeType.Line);

        float seconds = (float) map.getSeconds();
        float intervalRatio = (float) map.getMapIntervalSeconds();
        renderer.box(0, 0, 0, width, height, 0);
        float playbackX = (float)(playbackTimestamp / map.getSeconds())* width;
        Color startColor = renderer.getColor();
        Color targetColor = Color.CYAN;
        renderer.setColor(targetColor);
        renderer.line(playbackX, 0, playbackX, height);
        renderer.setColor(startColor);

        for (Map.Entry<Double, FrequencyDomain> e : map.timeToPeakFrequencies.entrySet()) {
            float time = (float) e.getKey().doubleValue();
            float timeRatio = time / seconds;
            for (Map.Entry<Double, Double> f : e.getValue().fft.entrySet()) {
                float frequency = (float) f.getValue().doubleValue();
                float frequencyRatio = getFrequencyRatioInRange(map.range, frequency);

                float y = frequencyRatio * height;
                renderer.line(timeRatio * width, y, (timeRatio + intervalRatio) * width, y);
            
            }
        }
        renderer.end();
        return height;
    }

    @Override
    public void resize(int width, int height) {
        // TODO Auto-generated method stub

    }

    @Override
    public void show() {
        // TODO Auto-generated method stub

    }

    @Override
    public void hide() {
        // TODO Auto-generated method stub

    }

    @Override
    public void pause() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resume() {
        // TODO Auto-generated method stub

    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub

    }

}
