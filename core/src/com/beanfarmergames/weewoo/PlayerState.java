package com.beanfarmergames.weewoo;

import com.beanfarmergames.weewoo.audio.FrequencyRange;
import com.beanfarmergames.weewoo.entities.Car;

public class PlayerState {
    private float actual;
    private float target;
    private float boostRatio;
    private FrequencyRange targetRange;
    private int playerNumber;
    private Car car;
    private int score;

    public float getActual() {
        return actual;
    }

    public void setActual(float actual) {
        this.actual = actual;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }

    public void setPlayerNumber(int playerNumber) {
        this.playerNumber = playerNumber;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public float getTarget() {
        return target;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public FrequencyRange getTargetRange() {
        return targetRange;
    }

    public void setTargetRange(FrequencyRange targetRange) {
        this.targetRange = targetRange;
    }

    public float getBoostRatio() {
        return boostRatio;
    }

    public void setBoostRatio(float boostRatio) {
        this.boostRatio = boostRatio;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
    
    
}