package com.beanfarmergames.weewoo;

import com.beanfarmergames.common.controls.AxisControl;

public class CarControl {
    private final AxisControl x = new AxisControl(0,1,-1);
    private final AxisControl y = new AxisControl(0,1,-1);
    
    public AxisControl getX() {
        return x;
    }
    public AxisControl getY() {
        return y;
    }
    
    
}
