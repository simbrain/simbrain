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

import java.awt.Color;

import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.ComboBoxWrapper;
import org.simbrain.util.propertyeditor.DisplayOrder;

/**
 * Manage the coloring of datapoints.
 */
public class DataColoringManager {

    /**
     * List of methods for coloring points.
     */
    private enum ColoringMethod {

        /** No special coloring. All points colored the base color. */
        None,

        /**
         * Color the current point a "hot color" and the rest a "base color".
         */
        HotPoint,

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
    };

    /** How to color points. */
    private ColoringMethod coloringMethod = ColoringMethod.HotPoint;

    /** The "hot color" to be used for the current point. */
    private Color hotColor = Color.red;

    /** The base color to be used for all points besides the current point. */
    private Color baseColor = Color.green;

    /**
     * The lower bound for "activation" of a point in frequency and decay trail.
     */
    private double floor = 3;

    /**
     * The upper bound for "activation" of a point in frequency and decay trail.
     */
    private double ceiling = 10;

    /**
     * How much to increment activation at each time step (used in frequency
     * coloring method).
     */
    private double incrementAmount = .5;

    /**
     * How much to decrement activation at each time step (used in decay trail
     * coloring method).
     */
    private double decrementAmount = .5;

    /**
     * Helper variable holding the float representation of the current base
     * color.
     *
     * TODO: Test persistence.
     */
    private float floatColor = Utils.colorToFloat(baseColor);

    /**
     * Construct a data coloring manager.
     */
    public DataColoringManager() {
    }

    /**
     * Update the colors of all the datapoints in a dataset.
     *
     * @param data the dataset whose points should be colored.
     */
    void updateDataPointColors(Dataset data) {
        for (int i = 0; i < data.getNumPoints(); i++) {
            DataPointColored point = (DataPointColored) data.getPoint(i);
            if (coloringMethod == ColoringMethod.None) {
                point.setColor(baseColor);
            } else if (coloringMethod == ColoringMethod.HotPoint) {
                point.setColor(baseColor);
            } else if (coloringMethod == ColoringMethod.DecayTrail) {
                point.decrementActivation(floor, decrementAmount);
                point.setColorBasedOnVal(floatColor, ceiling);
            } else if (coloringMethod == ColoringMethod.Frequency) {
                // TODO: Add slight decay
                point.setColorBasedOnVal(floatColor, ceiling);
            }
        }
    }

    /**
     * Update the color of the current poin the dataset.
     *
     * @param point the point to update.
     */
    public void updateColorOfCurrentPoint(DataPointColored point) {
        if (coloringMethod == ColoringMethod.None) {
            return;
        } else if (coloringMethod == ColoringMethod.HotPoint) {
            point.setColor(hotColor);
        } else if (coloringMethod == ColoringMethod.DecayTrail) {
            point.spikeActivation(ceiling);
        } else if (coloringMethod == ColoringMethod.Frequency) {
            point.incrementActivation(ceiling, incrementAmount);
        }
    }

    /**
     * Get the coloring method.
     *
     * @return the method
     */
    @DisplayOrder(val = 1)
    public ComboBoxWrapper getColoringMethod() {
        return new ComboBoxWrapper() {
            public Object getCurrentObject() {
                return coloringMethod;
            }

            public Object[] getObjects() {
                return ColoringMethod.values();
            }
        };
    }

    /**
     * Set the coloring method.
     *
     * @param method the method to set
     */
    public void setColoringMethod(ComboBoxWrapper method) {
        coloringMethod = ((ColoringMethod) method.getCurrentObject());
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

}
