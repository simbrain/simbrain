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
package org.simbrain.plot.barchart;

import com.thoughtworks.xstream.XStream;
import org.jfree.data.category.DefaultCategoryDataset;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;

import java.awt.*;

/**
 * Data for a JFreeChart bar chart.
 */
public class BarChartModel implements AttributeContainer, EditableObject {

    /**
     * JFreeChart dataset for bar charts.
     */
    private DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    /**
     * Color of bars in barchart.
     */
    @UserParameter(label = "Bar Color", order = 4)
    private Color barColor = Color.red;

    /**
     * Auto range bar chart.
     */
    @UserParameter(label = "Auto Range", order = 3, defaultValue = "true")
    private boolean autoRange = true;

    /**
     * Maximum range.
     */
    @UserParameter(label = "Upper Bound", defaultValue = "10", order = 2)
    private double upperBound = 10;

    /**
     * Minimum range.
     */
    @UserParameter(label = "Lower Bound", order = 1)
    private double lowerBound = 0;

    /**
     * Names for the bars in the barchart.  Set via coupling events.
     */
    private String[] barNames = {};

    /**
     * Track how many bars there are.  If an array with a different number of
     * components is sent to this component, numBars is updated.
     */
    private int numBars = 0;

    /**
     * Bar chart model constructor.
     */
    public BarChartModel() {
    }

    /**
     * Return JFreeChart category dataset.
     *
     * @return dataset
     */
    public DefaultCategoryDataset getDataset() {
        return dataset;
    }

    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = Utils.getSimbrainXStream();
        return xstream;
    }

    /**
     * Standard method call made to objects after they are deserialized. See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private Object readResolve() {
        return this;
    }

    public Color getBarColor() {
        return barColor;
    }

    public void setBarColor(final Color barColor) {
        this.barColor = barColor;
    }

    public boolean isAutoRange() {
        return autoRange;
    }

    public void setAutoRange(final boolean autoRange) {
        this.autoRange = autoRange;
    }

    public double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(final double upperBound) {
        this.upperBound = upperBound;
    }

    public double getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(final double lowerBound) {
        this.lowerBound = lowerBound;
    }

    public void setRange(double lowerBound, double upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    /**
     * Called by coupling producers via reflection.
     */
    @Consumable()
    public void setBarValues(double[] newPoint) {

        // Take care of size mismatches
        if (newPoint.length != numBars) {
            dataset.clear();
            numBars = newPoint.length;
        }

        // Write the data
        for (int i = 0; i < newPoint.length; i++) {
            if (i < barNames.length) {
                dataset.setValue((Number) newPoint[i], 1, barNames[i]);
            } else {
                // TODO: May need to go to this condition for if barNames is empty
                dataset.setValue((Number) newPoint[i], 1, "" + (i + 1));
            }
        }
    }

    public void setBarNames(String[] names) {
        this.barNames = names;
    }

}
