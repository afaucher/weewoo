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
import com.beanfarmergames.weewoo.audio.AudioProfiles;
import com.beanfarmergames.weewoo.audio.AudioRenderer;
import com.beanfarmergames.weewoo.audio.FrequencyDomain;
import com.beanfarmergames.weewoo.audio.FrequencyRange;
import com.beanfarmergames.weewoo.audio.Histogram;
import com.beanfarmergames.weewoo.audio.SoundMap;
import com.beanfarmergames.weewoo.audio.WavInputStream;

public class WeeWooScreen implements Screen {
    private static final String TAG = WeeWooScreen.class.getName();

    private ShapeRenderer renderer = null;

    // private SoundMap mapOne;
    private float playbackTimestamp = 0;

    private AudioPeakRecorder peakRecorder;

    public WeeWooScreen() {

        renderer = new ShapeRenderer(500);
        Gdx.app.setLogLevel(Application.LOG_DEBUG);

        peakRecorder = new AudioPeakRecorder(AudioProfiles.WEE_WOO, AudioProfiles.PEAK_FREQ_COUNT);

        peakRecorder.start();
    }

    public static SoundMap buildSoundMapFromWav(String filename, int sampleInterval, int peakCount) {
        try {
            FileHandle weeWoo = Gdx.files.internal(filename);
            WavInputStream wav = new WavInputStream(weeWoo);
            short[] samples = wav.getSamples();

            return AudioAnalyzer.buildSoundMap(samples, wav.getSampleRate(), AudioProfiles.HUMAN_VOICE, peakCount,
                    sampleInterval);

        } catch (IOException e) {
            throw new RuntimeException(":P");
        }
    }

    @Override
    public void render(float delta) {
        // Update


        // Draw
        renderer.getTransformMatrix().idt();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // TODO: Get these together, this is not atomic
        FrequencyDomain micDomain = peakRecorder.getLastMicDomain();
        FrequencyDomain weeWooDomain = peakRecorder.getLastFilteredDomain();
        FrequencyRange range = peakRecorder.getRange();

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

        float target = 0;//(float) Math.sin(globalClock * CLOCK_MULTILPIER) * this.FREQUENCY_RANGE + this.BASE_FREQUENCY;
        
        float actualClosest = AudioAnalyzer.getClosestFreqToTarget(weeWooDomain, target);

        int x = Gdx.app.getGraphics().getWidth();
        int y = Gdx.app.getGraphics().getHeight();

        renderer.setColor(Color.RED);
        float offset = 0;
        // MIC

        // offset = AudioRenderer.renderVolumePeak(renderer, samples, x, 10);
        // renderer.translate(0, offset, 0);
        // offset = AudioRenderer.renderSamples(renderer, samples, x, 100);
        // renderer.translate(0, offset, 0);

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
        AudioRenderer.renderTarget(renderer, range, target, x, hFreqBySampleCount.length * 25);
        renderer.setColor(Color.RED);
        AudioRenderer.renderTarget(renderer, range, actualClosest, x, hFreqBySampleCount.length * 25);
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
