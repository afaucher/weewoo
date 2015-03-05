package com.beanfarmergames.common.controls;

public class ToggleControl implements Control {

    private boolean enabled;

    public ToggleControl(boolean enabled) {
        super();
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void toggle() {
        this.enabled = !enabled;
    }

    @Override
    public ControlType getControlType() {
        return ControlType.TOGGLE;
    }

}
