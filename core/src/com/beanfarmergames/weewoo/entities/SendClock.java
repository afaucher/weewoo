package com.beanfarmergames.weewoo.entities;

public class SendClock {
    private long globalClockMiliseconds = 0;
    private long lastSentMiliseconds = 0;
    private final long sendFrequencyMiliseconds;
    
    public SendClock(long sendFrequencyMiliseconds) {
        this.sendFrequencyMiliseconds = sendFrequencyMiliseconds;
    }
    
    public boolean shouldSend(long deltaMiliseconds) {
        globalClockMiliseconds += deltaMiliseconds;
        return (globalClockMiliseconds > sendFrequencyMiliseconds + lastSentMiliseconds);
    }
}