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

import java.awt.Color;
import java.awt.EventQueue;

import org.jfree.data.category.DefaultCategoryDataset;
import org.simbrain.plot.ChartModel;
import org.simbrain.util.projection.Projector;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Data for a JFreeChart pie chart.
 */
public class BarChartModel extends ChartModel {
    
    /** JFreeChart dataset for bar charts. */
    private DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    /** Initial number of data sources. */
    private static final int INITIAL_DATA_SOURCES = 6;

    /** Color of bars in barchart. */
    private Color barColor = Color.red;

    /** Auto range bar chart. */
    private boolean autoRange = true;

    /** Maximum range. */
    private double upperBound = 10;

    /** Minimum range. */
    private double lowerBound = 0;

//    private Range chartRange = new Range(0, 10);

    /**
     * Bar chart model constructor.
     * @param parent component
     */
    public BarChartModel() {
    }

    /**
     * Return JFreeChart pie dataset.
     * 
     * @return dataset
     */
    public DefaultCategoryDataset getDataset() {
        return dataset;
    }

    /**
     * Default initialization.
     */
    public void defaultInit() {
        addDataSources(INITIAL_DATA_SOURCES);
    }

    /**
     * Create specified number of set of data sources.
     * Adds these two existing data sources.
     *
     * @param numDataSources number of data sources to initialize plot with
     */
    public void addDataSources(final int numDataSources) {
        int currentIndex = dataset.getColumnCount();
        for (int i = 0; i < numDataSources; i++) {
            addColumn(currentIndex + i);
        }
    }

    /**
     * Adds a new column to the dataset.
     */
    public void addColumn(final int index) {
        dataset.addValue(0, new Integer(1), new Integer(index));
        fireDataSourceAdded(index);
    }

    /**
     * Adds a bar to the bar chart dataset.
     */
    public void addColumn() {
        addColumn(dataset.getColumnCount());
    }

    /**
     * Removes the last bar from the bar chart data.
     */
    public void removeColumn() {
        if (dataset.getColumnCount() > 1) {
            removeColumn(dataset.getColumnCount() - 1);
        }
    }

    //TODO: Change names from row / column to "bars"?

    public void removeColumn(final int index) {
        dataset.removeColumn(index);
        fireDataSourceRemoved(index);
    }

    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = ChartModel.getXStream();
        return xstream;
    }

    /**
     * Set value of a specified bar.
     *
     * @param value value of bar
     * @param index which bar value to set
     */
    public void setValue(final Double value, final Integer index) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                getDataset().setValue(value, new Integer(1), index);
            }
        });
    }

    /**
     * Standard method call made to objects after they are deserialized.
     * See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private Object readResolve() {
        return this;
    }

    /**
     * Used for debugging model.
     */
    public void debug() {
        System.out.println("------------ Debug model ------------");
        for (int i = 0; i < dataset.getRowCount(); i++) {
            for (int j = 0; j < dataset.getColumnCount(); j++) {
                System.out.println("<" + i + "," + j + "> " + dataset.getValue(i, j));
            }
        }
        System.out.println("--------------------------------------");
    }

    /**
     * @return the barColor
     */
    public Color getBarColor() {
        return barColor;
    }

    /**
     * @param barColor
     *            the barColor to set
     */
    public void setBarColor(final Color barColor) {
        this.barColor = barColor;
        fireSettingsChanged();
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
        fireSettingsChanged();
    }

    /**
     * @return the upperBound
     */
    public double getUpperBound() {
        return upperBound;
    }

    /**
     * @param upperBound the upperBound to set
     */
    public void setUpperBound(final double upperBound) {
        this.upperBound = upperBound;
        fireSettingsChanged();
    }

    /**
     * @return the lowerBound
     */
    public double getLowerBound() {
        return lowerBound;
    }

    /**
     * @param lowerBound the lowerBound to set
     */
    public void setLowerBound(final double lowerBound) {
        this.lowerBound = lowerBound;
        fireSettingsChanged();
    }

    /**
     * @param lowerBound the lower range boundary.
     * @param upperBound the upper range boundary.
     */
    public void setRange(final double lowerBound, final double upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        fireSettingsChanged();
    }

}
