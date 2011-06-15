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

import org.simbrain.util.Utils;

/**
 * Superclass for data tables with numeric data.  Provides functions for
 * modifying those numeric values, and for import / export from .csv files.
 *
 * @author jyoshimi
 */
public abstract class NumericTable extends SimbrainDataTable {

    /** Randomization upper bound. */
    private int upperBound = 1;

    /** Randomization lower bound. */
    private int lowerBound = -1;

    /**
     * Initialize table with specified value.
     *
     * @param value value to initialize table with.
     */
    public void initValues(final double value) {
        fill(value);
    }

    /**
     * Fills the table with the given value.
     */
    public void fill(final double value) {
        for (int i = 0; i < this.getRowCount(); i++) {
            for (int j = 0; j < this.getColumnCount(); j++) {
                setValue(i, j, value, false);
            }
        }
        this.fireTableDataChanged();
    }

    /**
     * Normalize data in selected column. TODO: Use bounds.
     *
     * @param columnIndex column to normalize.
     */
    public void normalizeColumn(final int columnIndex) {
        // TODO: Check for valid column
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < this.getRowCount(); i++) {
            double val = getValue(i, columnIndex);
            if (val > max) {
                max = val;
            }
        }
        for (int i = 0; i < this.getRowCount(); i++) {
            setValue(i, columnIndex, getValue(i, columnIndex) / max, false);
        }
        this.fireTableDataChanged();
    }

    /**
     * Randomize neurons within specified bounds.
     */
    public void randomize() {
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

    public void randomize(int x, int y) {
        Random rand = new Random();
        int range = getUpperBound() - getLowerBound();
        double value = (rand.nextDouble() * range) + getLowerBound();
        setValue(x, y, value, false);
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
        // TOOD: If mutable do it
        //reset(values.length, values[0].length, 0);
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[0].length; j++) {
                if (((String) (values[i][j])).length() > 0) {
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
     * Reset data using a 2-d array of doubles.
     *
     * @param data the new data
     */
    public void setData(double[][] data) {
        // If mutable then do it...
        //reset(data.length, data[0].length, 0);
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                setValue(i, j, data[i][j], false);
            }
        }
        fireTableStructureChanged();
    }

    /**
     * Returns an array representation of the table.
     *
     * @return representation of table as double array
     */
    public double[][] asArray() {

        double returnList[][] = new double[getRowCount()][getColumnCount()];
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getColumnCount(); j++) {
                returnList[i][j] = (Double) this.getValue(i, j);
            }
        }
        return returnList;
    }

    /**
     * Returns a string array representation of the table, useful in csv
     * parsing.
     *
     * @return string array version of table
     */
    public String[][] asStringArray() {
        double doubleArray[][] = asArray();
        String stringArray[][] = new String[getRowCount()][getColumnCount()];
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getColumnCount(); j++) {
                stringArray[i][j] = "" + doubleArray[i][j];
            }
        }
        return stringArray;
    }


}
