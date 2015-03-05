package com.beanfarmergames.common.entities;

public class SettableMeter implements Meter {
    private float value;
    private int max;
    
    public SettableMeter(int max) {
        this.max = max;
        this.value = this.max;
    }
    
    public SettableMeter(int max, float value) {
        this.max = max;
        this.value = value;
    }

    @Override
    public int getMax() {
        return max;
    }

    @Override
    public float getValue() {
        return value;
    }
    
    public void setValue(float value) {
        this.value = Math.max(0, Math.min(value,max));
    }
}
