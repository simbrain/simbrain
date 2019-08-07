/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.util.projection;

import java.awt.*;
import java.util.HashMap;

/**
 * <b>DataPointColored</b> extends DataPoint adding functions for coloring
 * datapoints.
 *
 * @see DataColoringManager
 */
public class DataPointColored extends DataPoint  {

    /**
     * Color of this datapoint.
     */
    private Color color = Color.gray;

    /**
     * Default activation level.
     */
    public static final double DEFAULT_ACTIVATION = .15;

    /**
     * Number of times this datapoint has been visited or "activated"relative
     * to different source data points. Fan-in of activation counts.
     */
    private HashMap<DataPoint, Integer> activationCounts = new HashMap<>();

    /**
     * Probability of this point occurring relative to {@link OneStepPrediction}.
     * Used for Bayesian update of probabilities.
     */
    private HashMap<DataPoint, Double> probabilities = new HashMap<>();

    /**
     * An activation associated with this point that is used to determine the
     * color of the point. Frequency increments this when the point is active.
     * Decay decrements it at every time step but spikes the activate point to a
     * max value.
     *
     * @see DataColoringManager
     */
    private double activation = DEFAULT_ACTIVATION;

    /**
     * Default constructor for adding datasets.
     *
     * @param data
     */
    public DataPointColored(double[] data) {
        super(data);
    }

    public Color getColor() {
        return color;
    }

    /**
     * Use this to manually set color. ColoringMethod must be set to none.
     *
     * @param color the color to set
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Reset activation value.
     */
    public void resetActivation() {
        activation = DEFAULT_ACTIVATION;
    }

    /**
     * Set the color of this point based on the activation value.
     *
     * @param baseColor the base color whose saturation should be modified.
     */
    public void setColorBasedOnVal(float baseColor) {
        float saturation = clip((float) Math.abs(activation));
        setColor(Color.getHSBColor(baseColor, saturation, 1));
    }

    /**
     * Max out the activation of this point (for decay trail coloring).
     *
     * @param ceiling max value
     */
    public void spikeActivation(double ceiling) {
        activation = ceiling;
    }

    /**
     * Decrement the activation of this point (for decay trail coloring).
     *
     * @param base            lower bound of activation
     * @param decrementAmount amount by which to decrement
     */
    public void decrementActivation(double base, double decrementAmount) {
        activation -= decrementAmount;
        // in case of an overshoot
        if (activation < base) {
            activation = base;
        }
    }

    /**
     * Increment the activation of this point.
     *
     * @param incrementAmount amount to increment
     * @param ceiling         upper bound of activation
     */
    public void incrementActivation(double incrementAmount, double ceiling) {
        activation += incrementAmount;
        // System.out.println("activation:" + activation);
        // in case of an overshoot
        if (activation > ceiling) {
            activation = ceiling;
        }
    }

    public void setActivation(double activation) {
        this.activation = activation;
    }

    public double getActivation() {
        return activation;
    }

    /**
     * Check whether the specified saturation is valid or not.
     *
     * @param val the saturation value to check.
     * @return whether it is valid or not.
     */
    private float clip(final float val) {
        float tempval = val;

        if (val > 1) {
            tempval = 1;
        }

        if (val < 0) {
            tempval = 0;
        }

        return tempval;
    }

    // TODO: This is all temp just to get something working

    public double getProbability(DataPoint src) {
        Double prob = probabilities.get(src);
        return prob == null ? 0 : prob;
    }
    public void setProbability(DataPointColored src, double val) {
        probabilities.put(src, val);
    }

    public void incrementActivationCount(DataPoint src) {
        Integer count = activationCounts.get(src);
        if(count != null) {
            activationCounts.put(src, count + 1);
        } else {
            activationCounts.put(src, 1);
        }
    }

    public int getActivationCount(DataPoint src) {
        Integer count = activationCounts.get(src);
        return count == null ? 0 : count;
    }

    public void clear() {
        activationCounts.clear();
        probabilities.clear();
    }
}