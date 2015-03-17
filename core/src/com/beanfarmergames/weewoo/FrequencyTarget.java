package com.beanfarmergames.weewoo;

import com.beanfarmergames.common.callbacks.UpdateCallback;
import com.beanfarmergames.weewoo.audio.FrequencyRange;

public class FrequencyTarget implements UpdateCallback {
    
    private static final float CLOCK_MULTILPIER = 2.0f;
    private static final float BASE_FREQUENCY = 400;
    private static final float FREQUENCY_RANGE = 50;
    
    private float targetClock = 0;

    public float getTarget() {
        float target = (float) Math.sin(targetClock * CLOCK_MULTILPIER) * this.FREQUENCY_RANGE + this.BASE_FREQUENCY;
        return target;
    }

    @Override
    public void updateCallback(long miliseconds) {
        targetClock+= miliseconds / 1000.0f;
    }
    
    public FrequencyRange getTargetRange() {
        return null;
    }
}
