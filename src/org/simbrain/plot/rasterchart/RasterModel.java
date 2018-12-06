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
package org.simbrain.plot.rasterchart;

import com.thoughtworks.xstream.XStream;
import org.jfree.data.xy.XYSeries;
import org.simbrain.util.Utils;
import org.simbrain.workspace.AttributeContainer;

/**
 * Data model for a raster plot.
 */
public class RasterModel implements AttributeContainer {

    /**
     * Raster Data.
     */
    private XYSeries dataset = new XYSeries("Raster data");

    /**
     * Should the range automatically change to reflect the data.
     */
    private boolean autoRange = true;

    /**
     * Size of window.
     */
    private int windowSize = 100;

    /**
     * Upper bound of the chart range.
     */
    private double rangeUpperBound = 1;

    /**
     * Lower bound of the chart range.
     */
    private double rangeLowerBound = 0;

    /**
     * Whether this chart if fixed width or not.
     */
    private boolean fixedWidth = true;

    /**
     * Raster series model constructor.
     */
    public RasterModel() {

    }

    /**
     * Clears the plot.
     */
    public void clearData() {
        dataset.clear();
    }

    /**
     * @return JFreeChart data set.
     */
    public XYSeries getDataset() {
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

    /**
     * @return the fixedWidth
     */
    public boolean isFixedWidth() {
        return fixedWidth;
    }

    /**
     * @param fixedWidth the fixedWidth to set
     */
    public void setFixedWidth(final boolean fixedWidth) {
        this.fixedWidth = fixedWidth;
    }

    /**
     * @return the windowSize
     */
    public int getWindowSize() {
        return windowSize;
    }

    /**
     * @param windowSize the windowSize to set
     */
    public void setWindowSize(final int windowSize) {
        this.windowSize = windowSize;
    }

    /**
     * @return the autoRange
     */
    public boolean isAutoRange() {
        return autoRange;
    }

    /**
     * @param autoRange the autoRange to set
     */
    public void setAutoRange(final boolean autoRange) {
        this.autoRange = autoRange;
    }

    /**
     * @return the upperRangeBoundary
     */
    public double getRangeUpperBound() {
        return rangeUpperBound;
    }

    /**
     * @param upperBound the upperBound to set
     */
    public void setRangeUpperBound(final double upperBound) {
        this.rangeUpperBound = upperBound;
    }

    /**
     * @return the lowerRangeBoundary
     */
    public double getRangeLowerBound() {
        return rangeLowerBound;
    }

    /**
     * @param lowerRangeBoundary the lowerRangeBoundary to set
     */
    public void setRangeLowerBound(final double lowerRangeBoundary) {
        this.rangeLowerBound = lowerRangeBoundary;
    }

    /**
     * Add data to this model.
     *
     * @param time            data for x axis
     * @param value           data for y axis
     */
    public void addData(final double time, final double value) {
        getDataset().add(time, value);
    }


}
