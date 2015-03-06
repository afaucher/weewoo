package com.beanfarmergames.weewoo.audio;

public class FrequencyRange {
    public final float minimumFrequency;
    public final float maximumFrequency;
    public final float minimumMagnitude;
    public final float maximumMagnitude;
    public FrequencyRange(float minimumFrequency, float maximumFrequency, float minimumMagnitude, float maximumMagnitude) {
        super();
        this.minimumFrequency = minimumFrequency;
        this.maximumFrequency = maximumFrequency;
        this.minimumMagnitude = minimumMagnitude;
        this.maximumMagnitude = maximumMagnitude;
    }
}
