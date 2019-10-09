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
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Data model for a raster plot.
 */
public class RasterModel implements AttributeContainer, EditableObject {

    /**
     * Default number of data sources for plot initialization.
     */
    private static final int INITIAL_DATA_SOURCES = 1;

    /**
     * List of {@link RasterConsumer}'s that consume raster data.
     */
    private List<RasterConsumer> rasterConsumerList = new ArrayList<>();

    /**
     * Lambda to supply time to the time series model.
     */
    private transient Supplier<Integer> timeSupplier;

    /**
     * Raster Data.
     */
    private XYSeriesCollection dataset = new XYSeriesCollection();

    /**
     * Should the range automatically change to reflect the data.
     */
    @UserParameter(label = "Auto Range", order = 3)
    private boolean autoRange = true;

    /**
     * Size of window.
     */
    @UserParameter(label = "Dot Size", description = "Size of dots in chart", order = 5)
    private int dotSize = 4;

    /**
     * Size of window.
     */
    @UserParameter(label = "Window Size", order = 5)
    private int windowSize = 100;

    /**
     * Upper bound of the chart range.
     */
    @UserParameter(label = "Range upper Bound", order = 10)
    private double rangeUpperBound = 1;

    /**
     * Lower bound of the chart range.
     */
    @UserParameter(label = "Range Lower Bound", order = 20)
    private double rangeLowerBound = 0;

    /**
     * Whether this chart if fixed width or not.
     */
    @UserParameter(label = "Fixed width", order = 30)
    private boolean fixedWidth = true;

    /**
     * Raster series model constructor.
     */
    public RasterModel(Supplier<Integer> timeSupplier) {
        addDataSources(INITIAL_DATA_SOURCES);
        this.timeSupplier = timeSupplier;
    }

    /**
     * Create specified number of set of data sources. Adds these two existing
     * data sources.
     *
     * @param numDataSources number of data sources to initialize plot with
     */
    public void addDataSources(final int numDataSources) {
        for (int i = 0; i < numDataSources; i++) {
            addDataSource();
        }
    }

    /**
     * Removes a data source from the chart.
     */
    public void removeDataSource() {
        Integer lastSeriesIndex = dataset.getSeriesCount() - 1;
        if (lastSeriesIndex > 0) {
            dataset.removeSeries(lastSeriesIndex);
            rasterConsumerList.remove(lastSeriesIndex);
        }

    }

    /**
     * Adds a data source to the chart.
     */
    public void addDataSource() {
        Integer currentSize = dataset.getSeriesCount();
        dataset.addSeries(new XYSeries(currentSize + 1));
        rasterConsumerList.add(new RasterConsumer(currentSize));
    }

    /**
     * Clears the plot.
     */
    public void clearData() {
        int seriesCount = dataset.getSeriesCount();
        for (int i = 0; seriesCount > i; ++i) {
            dataset.getSeries(i).clear();
        }
    }

    public XYSeriesCollection getDataset() {
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

    public boolean isFixedWidth() {
        return fixedWidth;
    }

    public void setFixedWidth(final boolean fixedWidth) {
        this.fixedWidth = fixedWidth;
    }


    public List<RasterConsumer> getRasterConsumerList() {
        return rasterConsumerList;
    }

    public Supplier<Integer> getTimeSupplier() {
        return timeSupplier;
    }

    public int getDotSize() {
        return dotSize;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(final int windowSize) {
        this.windowSize = windowSize;
    }

    public boolean isAutoRange() {
        return autoRange;
    }

    public void setAutoRange(final boolean autoRange) {
        this.autoRange = autoRange;
    }

    public double getRangeUpperBound() {
        return rangeUpperBound;
    }

    public void setRangeUpperBound(final double upperBound) {
        this.rangeUpperBound = upperBound;
    }

    public double getRangeLowerBound() {
        return rangeLowerBound;
    }

    public void setRangeLowerBound(final double lowerRangeBoundary) {
        this.rangeLowerBound = lowerRangeBoundary;
    }

    /**
     *  Objects that represent separate sets of raster points, shown in a different color in the
     *      chart.
     */
    public class RasterConsumer implements AttributeContainer {

        /**
         * Index of this consumer in an {@link XYSeriesCollection}
         */
        int index = 0;

        RasterConsumer(int index) {
            this.index = index;
        }

        @Consumable()
        public void setValues(final double[] values) {
            if (values.length == 0) {
                getDataset().getSeries(index).add(timeSupplier.get(), null);
                return;
            }
            for (int i = 0, n = values.length; i < n; i++) {
                getDataset().getSeries(index).add(timeSupplier.get(), (Double) values[i]);
            }
        }

        @Override
        public String getId() {
            return "Raster " + (index + 1);
        }

    }

}
