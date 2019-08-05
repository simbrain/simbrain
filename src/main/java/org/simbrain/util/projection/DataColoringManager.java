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

import org.simbrain.util.Utils;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Manage the coloring of datapoints.
 */
public class DataColoringManager {

    /**
     * List of methods for coloring points.
     */
    public enum ColoringMethod {

        /**
         * No special coloring. All points colored the base color.
         */
        None,

        /**
         * The current point gets the greatest saturation and it trails off in a
         * decaying saturation after. Decays based on decay increment.
         */
        DecayTrail,

        /**
         * As a point is visited more its activation and hence the saturation of
         * its color representation increases.
         */
        Frequency,

        /**
         * Use the {@link OneStepPrediction} for datapoint coloring
         */
        Bayesian
    }

    /**
     * How to color points.
     */
    private ColoringMethod coloringMethod = ColoringMethod.None;

    /**
     * The "hot color" to be used for the current point.
     */
    private Color hotColor = new Color (8,0,255);

    /**
     * The base color to be used for all points besides the current point.
     */
    private Color baseColor = new Color(232, 232, 232);

    /**
     * Base color of states predicted by the Bayesian method.
     */
    private Color predictedColor = new Color(22, 219, 45);


    /**
     * Toggle for hot point coloring mode. The current point is colored hotColor
     * when this is true.
     */
    private boolean hotPointMode = true;

    /**
     * The lower bound for "activation" of a point in frequency and decay
     * trail.
     */
    private double floor = DataPointColored.DEFAULT_ACTIVATION;

    /**
     * The upper bound for "activation" of a point in frequency and decay trail.
     * Max is 1
     */
    private double ceiling = 1;

    /**
     * How much to increment activation at each time step (used in frequency
     * coloring method).
     */
    private double incrementAmount = .1;

    /**
     * How much to decrement activation at each time step (used in decay trail
     * coloring method).
     */
    private double decrementAmount = .02;

    /**
     * Reference to parent projector.
     */
    private final Projector projector;

    /**
     * Construct a data coloring manager.
     *
     * @param projector
     */
    public DataColoringManager(Projector projector) {
        this.projector = projector;
    }

    /**
     * Update the colors of all the datapoints in a dataset.
     *
     * @param data the dataset whose points should be colored.
     */
    public void updateDataPointColors(Dataset data) {
        for (int i = 0; i < data.getNumPoints(); i++) {
            DataPointColored point = (DataPointColored) data.getPoint(i);
            updateColorOfPoint(point);
        }
    }

    /**
     * Update the color of the specified point in the dataset.
     *
     * @param point the point to update.
     */
    private void updateColorOfPoint(DataPointColored point) {
        if (coloringMethod == ColoringMethod.None) {
            setHotPoint(point);
        } else if (coloringMethod == ColoringMethod.DecayTrail) {
            if (point == projector.getCurrentPoint()) {
                setHotPoint(point);
                point.spikeActivation(ceiling);
            } else {
                point.decrementActivation(floor, decrementAmount);
                point.setColorBasedOnVal(Utils.colorToFloat(baseColor));
            }
        } else if (coloringMethod == ColoringMethod.Frequency) {
            if (point == projector.getCurrentPoint()) {
                setHotPoint(point);
                point.incrementActivation(incrementAmount, ceiling);
            } else {
                point.setColorBasedOnVal(Utils.colorToFloat(baseColor));
            }
        } else if (coloringMethod == ColoringMethod.Bayesian) {
            ; // Should not occur.  See UpdateBayes.
        }

    }

    /**
     * Update entire datset using Bayesian method
     */
    public void updateBayes() {

        Dataset data = projector.getUpstairs();
        // Color in the base color first
        for (int i = 0; i < data.getNumPoints(); i++) {
            DataPointColored point = (DataPointColored) data.getPoint(i);
            point.setColor(baseColor);
        }

        // TODO: May not need this later. Just use probabilities
        // Or give this some other name, like simple-halo

        // Color in predicted colors relative to current point
        OneStepPrediction pred = projector.getPredictor();
        HashSet<DataPoint> targets = pred.getTargets(projector.getCurrentPoint());
        if (targets != null) {
            for (DataPoint point : targets) {
                ((DataPointColored) point).setColor(predictedColor);
            }
        }
        setHotPoint((DataPointColored) projector.getCurrentPoint());
    }

    /**
     * Set the color of the "hot point"
     */
    private void setHotPoint(DataPointColored point) {
        if (point == projector.getCurrentPoint() && hotPointMode == true) {
            point.setColor(hotColor);
        } else {
            point.setColor(baseColor);
        }
    }

    /**
     * Get the coloring method as a string.
     *
     * @return coloringMethodString the coloring method
     */
    public String getColoringMethodString() {
        if (coloringMethod == ColoringMethod.DecayTrail) {
            return "DecayTrail";
        } else if (coloringMethod == ColoringMethod.Frequency) {
            return "Frequency";
        } else if (coloringMethod == ColoringMethod.Bayesian) {
            return "Bayesian";
        } else {
            return "None";
        }
    }

    /**
     * Set the coloring method.
     *
     * @param selectedMethod
     */
    public void setColoringMethod(String selectedMethod) {
        if (selectedMethod == "None") {
            coloringMethod = ColoringMethod.None;
        } else if (selectedMethod == "DecayTrail") {
            coloringMethod = ColoringMethod.DecayTrail;
        } else if (selectedMethod == "Frequency") {
            coloringMethod = ColoringMethod.Frequency;
        } else if (selectedMethod == "Bayesian") {
            coloringMethod = ColoringMethod.Bayesian;
        }
    }

    public ColoringMethod getColoringMethod() {
        return coloringMethod;
    }

    public Color getHotColor() {
        return hotColor;
    }

    public Color getBaseColor() {
        return baseColor;
    }

    public double getFloor() {
        return floor;
    }

    public double getCeiling() {
        return ceiling;
    }

    public double getIncrementAmount() {
        return incrementAmount;
    }

    public double getDecrementAmount() {
        return decrementAmount;
    }

    public void setHotColor(Color hotColor) {
        this.hotColor = hotColor;
    }

    public void setBaseColor(Color baseColor) {
        this.baseColor = baseColor;
    }

    public void setFloor(double floor) {
        this.floor = floor;
    }

    public void setCeiling(double ceiling) {
        this.ceiling = ceiling;
    }

    public void setIncrementAmount(double incrementAmount) {
        this.incrementAmount = incrementAmount;
    }

    public void setDecrementAmount(double decrementAmount) {
        this.decrementAmount = decrementAmount;
    }

    /**
     * Set hot point coloring mode to on (true) or off (false).
     *
     * @param hotPointMode
     */
    public void setHotPointMode(boolean hotPointMode) {
        this.hotPointMode = hotPointMode;
    }

    public boolean getHotPointMode() {
        return hotPointMode;
    }
}
