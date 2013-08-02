/*
 * Copyright (C) 2005,2007 The Authors. See http://www.simbrain.net/credits This
 * program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.plot.histogram;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.jfree.data.xy.IntervalXYDataset;
import org.simbrain.plot.ChartModel;
import org.simbrain.util.Utils;

/**
 * Underlying model for the histogram data, in the form of a list of double
 * arrays, one array per histogram. The histograms are represented by different
 * colors in HistogramPanel. The JFreeChart dataset is also stored here.
 *
 * @author Zach Tosi
 * @author Jeff Yoshimi
 *
 */
public class HistogramModel extends ChartModel {

    /** The default number of bins. **/
    public static final int DEFAULT_BINS = 15;

    /** Default number of data sources for plot initialization. */
    public static final int INITIAL_DATA_SOURCES = 4;

    /**
     * An array containing all the data series to be plotted. This is redundant
     * with the dataset object for JFreeChart, but must be kept in case the
     * number of bins is changed.
     */
    private List<double[]> data = new ArrayList<double[]>();

    /** The data set used to generate the histogram. */
    private OverwritableHistogramDataset dataSet = new OverwritableHistogramDataset();

    /** An array containing the names of each of the data series. */
    private List<String> dataNames = new ArrayList<String>();

    /** The default number of bins used by the histogram. */
    private int bins = DEFAULT_BINS;

    /**
     * This flag is used for safety, minimum and maximum values are determined
     * assuming sorted data series.
     */
    private boolean sortedFlag;

    /**
     * Creates a blank histogram. Used in de-serializing.
     */
    public HistogramModel() {
        this(null, null, DEFAULT_BINS);
    }

    /**
     * Creates a histogram with no data and a specified number of datsets.
     *
     * @param numdsources number of datasets to add
     */
    public HistogramModel(int numdsources) {
        this.addDataSources(numdsources);
        redraw();
    }

    /**
     * Creates a histogram with the provided number of bins, data set(s), and
     * data name(s), but does not include titles for the histogram, the x axis
     * or the y axis.
     *
     * @param data the data set(s) to be plotted
     * @param dataNames the name(s) of the data set(s)
     * @param bins the number of bins used in the histogram
     */
    public HistogramModel(List<double[]> data, List<String> dataNames, int bins) {
        this(data, dataNames, bins, "", "", "");
    }

    /**
     * Creates a histogram with the provided number of bins, data set(s), data
     * name(s), title, and x and y axis titles.
     *
     * @param data the data set(s) to be plotted
     * @param dataNames the name(s) of the data set(s)
     * @param bins the number of bins used in the histogram
     * @param title the title of the histogram
     * @param xAxisName the title of the x axis
     * @param yAxisName the title of the y axis
     */
    public HistogramModel(List<double[]> data, List<String> dataNames,
            int bins, String title, String xAxisName, String yAxisName) {
        this(data, dataNames, bins, title, xAxisName, yAxisName, null);
    }

    /**
     * Creates a histogram with the provided number of bins, data set(s), data
     * name(s), title, and x and y axis titles.
     *
     * @param newData the data set(s) to be plotted
     * @param dataNames the name(s) of the data set(s)
     * @param bins the number of bins used in the histogram
     * @param title the title of the histogram
     * @param xAxisName the title of the x axis
     * @param yAxisName the title of the y axis
     * @param colorPallet a custom color pallete
     */
    public HistogramModel(List<double[]> newData, List<String> dataNames,
            int bins, String title, String xAxisName, String yAxisName,
            Color[] colorPallet) {
        this.bins = bins;
        this.dataNames = dataNames;

        // Special initialization for case where no data is provided
        if (newData == null) {
            addDataSources(INITIAL_DATA_SOURCES);
        } else {
            this.data = newData;
        }

        for (int i = 0, n = data.size(); i < n; i++) {
            if (data.get(i).length != 0) {
                sort(data.get(i));
                double min = minValue(data.get(i));
                double max = maxValue(data.get(i));
                setSortedFlag(false);
                ((OverwritableHistogramDataset) dataSet).overwrriteSeries(i,
                        dataNames.get(i), data.get(i), getBins(), min, max);
            }

        }

    }

    /**
     * Add data to a specified data series.   This is the main
     * method used to dynamically add data when the histogram is used
     * as a plot component.  Called via reflection from HistogramComponent.
     *
     * @param index data index
     * @param histData the data to add at that index
     */
    public void addData(double[] histData, Integer index) {
        data.remove(index.intValue());
        data.add(index.intValue(), histData);
        redraw();
    }

    /**
     * Re-add the data.
     */
    public void redraw() {
        for (int i = 0; i < data.size(); i++) {
            // System.out.println(i + ":" + Utils.getVectorString(data.get(i),
            // ","));
            //System.out.println(i + ":" + dataNames.get(i));
            sort(data.get(i));
            double min = minValue(data.get(i));
            double max = maxValue(data.get(i));
            setSortedFlag(false);
            ((OverwritableHistogramDataset) dataSet).overwrriteSeries(i,
                    dataNames.get(i), data.get(i), getBins(), min, max);
        }
    }

    /**
     * Sorts the data set. This must be used before getMaxValue(...) or
     * getMinValue(...) is used.
     *
     * @param dataSeries the data set to be sorted.
     */
    public void sort(double dataSeries[]) {
        Arrays.sort(dataSeries);
        sortedFlag = true;
    }

    /**
     * Returns the max value from the data set by returning the last element of
     * the sorted data set.
     *
     * @param dataSeries the data series being queried.
     * @return the maximum value of the data set, according to their natural
     *         ordering as double-precision floating point values.
     * @throws IllegalStateException if the sorted flag has not been set to true
     *             (indicating the data set is unsorted or data is being
     *             manipulated in a way that breaks the contract of this class).
     */
    public double maxValue(double[] dataSeries) throws IllegalStateException {
        if (dataSeries.length == 0) {
            return 0;
        }
        if (sortedFlag) {
            return dataSeries[dataSeries.length - 1];
        } else {
            throw new IllegalStateException("Unsorted Dataset or Improper"
                    + "Dataset Manipulation Exception");
        }
    }

    /**
     * Returns the minimum value from the data set by returning the first
     * element of the sorted data set.
     *
     * @param dataSeries the data series being queried.
     * @return the minimum value of the data set, according to their natural
     *         ordering as double-precision floating point values.
     * @throws IllegalStateException if the sorted flag has not been set to true
     *             (indicating the data set is unsorted or data is being
     *             manipulated in a way that breaks the contract of this class).
     */
    public double minValue(double[] dataSeries) throws IllegalStateException {
        if (dataSeries.length == 0) {
            return 0;
        }
        if (sortedFlag) {
            return dataSeries[0];
        } else {
            throw new IllegalStateException("Unsorted Dataset or Improper"
                    + "Dataset Manipulation Exception");
        }
    }

    /**
     * @param data the data to set
     */
    public void resetData(List<double[]> data, List<String> names) {
        this.data = data;
        this.dataNames = names;
        redraw();
    }

    /**
     * Replaces data names with blank strings. A bit of a hack needed because
     * there is no way currently to dynamically change the labels of the
     * datasets.
     */
    private void resetDataNames() {
        // Replace names with blank strings
        for (int i = 0; i < dataNames.size(); i++) {
            dataNames.remove(i);
            dataNames.add(i, "---");
        }
    }

    /**
     * Clears the data. Currently just adds a single vector to each data source.
     */
    public void resetData() {
        for (int i = 0; i < data.size(); i++) {
            addData(new double[] { 0 }, i);
        }
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
     * Adds a data source to the chart.
     */
    public void addDataSource() {

        data.add(new double[] { 0 });
        dataNames.add("Hist " + data.size());
        this.fireDataSourceAdded(data.size());
        redraw();
    }

    /**
     * Sets the number of bins. Also automatically updates the number of bins
     * text field, but does NOT redraw the histogram.
     *
     * @param bins the new number of bins
     */
    public void setBins(int bins) {
        this.bins = bins;
    }

    /**
     * @return the sortedFlag
     */
    public boolean isSortedFlag() {
        return sortedFlag;
    }

    /**
     * @param sortedFlag the sortedFlag to set
     */
    public void setSortedFlag(boolean sortedFlag) {
        this.sortedFlag = sortedFlag;
    }

    /**
     * @return the data
     */
    public List<double[]> getData() {
        return data;
    }

    /**
     * @return the dataNames
     */
    public List<String> getDataNames() {
        return dataNames;
    }

    /**
     * @return the bins
     */
    public int getBins() {
        return bins;
    }

    /**
     * @return the dataSet
     */
    public IntervalXYDataset getDataSet() {
        return dataSet;
    }

}
