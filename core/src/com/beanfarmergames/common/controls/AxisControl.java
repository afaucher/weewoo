package com.beanfarmergames.common.controls;

public class AxisControl implements Control {
    float x = 0;
    float maxValue = 1;
    float minValue = -1;

    public AxisControl(float x, float maxValue, float minValue) {
        super();
        this.x = x;
        this.maxValue = maxValue;
        this.minValue = minValue;

        setX(x);
    }

    public AxisControl() {
        super();
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = Math.max(minValue, Math.min(maxValue, x));
    }

    public float getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(float maxValue) {
        this.maxValue = maxValue;
        setX(x);
    }

    public float getMinValue() {
        return minValue;
    }

    public void setMinValue(float minValue) {
        this.minValue = minValue;
        setX(x);
    }

    @Override
    public ControlType getControlType() {
        return ControlType.AXIS;
    }

}
