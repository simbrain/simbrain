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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.simbrain.util.Utils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * A table of data, with convenience methods and functions useful in neural
 * network and other Simbrain applications.
 *
 * An optional concept of "current row" is implemented which allows the table to
 * iterated from row to row when updated.
 */
public class SimbrainDataTable {

    /** Default initial number of rows. */
    private static final int DEFAULT_ROW_COUNT = 30;

    /** Default initial number of columns. */
    private static final int DEFAULT_COLUMN_COUNT = 5;

    /** The data. */
    private List<List<Double>> rowData = new ArrayList<List<Double>>();

    /** Number of columns. */
    private int numColumns = DEFAULT_COLUMN_COUNT;

    /** Number of rows. */
    private int numRows = DEFAULT_ROW_COUNT;

    /** Iteration mode. */
    private boolean iterationMode = false;

    /** Randomization upper bound. */
    private int upperBound = 1;

    /** Randomization lower bound. */
    private int lowerBound = 0;

    /** Current row. */
    private int currentRow = 0;

    /** Listeners. */
    private List<TableListener> listeners;

    // TODO: Document this.
    boolean initialized = false;

    /**
     * Construct a dataworld model of a specified number of rows and columns.
     *
     * @param numRows number of rows.
     * @param numColumns number of columns.
     */
    public SimbrainDataTable(final int numRows, final int numColumns) {
        this.numRows = numRows;
        this.numColumns = numColumns;
        init();
    }

    /**
     * Default constructor.
     */
    public SimbrainDataTable() {
        init();
    }

    /**
     * Initialize listeners.
     */
    private void init() {
        for (int i = 0; i < numRows; i++) {
            rowData.add((List<Double>) getNewRow(0));
        }
        listeners = new ArrayList<TableListener>();
    }


    /**
     * Reset the table structure.
     *
     * @param rows number of rows
     * @param cols number of columns
     */
    public void reset(int rows, int cols) {
        rowData.clear();

        numRows = rows;
        numColumns = cols;

        for (int i = 0; i < rows; i++) {
            rowData.add((List<Double>) getNewRow(0));
        }

        fireStructureChanged();
    }

    /**
     * Reset the table structure, and use a specified fill value.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @param val new fill value
     */
    public void reset(int rows, int cols, double val) {
        reset(rows, cols);
        fill(val);
    }

    /**
     * Initialize table with specified value.
     *
     * @param value value to initialize table with.
     */
    public void initValues(final double value) {
        if (!initialized) {
            fill(value);
            initialized = true;
        }
    }

    /**
     * Fills the table with the given value.
     */
    public void fill(final double value) {
        for (List<Double> row : rowData) {
            Collections.fill(row, value);
        }
        this.fireDataChanged();
    }

    /**
     * Randomize neurons within specified bounds.
     */
    public void randomize() {
        Random rand = new Random();
        int range = getUpperBound() - getLowerBound();
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getColumnCount(); j++ ) {
                double value = (rand.nextDouble() * range) + getLowerBound();
                setValue(i , j, value);
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
        xstream.omitField(SimbrainDataTable.class, "parent");
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
        init();
        initialized = true;
        return this;
    }

    /**
     * Create a new row for the table, with a specified value.
     *
     * @param value value for columns of new row
     * @return the new row
     */
    private List<Double> getNewRow(final double value) {
        ArrayList<Double> row = new ArrayList<Double>();

        for (int i = 0; i < numColumns; i++) {
            row.add(value);
        }
        return row;
    }

    /**
     * Set the value at specific position in the table.
     *
     * @param row row index
     * @param column column index
     * @param value value to add
     */
    public void setValue(final int row, final int column, final double value) {
        rowData.get(row).set(column, value);
        for (TableListener listener : listeners) {
            listener.itemChanged(row, column);
        }
    }

    /**
     * Get the value at a specific location in the table.
     *
     * @param row row index
     * @param column column index
     * @return value to get
     */
    public double get(final int row, final int column) {
        return rowData.get(row).get(column);
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
    public Double getValueCurrentRow(final int column) {
        // TODO: Change Double to double after attribute refactor.
        return get(currentRow, column);
    }

    /**
     * Add a new row at the bottom of the table.
     *
     * @param value value for new row
     */
    public void addNewRow(final double value) {
        numRows++;
        rowData.add(getNewRow(value));
        for (TableListener listener : listeners) {
            listener.rowAdded(numRows - 1);
        }
    }

    /**
     * Insert a new row at the specified position.
     *
     * @param at row index for where to put the new row
     * @param value value for new row cells
     */
    public void insertNewRow(final int at, final double value) {
        numRows++;
        rowData.add(at, getNewRow(value));
        for (TableListener listener : listeners) {
            listener.rowAdded(at);            
        }
    }

    /**
     * Add a new column at the far right of the table.
     *
     * @param value value to add
     */
    public void addNewColumn(final double value) {
        numColumns++;
        for (List<Double> row : rowData) {
            row.add(value);
        }
        for (TableListener listener : listeners) {
            listener.columnAdded(numColumns - 1);
        }
    }

    /**
     * Adds rows or columns.
     * @param row number of rows to add.
     * @param col number of columns to add.
     * @param value to be added to the table.
     */
    public void addRowsColumns(final int row, final int col, final double value) {
        modifyRowsColumns(row + numRows, col + numColumns, value);
    }

    /**
     * Adds or removes rows and columns.
     *
     * @param row Number of rows in table.
     * @param col Number of columns in table.
     * @param value to be added in the table.
     */
    public void modifyRowsColumns(final int row, final int col, final double value) {
        int currentColNum = numColumns;
        int currentRowNum = numRows;
        if (col > currentColNum) {
            for (int i = 0; i < col - currentColNum; ++i) {
                addNewColumn(value);
            }
        } else if (col < currentColNum) {
            for (int i = 0; i < currentColNum - col; ++i) {
                removeLastColumn();
            }
        }

        if (row > currentRowNum) {
            for (int i = 0; i < row - currentRowNum; ++i) {
                addNewRow(value);
            }
        } else if (row < currentRowNum) {
            for (int i = 0; i < currentRowNum - row; ++i) {
                removeLastRow();
            }
        }
    }

    /**
     * Add a specified number of rows to the bottom of the table.
     *
     * @param rowsToAdd number of rows to add
     * @param number value for cells of new rows
     */
    public void addRows(final int rowsToAdd, final double number) {
        addRowsColumns(rowsToAdd, 0, number);
    }

    /**
     * Adds a specified nmber of columns to the right of the table.
     *
     * @param colsToAdd number of columns to add.
     * @param number value for cells of new columns
     */
    public void addColumns(final int colsToAdd, final double number) {
        addRowsColumns(0, colsToAdd, number);
    }

    /**
     * Insert a new column at the specified position.
     *
     * @param at column index where column should be added
     * @param value value for cells of new column
     */
    public void insertNewColumn(final int at, final double value) {
        numColumns++;
        for (List<Double> row : rowData) {
            row.add(at, value);
        }
        for (TableListener listener : listeners) {
            listener.columnAdded(at);
        }
    }

    /**
     * Remove last row.
     */
    public void removeLastRow() {
        numRows--;
        rowData.remove(numRows);
        for (TableListener listener : listeners) {
            listener.rowRemoved(numRows);
        }
    }

    /**
     * Remove a specified row.
     *
     * @param rowToRemoveIndex index of row to remove.
     */
    public void removeRow(final int rowToRemoveIndex) {
        numRows--;
        if (currentRow >= numRows) {
            currentRow = numRows - 1;
        }
        rowData.remove(rowToRemoveIndex);
        for (TableListener listener : listeners) {
            listener.rowRemoved(rowToRemoveIndex);
        }
    }

    /**
     * Remove last column.
     */
    public void removeLastColumn() {
        numColumns--;
        for (List<Double> row : rowData) {
            row.remove(numColumns);
        }
        for (TableListener listener : listeners) {
            listener.columnRemoved(numColumns);
        }
    }

    /**
     * Remove column at specified index.
     *
     * @param columnToRemoveIndex index of column to remove
     */
    public void removeColumn(final int columnToRemoveIndex) {
        numColumns--;
        for (List<Double> row : rowData) {
            row.remove(columnToRemoveIndex);
        }
        for (TableListener listener : listeners) {
            listener.columnRemoved(columnToRemoveIndex);
        }
    }

    /**
     * Lower bound for (e.g.) randomization
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
     * Returns the current row.
     *
     * @return current row
     */
    public int getCurrentRow() {
        return currentRow;
    }

    /**
     * Whether the table is in iteration mode (where update increments the current row).
     *
     * @return true if in iteration mode
     */
    public boolean isIterationMode() {
        return iterationMode;
    }

    /**
     * Set iteration mode.
     *
     * @param iterationMode iteration mode to set
     */
    public void setIterationMode(boolean iterationMode) {
        this.iterationMode = iterationMode;
    }

    /**
     * Sets the current row.
     *
     * @param currentRow current row to set
     */
    public void setCurrentRow(int currentRow) {
        this.currentRow = currentRow;
    }

    /**
     * Returns number of columns.
     *
     * @return number of columns.
     */
    public int getColumnCount() {
        return numColumns;
    }

    /**
     * Returns number of rows.
     *
     * @return number of rows.
     */
    public int getRowCount() {
        return numRows;
    }

    /**
     * Update the table, by increment the row. If the row is the bottom row,
     * loop back to first row.
     */
    public void update() {
        if (isIterationMode()) {
            if (getCurrentRow() >= (getRowCount() - 1)) {
                setCurrentRow(0);
            } else {
                setCurrentRow(getCurrentRow() + 1);
            }
        }
    }


    /**
     * Returns an array representation of the table.
     * 
     * @return representation of table as double arroy
     */
    public double[][] asArray() {
        double returnList[][] = new double[getRowCount()][getColumnCount()];
        
        for(int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getColumnCount(); j++) {
                returnList[i][j] = (Double) this.get(i, j);
            }
        }
        return returnList;
    }
    

    /**
     * Returns a string array representation of the table, useful in csv parsing.
     *
     * @return string array version of table
     */
    public String[][] asStringArray() {
        double doubleArray[][] = asArray();
        String stringArray[][] = new String[getRowCount()][getColumnCount()];
        for(int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getColumnCount(); j++) {
                stringArray[i][j] = "" + doubleArray[i][j];
            }
        }
        return stringArray;
    }
    
    /**
     * Read in stored dataset file as CVS File.
     *
     * @param file the CSV file
     */
    public void readData(final File file) {

        String[][] values = Utils.getStringMatrix(file);
        reset(values.length, values[0].length, 0);
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[0].length; j++) {
                if (((String) (values[i][j])).length() > 0) {
                    Double num = new Double(0);
                    try {
                        num = Double.valueOf(values[i][j]);
                    } catch (NumberFormatException exception) {
                    } finally {
                        setValue(i, j, num);
                    }
                }
            }
        }

    }

    /**
     * Listener interface for receiving table structure and data change events.
     */
    public interface TableListener {

        /**
         * A column was added.
         *
         * @param column index of new column
         */
        void columnAdded(int column);

        /**
         * A column was removed.
         *
         * @param column index of removed column
         */
        void columnRemoved(int column);

        /**
         * A row was added.
         *
         * @param row index of added row.
         */
        void rowAdded(int row);

        /**
         * A row was removed.
         *
         * @param row index of removed row
         */
        void rowRemoved(int row);

        /**
         * A cell was changed.
         *
         * @param row row index
         * @param column column index
         */
        void itemChanged(int row, int column);

        /**
         * The table structure changed.
         */
        void structureChanged();

        /**
         * The table data changed.
         */
        void dataChanged();

    }

    /**
     * Fire data changed event.
     */
    public void fireDataChanged() {
        for(TableListener listener : listeners) {
            listener.dataChanged();
        }
    }

    /**
     * Fire data changed event.
     */
    public void fireStructureChanged() {
        for(TableListener listener : listeners) {
            listener.structureChanged();
        }
    }

    /**
     * Add a table listener.
     *
     * @param listener listener to add
     */
    public void addListener(TableListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a table listener.
     *
     * @param listener listener to remove
     */
    public void removeListener(TableListener listener) {
        listeners.remove(listener);
    }
}
