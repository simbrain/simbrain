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
import org.simbrain.util.propertyeditor.DisplayOrder;

import java.awt.*;

/**
 * Manage the coloring of datapoints.
 */
public class DataColoringManager {

    /**
     * List of methods for coloring points.
     */
    private enum ColoringMethod {

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
        Frequency
    }

    /**
     * How to color points.
     */
    private ColoringMethod coloringMethod = ColoringMethod.None;

    /**
     * The "hot color" to be used for the current point.
     */
    private Color hotColor = Color.red;

    /**
     * The base color to be used for all points besides the current point.
     */
    private Color baseColor = Color.green;

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
            if (point == projector.getCurrentPoint() && hotPointMode == true) {
                point.setColor(hotColor);
            } else {
                point.setColor(baseColor);
            }
        } else if (coloringMethod == ColoringMethod.DecayTrail) {
            if (point == projector.getCurrentPoint()) {
                if (point == projector.getCurrentPoint() && hotPointMode == true) {
                    point.setColor(hotColor);
                } else {
                    point.setColor(baseColor);
                }
                point.spikeActivation(ceiling);
            } else {
                point.decrementActivation(floor, decrementAmount);
                point.setColorBasedOnVal(Utils.colorToFloat(baseColor));
            }
        } else if (coloringMethod == ColoringMethod.Frequency) {
            if (point == projector.getCurrentPoint()) {
                if (point == projector.getCurrentPoint() && hotPointMode == true) {
                    point.setColor(hotColor);
                } else {
                    point.setColor(baseColor);
                }
                point.incrementActivation(ceiling, incrementAmount);
            } else {
                point.setColorBasedOnVal(Utils.colorToFloat(baseColor));
            }
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
        }
    }

    /**
     * @return the hotColor
     */
    @DisplayOrder(val = 10)
    public Color getHotColor() {
        return hotColor;
    }

    /**
     * @return the baseColor
     */
    @DisplayOrder(val = 20)
    public Color getBaseColor() {
        return baseColor;
    }

    /**
     * @return the floor
     */
    @DisplayOrder(val = 30)
    public double getFloor() {
        return floor;
    }

    /**
     * @return the ceiling
     */
    @DisplayOrder(val = 40)
    public double getCeiling() {
        return ceiling;
    }

    /**
     * @return the incrementAmount
     */
    @DisplayOrder(val = 50)
    public double getIncrementAmount() {
        return incrementAmount;
    }

    /**
     * @return the decrementAmount
     */
    @DisplayOrder(val = 60)
    public double getDecrementAmount() {
        return decrementAmount;
    }

    /**
     * @param hotColor the hotColor to set
     */
    public void setHotColor(Color hotColor) {
        this.hotColor = hotColor;
    }

    /**
     * @param baseColor the baseColor to set
     */
    public void setBaseColor(Color baseColor) {
        this.baseColor = baseColor;
    }

    /**
     * @param floor the floor to set
     */
    public void setFloor(double floor) {
        this.floor = floor;
    }

    /**
     * @param ceiling the ceiling to set
     */
    public void setCeiling(double ceiling) {
        this.ceiling = ceiling;
    }

    /**
     * @param incrementAmount the incrementAmount to set
     */
    public void setIncrementAmount(double incrementAmount) {
        this.incrementAmount = incrementAmount;
    }

    /**
     * @param decrementAmount the decrementAmount to set
     */
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

    /**
     * @return the hotPointMode
     */
    public boolean getHotPointMode() {
        return hotPointMode;
    }
}
