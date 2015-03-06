package com.beanfarmergames.weewoo.audio;

public class AudioProfiles {
    // Completely made this up, we should use a lower min but make it dynamic
    private static final int MIN_MAGNITUDE = 500000;

    public static final FrequencyRange HUMAN_VOICE_FF = new FrequencyRange(85, 255, MIN_MAGNITUDE, Float.MAX_VALUE);
    public static final FrequencyRange HUMAN_VOICE = new FrequencyRange(300, 3400, MIN_MAGNITUDE, Float.MAX_VALUE);
    public static final FrequencyRange WEE_WOO = new FrequencyRange(85, 700, MIN_MAGNITUDE, Float.MAX_VALUE);

    public static final int PEAK_FREQ_COUNT = 8;
}
