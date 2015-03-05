package com.beanfarmergames.weewoo;

import java.util.TreeMap;

public class FilteredFrequencyDomain {
    final TreeMap<Double, Double> filtered;
    final TreeMap<Double, Double> unfiltered;
    final double maximumFrequency;
    public FilteredFrequencyDomain(TreeMap<Double, Double> filtered, TreeMap<Double, Double> unfiltered, double maximumFrequency) {
        super();
        this.filtered = filtered;
        this.unfiltered = unfiltered;
        this.maximumFrequency = maximumFrequency;
    }
}