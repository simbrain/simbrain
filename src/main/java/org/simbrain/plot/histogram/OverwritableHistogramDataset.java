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

import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.statistics.HistogramBin;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.statistics.SimpleHistogramDataset;
import org.jfree.data.xy.AbstractIntervalXYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.util.ObjectUtilities;
import org.jfree.util.PublicCloneable;

import java.awt.*;
import java.io.Serializable;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

/**
 * A modification of the JFreeChart class HistogramDataset that allows data to
 * be overridden. The main change is the addition of the method overwrriteSeries
 * I would have extended HistogramDataset but needed access to the internal
 * list.
 *
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 * @see HistogramDataset
 * @see SimpleHistogramDataset
 */
public class OverwritableHistogramDataset extends AbstractIntervalXYDataset implements IntervalXYDataset, Cloneable, PublicCloneable, Serializable {

    /**
     * For serialization.
     */
    private static final long serialVersionUID = -6341668077370231153L;

    /**
     * A mapping from the names of data series to the data themselves.
     */
    private LinkedHashMap<String, ColoredDataSeries> dataMap = new LinkedHashMap<String, ColoredDataSeries>();

    /**
     * The histogram type.
     */
    private HistogramType type;

    /**
     * A value representing the largest value range among datasets.
     */
    private double maxRangeValue = Double.MIN_VALUE;

    /**
     * Creates a new (empty) dataset with a default type of
     * {@link HistogramType}.FREQUENCY.
     */
    public OverwritableHistogramDataset() {
        this.type = HistogramType.FREQUENCY;
    }

    /**
     * Returns the histogram type.
     *
     * @return The type (never <code>null</code>).
     */
    public HistogramType getType() {
        return this.type;
    }

    /**
     * Sets the histogram type and sends a {@link DatasetChangeEvent} to all
     * registered listeners.
     *
     * @param type the type (<code>null</code> not permitted).
     */
    public void setType(HistogramType type) {
        if (type == null) {
            throw new IllegalArgumentException("Null 'type' argument");
        }
        this.type = type;
        notifyListeners(new DatasetChangeEvent(this, this));
    }

    /**
     * Add new values to an existing series. Overwrites the old data. The value
     * set will be sorted after this method completes.
     *
     * @param key    the series key (<code>null</code> not permitted).
     * @param values the raw observations.
     * @param bins   the number of bins (must be at least 1).
     */
    public void overwriteSeries(String key, double[] values, int bins) {
        addSeries(key, values, bins);
    }

    /**
     * Adds a series to the dataset. Any data value less than minimum will be
     * assigned to the first bin, and any data value greater than maximum will
     * be assigned to the last bin. Values falling on the boundary of adjacent
     * bins will be assigned to the higher indexed bin.
     * <p>
     * The values array passed to this method will be sorted upon completion.
     *
     * @param key    the series key (<code>null</code> not permitted).
     * @param values the raw observations.
     * @param bins   the number of bins (must be at least 1).
     */
    public void addSeries(String key, double[] values, int bins) {
        if (key == null) {
            throw new IllegalArgumentException("Null 'key' argument.");
        }
        if (values == null) {
            throw new IllegalArgumentException("Null 'values' argument.");
        } else if (bins < 1) {
            throw new IllegalArgumentException("The 'bins' value must be at least 1.");
        }
        if (values.length == 0) {
            return;
        }

        if (dataMap.get(key) != null) {
            HistogramBin[] original = dataMap.get(key).data;
            double range = getRange(original);
            if (range >= maxRangeValue - maxRangeValue / 10 && range <= maxRangeValue + maxRangeValue / 10) {
                double newMax = Double.MIN_VALUE;
                for (ColoredDataSeries data : dataMap.values()) {
                    if (data.equals(dataMap.get(key))) {
                        continue;
                    }
                    range = getRange(data.data);
                    if (range > newMax) {
                        newMax = range;
                    }
                }
                maxRangeValue = newMax;
            }
        }
        Arrays.sort(values);
        double range = Math.abs(values[values.length - 1] - values[0]);
        if (range > maxRangeValue) {
            maxRangeValue = range;
        }
        HistogramBin[] histBins = new HistogramBin[0];
        double binWidth = 0;
        if (values.length != 0) {
            binWidth = (maxRangeValue) / bins;
            histBins = new HistogramBin[bins];
            int index = 0;
            HistogramBin bin;
            double endVal = 0;
            double startVal = values[0];
            for (int i = 0; i < bins; i++) {
                if (index < values.length) {
                    endVal = startVal + binWidth;
                    bin = new HistogramBin(startVal, endVal);
                    while (index < values.length && values[index] <= endVal) {
                        bin.incrementCount();
                        index++;
                    }
                    startVal = endVal;
                } else {
                    bin = new HistogramBin(0, 0); // Empty bin
                }
                histBins[i] = bin;
            }
        }

        ColoredDataSeries packet = new ColoredDataSeries(histBins);

        dataMap.put(key, packet);
        this.fireDatasetChanged();
    }

    /**
     * Contingent on the histogram bins being sorted.
     *
     * @param histSet
     * @return
     */
    private double getRange(HistogramBin[] histSet) {
        int cap1 = -1;
        int cap2 = -1;
        for (int i = 0; i < histSet.length; i++) {
            if (histSet[i].getCount() > 0) {
                cap1 = i;
                break;
            }
        }
        for (int i = histSet.length - 1; i >= 0; i--) {
            if (histSet[i].getCount() > 0) {
                cap2 = i;
                break;
            }
        }
        return Math.abs(histSet[cap2].getEndBoundary() - histSet[cap1].getStartBoundary());
    }

    /**
     * Reset the data in the data map field.
     *
     * @param names List of data series names
     * @param data  The data
     * @param bins  number of bins to use
     */
    public void resetData(List<String> names, List<double[]> data, int bins) {
        if (names.size() != data.size()) {
            throw new IllegalStateException("Number of names for series (" +
                names.size() + ") does not equal the number of data series (" +
                data.size() + ")");
        }
        Iterator<double[]> dataIterator = data.iterator();
        for (String str : names) {
            addSeries(str, dataIterator.next(), bins);
        }
        dataMap.keySet().retainAll(names);
    }

    /**
     * {@inheritDoc} A bit expensive...
     */
    @Override
    public Comparable<String> getSeriesKey(int arg0) {
        int i = 0;
        for (Entry<String, ColoredDataSeries> entry : dataMap.entrySet()) {
            if (arg0 == i) {
                return entry.getKey();
            }
            ++i;
        }
        return null;
    }

    /**
     * Returns the bins for a series.
     *
     * @param series the series index (in the range <code>0</code> to
     *               <code>getSeriesCount() - 1</code>).
     * @return A list of bins.
     * @throws IndexOutOfBoundsException if <code>series</code> is outside the
     *                                   specified range.
     */
    List<HistogramBin> getBins(int series) {
        return Arrays.asList(dataMap.get(getSeriesKey(series)).data);
    }

    /**
     * Returns the number of series in the dataset.
     *
     * @return The series count.
     */
    public int getSeriesCount() {
        return this.dataMap.size();
    }

    /**
     * Returns the number of data items for a series.
     *
     * @param series the series index (in the range <code>0</code> to
     *               <code>getSeriesCount() - 1</code>).
     * @return The item count.
     * @throws IndexOutOfBoundsException if <code>series</code> is outside the
     *                                   specified range.
     */
    public int getItemCount(int series) {
        return getBins(series).size();
    }

    /**
     * Returns the X value for a bin. This value won't be used for plotting
     * histograms, since the renderer will ignore it. But other renderers can
     * use it (for example, you could use the dataset to create a line chart).
     *
     * @param series the series index (in the range <code>0</code> to
     *               <code>getSeriesCount() - 1</code>).
     * @param item   the item index (zero based).
     * @return The start value.
     * @throws IndexOutOfBoundsException if <code>series</code> is outside the
     *                                   specified range.
     */
    public Number getX(int series, int item) {
        List<HistogramBin> bins = getBins(series);
        HistogramBin bin = bins.get(item);
        double x = (bin.getStartBoundary() + bin.getEndBoundary()) / 2.0;
        return new Double(x);
    }

    /**
     * Returns the y-value for a bin (calculated to take into account the
     * histogram type).
     *
     * @param series the series index (in the range <code>0</code> to
     *               <code>getSeriesCount() - 1</code>).
     * @param item   the item index (zero based).
     * @return The y-value.
     * @throws IndexOutOfBoundsException if <code>series</code> is outside the
     *                                   specified range.
     */
    public Number getY(int series, int item) {
        List<HistogramBin> bins = getBins(series);
        HistogramBin bin = bins.get(item);
        if (this.type == HistogramType.FREQUENCY) {
            return new Double(bin.getCount());
        } else if (this.type == HistogramType.RELATIVE_FREQUENCY) {
            return new Double(bin.getCount() / bins.size());
        } else if (this.type == HistogramType.SCALE_AREA_TO_1) {
            return new Double(bin.getCount() / bin.getBinWidth());
        } else { // pretty sure this shouldn't ever happen
            throw new IllegalStateException();
        }
    }

    /**
     * Returns the start value for a bin.
     *
     * @param series the series index (in the range <code>0</code> to
     *               <code>getSeriesCount() - 1</code>).
     * @param item   the item index (zero based).
     * @return The start value.
     * @throws IndexOutOfBoundsException if <code>series</code> is outside the
     *                                   specified range.
     */
    public Number getStartX(int series, int item) {
        List<HistogramBin> bins = getBins(series);
        HistogramBin bin = bins.get(item);
        return new Double(bin.getStartBoundary());
    }

    /**
     * Returns the end value for a bin.
     *
     * @param series the series index (in the range <code>0</code> to
     *               <code>getSeriesCount() - 1</code>).
     * @param item   the item index (zero based).
     * @return The end value.
     * @throws IndexOutOfBoundsException if <code>series</code> is outside the
     *                                   specified range.
     */
    public Number getEndX(int series, int item) {
        List<HistogramBin> bins = getBins(series);
        HistogramBin bin = bins.get(item);
        return new Double(bin.getEndBoundary());
    }

    /**
     * Returns the start y-value for a bin (which is the same as the y-value,
     * this method exists only to support the general form of the
     * {@link IntervalXYDataset} interface).
     *
     * @param series the series index (in the range <code>0</code> to
     *               <code>getSeriesCount() - 1</code>).
     * @param item   the item index (zero based).
     * @return The y-value.
     * @throws IndexOutOfBoundsException if <code>series</code> is outside the
     *                                   specified range.
     */
    public Number getStartY(int series, int item) {
        return getY(series, item);
    }

    /**
     * Returns the end y-value for a bin (which is the same as the y-value, this
     * method exists only to support the general form of the
     * {@link IntervalXYDataset} interface).
     *
     * @param series the series index (in the range <code>0</code> to
     *               <code>getSeriesCount() - 1</code>).
     * @param item   the item index (zero based).
     * @return The Y value.
     * @throws IndexOutOfBoundsException if <code>series</code> is outside the
     *                                   specified range.
     */
    public Number getEndY(int series, int item) {
        return getY(series, item);
    }

    /**
     * Tests this dataset for equality with an arbitrary object.
     *
     * @param obj the object to test against (<code>null</code> permitted).
     * @return A boolean.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj.getClass().equals(this.getClass()))) {
            return false;
        }
        OverwritableHistogramDataset that = (OverwritableHistogramDataset) obj;
        if (!ObjectUtilities.equal(this.type, that.type)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.dataMap, that.dataMap)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        final int p1 = 13;
        final int p2 = 89;
        return p1 * this.type.hashCode() + p2 * this.dataMap.hashCode();
    }

    /**
     * Returns a clone of the dataset.
     *
     * @return A clone of the dataset.
     * @throws CloneNotSupportedException if the object cannot be cloned.
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Set the color of a data series.
     *
     * @param seriesName the series name
     * @param color      the color to set
     */
    public void setSeriesColor(String seriesName, Color color) {
        if (dataMap.get(seriesName) != null) {
            dataMap.get(seriesName).color = color;
        }
    }

    /**
     * Returns the set of colored data series.
     *
     * @return the data
     */
    public Collection<ColoredDataSeries> getDataSeries() {
        return dataMap.values();
    }

    /**
     * A histogram data series associated with a color.
     *
     * @author Zoë
     */
    public static class ColoredDataSeries {

        /**
         * The data in a data series.
         */
        public final HistogramBin[] data;

        /**
         * The color of a given data series.
         */
        public Color color;

        /**
         * Create the data series.
         *
         * @param bins the data
         */
        public ColoredDataSeries(final HistogramBin[] bins) {
            this.data = bins;
        }
    }

}
