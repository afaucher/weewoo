package com.beanfarmergames.weewoo;

import java.io.IOException;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.AudioRecorder;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.beanfarmergames.weewoo.audio.AudioAnalyzer;
import com.beanfarmergames.weewoo.audio.AudioRenderer;
import com.beanfarmergames.weewoo.audio.FrequencyDomain;
import com.beanfarmergames.weewoo.audio.FrequencyRange;
import com.beanfarmergames.weewoo.audio.Histogram;
import com.beanfarmergames.weewoo.audio.SoundMap;
import com.beanfarmergames.weewoo.audio.WavInputStream;

public class WeeWooScreen implements Screen {
    private static final String TAG = WeeWooScreen.class.getName();

    
    private ShapeRenderer renderer = null;

    
    private static final int FREQ_COUNT = 8;
    // Completely made this up, we should use a lower min but make it dynamic
    private static final int MIN_MAGNITUDE = 500000;

    private static FrequencyRange humanVoiceFF = new FrequencyRange(85, 255, MIN_MAGNITUDE, Float.MAX_VALUE);
    private static FrequencyRange humanVoice = new FrequencyRange(300, 3400, MIN_MAGNITUDE, Float.MAX_VALUE);
    private static FrequencyRange weeWoo = new FrequencyRange(85, 700, MIN_MAGNITUDE, Float.MAX_VALUE);

    // private SoundMap mapOne;
    private float playbackTimestamp = 0;
    private float globalClock = 0;
    private float CLOCK_MULTILPIER = 2.0f;
    private float BASE_FREQUENCY = 400;
    private float FREQUENCY_RANGE = 50;
    
    private AudioPeakRecorder peakRecorder;

    public WeeWooScreen() {
        
        renderer = new ShapeRenderer(500);
        Gdx.app.setLogLevel(Application.LOG_DEBUG);
        
        peakRecorder = new AudioPeakRecorder(weeWoo, FREQ_COUNT);
        
        peakRecorder.start();
    }

    public static SoundMap buildSoundMapFromWav(String filename, int sampleInterval, int peakCount) {
        try {
            FileHandle weeWoo = Gdx.files.internal(filename);
            WavInputStream wav = new WavInputStream(weeWoo);
            short[] samples = wav.getSamples();

            return AudioAnalyzer.buildSoundMap(samples, wav.getSampleRate(), humanVoice, peakCount, sampleInterval);

        } catch (IOException e) {
            throw new RuntimeException(":P");
        }
    }

    private float getWeightedAverageFrequency(Histogram h, FrequencyRange sourceRange) {
        float weightedAverage = 0;
        float sum = 0;
        float rangePerBucket = (float) (sourceRange.maximumFrequency - sourceRange.minimumFrequency) / h.buckets.length;
        for (int i = 0; i < h.buckets.length; i++) {
            // Take the midpoint
            float bucketFrequency = sourceRange.minimumFrequency + rangePerBucket * (i + 0.5f);
            weightedAverage += h.buckets[i] * bucketFrequency;
            sum += h.buckets[i];
        }
        weightedAverage /= sum;
        return weightedAverage;
    }

    @Override
    public void render(float delta) {
        // Update

        globalClock += delta;
        


        /*
         * update(delta) targetClock += delta; targetFrequency =
         * Sin(targetClock) * range + base. for players accuracy =
         * (freqencyTolerance - Clamp(0,freqencyTolerance,abs(targetFrequency -
         * actualFrequency))) / freqencyTolerance player.points += accuracy *
         * pointsMultipler player.speed = accuracy * playerSpeedMultiplier
         * targetClock += accuracy * clockSpeedupMultiplier
         */

        // match(target, filteredMicDomainHumanVoiceFF);

        // Draw
        renderer.getTransformMatrix().idt();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        //TODO: Get these together, this is not atomic
        FrequencyDomain micDomain = peakRecorder.getLastMicDomain();
        FrequencyDomain weeWooDomain = peakRecorder.getLastFilteredDomain();
        
        if (micDomain == null || weeWooDomain == null) {
            return;
        }

        int[] bucketCount = { 5, 10, 20, 40, 80 };
        Histogram[] hFreqBySampleCount = new Histogram[bucketCount.length];
        for (int i = 0; i < bucketCount.length; i++) {
            hFreqBySampleCount[i] = AudioAnalyzer.bucketFrequencyBySampleCount(weeWooDomain, bucketCount[i],
                    "Freq Peak Count");
        }
        Histogram[] hFreqByMaxMag = new Histogram[bucketCount.length];
        for (int i = 0; i < bucketCount.length; i++) {
            hFreqByMaxMag[i] = AudioAnalyzer.bucketFrequencyByMaxMagnitude(weeWooDomain, bucketCount[i], "Max Mag");
        }

        float target = (float) Math.sin(globalClock * CLOCK_MULTILPIER) * this.FREQUENCY_RANGE + this.BASE_FREQUENCY;
        float actualHistogram = getWeightedAverageFrequency(hFreqBySampleCount[hFreqBySampleCount.length - 1], weeWoo);
        float actualClosest = getClosest(weeWooDomain, target);

        int x = Gdx.app.getGraphics().getWidth();
        int y = Gdx.app.getGraphics().getHeight();

        renderer.setColor(Color.RED);
        float offset = 0;
        // MIC

        //offset = AudioRenderer.renderVolumePeak(renderer, samples, x, 10);
        //renderer.translate(0, offset, 0);
        //offset = AudioRenderer.renderSamples(renderer, samples, x, 100);
        //renderer.translate(0, offset, 0);

        // offset = renderPeakFFT(renderer, filteredMicDomainHumanVoice,
        // micDomain.range, x, 200);
        // offset = renderPeakFFT(renderer, filteredMicDomainHumanVoiceFF,
        // micDomain.range, x, 200);
        renderer.setColor(Color.WHITE);
        offset = AudioRenderer.renderTarget(renderer, micDomain.getRange(), target, x, 200);
        renderer.setColor(Color.RED);
        offset = AudioRenderer.renderPeakFFT(renderer, weeWooDomain, micDomain.getRange(), x, 200);

        // renderer.translate(0, offset, 0);
        renderer.setColor(Color.LIGHT_GRAY);
        offset = AudioRenderer.renderFFT(renderer, micDomain, x, 200);
        renderer.translate(0, offset, 0);

        renderer.setColor(Color.WHITE);
        AudioRenderer.renderTarget(renderer, weeWoo, target, x, hFreqBySampleCount.length * 25);
        renderer.setColor(Color.GREEN);
        AudioRenderer.renderTarget(renderer, weeWoo, actualHistogram, x, hFreqBySampleCount.length * 25);
        renderer.setColor(Color.RED);
        AudioRenderer.renderTarget(renderer, weeWoo, actualClosest, x, hFreqBySampleCount.length * 25);
        renderer.setColor(Color.MAGENTA);
        for (int i = 0; i < hFreqBySampleCount.length; i++) {
            offset = AudioRenderer.renderHistogram(renderer, hFreqBySampleCount[i], true, x, 25);
            renderer.translate(0, offset, 0);
        }

        renderer.setColor(Color.MAROON);
        for (int i = 0; i < hFreqByMaxMag.length; i++) {
            offset = AudioRenderer.renderHistogram(renderer, hFreqByMaxMag[i], true, x, 25);
            renderer.translate(0, offset, 0);
        }

        // Draw Sound Map
        // renderer.setColor(Color.BLUE);
        // offset = renderSoundMap(renderer, mapOne, playbackTimestamp, x, 200);
        // renderer.translate(0, offset, 0);

    }

    private float getClosest(FrequencyDomain weeWooDomain, float target) {
        double closestFreq = 0;
        double closestDist = Double.MAX_VALUE;
        for (Double actual : weeWooDomain.getFft().values()) {
            double dist = Math.abs(target - actual);
            if (dist < closestDist) {
                closestFreq = actual;
                closestDist = dist;
            }
        }
        return (float) closestFreq;
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
