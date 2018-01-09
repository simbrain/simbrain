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
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.jfree.data.category.DefaultCategoryDataset;
import org.simbrain.plot.ChartModel;
import org.simbrain.workspace.Consumable;

import com.thoughtworks.xstream.XStream;

/**
 * Data for a JFreeChart pie chart.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class BarChartModel extends ChartModel {

    /**
     * Bar encapsulates a single data column in the BarChartModel.
     */
    public class Bar {
        private String key;

        Bar(String key) {
            this.key = key;
            dataset.addValue((Number)0, 1, key);
        }

        public String getId() {
            return key;
        }

        @Consumable(idMethod="getId")
        public void setValue(double value) {
            dataset.setValue((Number)value, 1, key);
        }
    }

    /** JFreeChart dataset for bar charts. */
    @XmlJavaTypeAdapter(ChartDataAdapter.class)
    private DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    private List<Bar> bars = new ArrayList<Bar>();

    /** Color of bars in barchart. */
    private Color barColor = Color.red;

    /** Auto range bar chart. */
    private boolean autoRange = true;

    /** Maximum range. */
    private double upperBound = 10;

    /** Minimum range. */
    private double lowerBound = 0;

    /** Bar chart model constructor. */
    public BarChartModel() {}

    /**
     * Return JFreeChart category dataset.
     * @return dataset
     */
    public DefaultCategoryDataset getDataset() {
        return dataset;
    }

    /**
     * Create specified number of bars.
     * @param numBars number of bars to add.
     */
    public void addBars(int numBars) {
        for (int i = 0; i < numBars; i++) {
            addBar();
        }
    }

    /** Add a new bar to the dataset. */
    public void addBar() {
        // Should fetch the name of the bar from the user
        // Default should be "Bar" + (bars.size() + 1);
        Bar bar = new Bar("Bar" + + (bars.size() + 1));
        bars.add(bar);
        fireDataSourceAdded(0);
    }

    /**
     * Removes the last bar from the bar chart data.
     */
    public void removeBar() {
        if (dataset.getColumnCount() > 0) {
            bars.remove(bars.size() - 1);
            fireDataSourceRemoved(bars.size() - 1);
        }
    }

    public void removeBar(int index) {
        // TODO: This won't work yet
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
     * @return the barColor
     */
    public Color getBarColor() {
        return barColor;
    }

    /**
     * @param barColor the barColor to set
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
    public void setRange(double lowerBound, double upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        fireSettingsChanged();
    }

    /** Return a list of all bars used by this BarChartModel. */
    public List<Bar> getBars() {
        return bars;
    }

    static class ChartDataAdapter extends XmlAdapter<Number[][], DefaultCategoryDataset> {

        @Override
        public DefaultCategoryDataset unmarshal(Number[][] v) throws Exception {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (int i = 0; i < v.length; i++) {
                for (int j = 0; j < v[0].length; j++) {
                    dataset.addValue(v[i][j], i, j);
                }
            }
            return dataset;
        }

        @Override
        public Number[][] marshal(DefaultCategoryDataset dataset)
                throws Exception {
            Number[][] ret = new Number[dataset.getRowCount()][dataset
                    .getColumnCount()];
            for (int i = 0; i < dataset.getRowCount(); i++) {
                for (int j = 0; j < dataset.getColumnCount(); j++) {
                    ret[i][j] = dataset.getValue(i, j);
                }
            }
            return ret;
        }
    }

}
