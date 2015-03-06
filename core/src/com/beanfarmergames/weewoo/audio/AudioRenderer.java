package com.beanfarmergames.weewoo.audio;

import java.util.Map;
import java.util.TreeMap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

public class AudioRenderer {

    public static float renderHistogram(ShapeRenderer renderer, Histogram h, boolean horiz, float width, float height) {
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
                float ratio = (float) h.buckets[i] / (float) totalCount;
                if (horiz) {
                    renderer.rect(i * bucketWidth, 0, bucketWidth, height * ratio);
                } else {
                    renderer.rect(0, i * bucketHeight, width * ratio, bucketHeight);
                }
            }
            renderer.end();
        }
        return height;
    }

    public static float renderFFT(ShapeRenderer renderer, FrequencyDomain fd, float width, float height) {
        renderer.begin(ShapeType.Line);

        double maximumAmplitude = 1000000;
        for (Map.Entry<Double, Double> next : fd.fft.entrySet()) {
            double frequency = next.getValue();
            double magnitude = Math.min(Math.abs(next.getKey()), maximumAmplitude);
            float x = AudioAnalyzer.getFrequencyRatioInRange(fd.range, (float) frequency) * width;
            float y = (float) (height * magnitude / maximumAmplitude);
            renderer.line(x, 0, x, y);
        }
        renderer.end();
        return height;
    }

    public static float renderPeakFFT(ShapeRenderer renderer, FrequencyDomain dataDomain, FrequencyRange displayRange,
            float width, float height) {
        // Render the clamping range
        renderer.begin(ShapeType.Line);
        FrequencyRange dataRange = dataDomain.range;
        float minWidthRatio = AudioAnalyzer.getFrequencyRatioInRange(displayRange, dataRange.minimumFrequency);
        float maxWidthRatio = AudioAnalyzer.getFrequencyRatioInRange(displayRange, dataRange.maximumFrequency);
        // TODO: Something is fishy with trying to draw the magnitude range
        // float minMagHeightRatio = getMagnitudeRatioInRange(displayRange,
        // dataRange.minimumMagnitude);
        // float maxMagHeightRatio = getMagnitudeRatioInRange(displayRange,
        // dataRange.maximumMagnitude);
        float minFreqX = minWidthRatio * width;
        float maxFreqX = maxWidthRatio * width;
        // float minMagY = minMagHeightRatio * height;
        // float maxMagY = maxMagHeightRatio * height;
        // renderer.line(minFreqX, minMagY, minFreqX, maxMagY);
        // renderer.line(maxFreqX, minMagY, maxFreqX, maxMagY);
        renderer.line(minFreqX, 0, minFreqX, height);
        renderer.line(maxFreqX, 0, maxFreqX, height);
        renderer.end();
        renderer.begin(ShapeType.Filled);
        TreeMap<Double, Double> peakFrequnecies = dataDomain.fft;
        for (Map.Entry<Double, Double> next : peakFrequnecies.entrySet()) {
            float frequency = (float) next.getValue().doubleValue();
            float frequencyRatio = AudioAnalyzer.getFrequencyRatioInRange(displayRange, frequency);
            float freqX = frequencyRatio * width;
            renderer.rectLine(freqX, 0, freqX, height, 5);
            next = peakFrequnecies.lowerEntry(next.getKey());
        }
        renderer.end();
        return height;
    }

    public static float renderTarget(ShapeRenderer renderer, FrequencyRange displayRange, float target, float width,
            float height) {
        // Render the clamping range
        renderer.begin(ShapeType.Line);
        float targetRatio = AudioAnalyzer.getFrequencyRatioInRange(displayRange, target);
        renderer.rectLine(targetRatio * width, 0, targetRatio * width, height, 5);
        renderer.end();
        return height;
    }

    public static float renderSamples(ShapeRenderer renderer, short[] samples, float width, float height) {
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

    public static float renderVolumePeak(ShapeRenderer renderer, short[] samples, float width, float height) {
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

    public static float renderSoundMap(ShapeRenderer renderer, SoundMap map, float playbackTimestamp, float width,
            float height) {
        renderer.begin(ShapeType.Line);

        float seconds = (float) map.getSeconds();
        float intervalRatio = (float) map.getMapIntervalSeconds();
        renderer.box(0, 0, 0, width, height, 0);
        float playbackX = (float) (playbackTimestamp / map.getSeconds()) * width;
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
                float frequencyRatio = AudioAnalyzer.getFrequencyRatioInRange(map.range, frequency);

                float y = frequencyRatio * height;
                renderer.line(timeRatio * width, y, (timeRatio + intervalRatio) * width, y);

            }
        }
        renderer.end();
        return height;
    }
}
