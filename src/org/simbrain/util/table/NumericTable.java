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
package org.simbrain.util.table;

import java.io.File;
import java.util.Random;

import org.simbrain.network.core.Synapse;
import org.simbrain.util.Utils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Default implementation of a table of numerical data. The table is mutable,
 * and the data is saved as a list of lists of Doubles.
 *
 * @author jyoshimi
 */
public class NumericTable extends MutableTable<Double> implements IterableRowsTable {

    /** Default initial number of rows. */
    private static final int DEFAULT_ROW_COUNT = 30;

    /** Default initial number of columns. */
    private static final int DEFAULT_COLUMN_COUNT = 5;

    /** Iteration mode. */
    private boolean iterationMode = false;

    /** Current row. */
    private int currentRow = 0;

    /** Randomization upper bound. */
    private int upperBound = 1;

    /** Randomization lower bound. */
    private int lowerBound = -1;

    /**
     * Construct a table with a specified number of rows and columns.
     *
     * @param numRows number of rows.
     * @param getColumnCount() number of columns.
     */
    public NumericTable(final int numRows, final int numColumns) {
        init(numRows, numColumns);
    }

    /**
     * Construct a table from an 2-d array of doubles.
     *
     * @param data array of doubles
     */
    public NumericTable(final double[][] data) {
        setData(data);
    }

    /**
     * Default constructor.
     */
    public NumericTable() {
        init(DEFAULT_ROW_COUNT, DEFAULT_COLUMN_COUNT);
    }

    /**
     * Initialize the table.
     *
     * @param rows num rows
     * @param cols num cols
     */
    protected void init(int rows, int cols) {
        rowData.clear();
        for (int i = 0; i < rows; i++) {
            rowData.add(getNewRow(new Double(0), cols));
        }
        fireTableStructureChanged();
    }

    @Override
    public Class<?> getDataType() {
        return Double.class;
    }

    @Override
    Double getDefaultValue() {
        return new Double(0);
    }

    /**
     * Reset data using a 2-d array of doubles.
     *
     * @param data the new data
     */
    public void setData(double[][] data) {
        reset(data.length, data[0].length);
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                this.setValue(i, j, data[i][j], false);
            }
        }
        fireTableDataChanged();
    }

    /**
     * Set the values of the specified column in the current row.
     *
     * @param column column index
     * @param value value to set
     */
    public void setValueCurrentRow(final int column, final double value) {
        setValue(currentRow, column, value);
    }

    /**
     * Get the value of the specified column in the current row.
     *
     * @param column column index
     * @return value of this column in current row
     */
    public double getValueCurrentRow(final int column) {
        return getValue(currentRow, column);
    }

    @Override
    public int getCurrentRow() {
        return currentRow;
    }

    @Override
    public void setCurrentRow(int currentRow) {
        this.currentRow = currentRow;
    }

    @Override
    public void updateCurrentRow() {
        if (isIterationMode()) {
            if (getCurrentRow() >= (getRowCount() - 1)) {
                setCurrentRow(0);
            } else {
                setCurrentRow(getCurrentRow() + 1);
            }
        }
    }

    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.omitField(SimbrainDataTable.class, "listeners");
        xstream.omitField(SimbrainDataTable.class, "initialized");
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
     * @return the iterationMode
     */
    public boolean isIterationMode() {
        return iterationMode;
    }

    /**
     * @param iterationMode the iterationMode to set
     */
    public void setIterationMode(boolean iterationMode) {
        this.iterationMode = iterationMode;
    }

    /**
     * Normalize data in selected column. TODO: Use bounds.
     *
     * @param columnIndex column to normalize.
     */
    public void normalizeColumn(final int columnIndex) { // TODO: combine with normalizeTable?
        // TODO: Check for valid column
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < this.getRowCount(); i++) {
            double val = getValue(i, columnIndex);
            if (val > max) {
                max = val;
            }
            if (val < min) {
                min = val;
            }
        }
        for (int i = 0; i < this.getRowCount(); i++) {
            setValue(i, columnIndex, (getValue(i, columnIndex) - min) / (max - min), false);
        }
        this.fireTableDataChanged();
    }

    /**
     * Randomize neurons within specified bounds.
     */
    public void randomizeTable() {
        Random rand = new Random();
        int range = getUpperBound() - getLowerBound();
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getColumnCount(); j++) {
                double value = (rand.nextDouble() * range) + getLowerBound();
                setValue(i, j, value, false);
            }
        }
        fireTableDataChanged();
    }

    /**
     * Normalize the whole table.
     */
    public void normalizeTable() {
        for (int i = 0; i < this.getColumnCount(); i++) {
            normalizeColumn(i);
        }
    }

    /**
     * Lower bound for (e.g.) randomization.
     *
     * @return The lower bound.
     */
    public int getLowerBound() {
        return lowerBound;
    }

    /**
     * Sets the lower bound .
     *
     * @param lowerBound value to set
     */
    public void setLowerBound(final int lowerBound) {
        this.lowerBound = lowerBound;
    }

    /**
     * Upper bound for (e.g.) randomization.
     *
     * @return The upper bound value.
     */
    public int getUpperBound() {
        return upperBound;
    }

    /**
     * Sets the upper bound value.
     *
     * @param upperBound Value to set
     */
    public void setUpperBound(final int upperBound) {
        this.upperBound = upperBound;
    }

    /**
     * Read in stored dataset file as CVS File.
     *
     * @param file the CSV file
     */
    public void readData(final File file) {
        String[][] values = Utils.getStringMatrix(file);
        reset(values.length, values[0].length);
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[0].length; j++) {
                if ((values[i][j]).length() > 0) {
                    Double num = new Double(0);
                    try {
                        num = Double.valueOf(values[i][j]);
                    } catch (NumberFormatException exception) {
                    } finally {
                        setValue(i, j, num, false);
                    }
                }
            }
        }
        fireTableStructureChanged();

    }

    /**
     * Returns a double array representation of the table.
     *
     * @return representation of table as double array
     */
    public double[][] asDoubleArray() {

        double returnList[][] = new double[getRowCount()][getColumnCount()];
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getColumnCount(); j++) {
                returnList[i][j] = this.getValue(i, j);
            }
        }
        return returnList;
    }

}
