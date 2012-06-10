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

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Default implementation of a table of numerical data. The table is mutable,
 * and the data is saved as a list of lists of Doubles.
 *
 * @author jyoshimi
 */
public final class DefaultNumericTable extends NumericTable implements
        MutableTable, IterableRowsTable {

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

    /** Current row. */
    private int currentRow = 0;

    // Todo; init with a matrix of data.

    /**
     * Construct a table with a specified number of rows and columns.
     *
     * @param numRows number of rows.
     * @param numColumns number of columns.
     */
    public DefaultNumericTable(final int numRows, final int numColumns) {
        this.numRows = numRows;
        this.numColumns = numColumns;
        init();
    }

    /**
     * Construct a table from an 2-d array of doubles.
     *
     * @param data array of doubles
     */
    public DefaultNumericTable(final double[][] data) {
        this.numRows = data.length;
        if (data.length > 0) {
            this.numColumns = data[0].length;
        }
        init();
        setData(data);
    }

    /**
     * Default constructor.
     */
    public DefaultNumericTable() {
        init();
    }

    /**
     * Initialize listeners.
     */
    protected void init() {
        for (int i = 0; i < numRows; i++) {
            rowData.add(getNewRow(0));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.MutableTable#reset(int, int)
     */
    public void reset(int rows, int cols) {
        rowData.clear();

        numRows = rows;
        numColumns = cols;

        for (int i = 0; i < rows; i++) {
            rowData.add(getNewRow(0));
        }

        fireTableStructureChanged();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.MutableTable#reset(int, int, double)
     */
    public void reset(int rows, int cols, double val) {
        reset(rows, cols);
        fill(val);
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

    @Override
    public void setValue(final int row, final int column, final Double value) {
        rowData.get(row).set(column, value);
        // TODO: fireTableDataChanged() used to be called but it was a
        // performance problem. May be cases where update does not
        // happen properly. If so add amethod for setValue with a boolean
        // fireEvent flag here.
    }

    /**
     * Get the value at a specific location in the table.
     *
     * @param row row index
     * @param column column index
     * @return value to get
     */
    public Double getValue(final int row, final int column) {
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
    public double getValueCurrentRow(final int column) {
        return getValue(currentRow, column);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.MutableTable#addRow(double)
     */
    public void addRow(final double value) {
        addRow(value, true);
    }

    /**
     * Add a new row.
     *
     * @param value value for cells of new row
     * @param fireEvent whether to fire an update event or not
     */
    private void addRow(double value, boolean fireEvent) {
        numRows++;
        rowData.add(getNewRow(value));
        if (fireEvent) {
            this.fireRowAdded(numRows - 1);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.MutableTable#insertRow(int, double)
     */
    public void insertRow(final int at, final double value) {
        numRows++;
        rowData.add(at, getNewRow(value));
        this.fireRowAdded(at);
    }

    /**
     * Add a new column.
     *
     * @param value value for cells of new column
     * @param fireEvent whether to fire an update event or not
     */
    private void addColumn(double value, boolean fireEvent) {
        numColumns++;
        for (List<Double> row : rowData) {
            row.add(value);
        }
        if (fireEvent) {
            this.fireColumnAdded(numColumns - 1);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.MutableTable#addColumn(double)
     */
    public void addColumn(final double value) {
        addColumn(value, true);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.MutableTable#addRowsColumns(int, int,
     * double)
     */
    public void addRowsColumns(final int row, final int col, final double value) {
        modifyRowsColumns(row + numRows, col + numColumns, value);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.MutableTable#modifyRowsColumns(int, int,
     * double)
     */
    public void modifyRowsColumns(final int row, final int col,
            final double value) {
        int currentColNum = numColumns;
        int currentRowNum = numRows;
        if (col > currentColNum) {
            for (int i = 0; i < col - currentColNum; ++i) {
                addColumn(value, false);
            }
        } else if (col < currentColNum) {
            for (int i = 0; i < currentColNum - col; ++i) {
                removeColumn(numColumns - 1, false);
            }
        }

        if (row > currentRowNum) {
            for (int i = 0; i < row - currentRowNum; ++i) {
                addRow(value, false);
            }
        } else if (row < currentRowNum) {
            for (int i = 0; i < currentRowNum - row; ++i) {
                removeRow(numRows - 1, false);
            }
        }
        fireTableStructureChanged();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.MutableTable#addRows(int, double)
     */
    public void addRows(final int rowsToAdd, final double number) {
        addRowsColumns(rowsToAdd, 0, number);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.MutableTable#addColumns(int, double)
     */
    public void addColumns(final int colsToAdd, final double defaultValue) {
        addRowsColumns(0, colsToAdd, defaultValue);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.MutableTable#insertColumn(int, double)
     */
    public void insertColumn(final int at, final double value) {
        numColumns++;
        for (List<Double> row : rowData) {
            row.add(at, value);
        }
        this.fireColumnAdded(at);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.MutableTable#removeRow(int)
     */
    public void removeRow(final int rowToRemoveIndex) {
        removeRow(rowToRemoveIndex, true);
    }

    /**
     * Remove row with choice whether to fire an event or not.
     *
     * @param rowToRemoveIndex index of row to remove
     * @param fireEvent whether to fire an event or not
     */
    public void removeRow(final int rowToRemoveIndex, boolean fireEvent) {
        // Don't allow numrows to go to 0
        if (numRows <= 1) {
            return;
        }
        numRows--;
        if (currentRow >= numRows) {
            currentRow = numRows - 1;
        }
        rowData.remove(rowToRemoveIndex);

        if (fireEvent) {
            this.fireRowRemoved(rowToRemoveIndex);
        }
    }

    /**
     * Remove column with choice whether to fire an event or not.
     *
     * @param columnToRemoveIndex index of row to remove
     * @param fireEvent whether to fire an event or not
     */
    public void removeColumn(final int columnToRemoveIndex, boolean fireEvent) {
        // Don't allow no columns
        if (numColumns <= 1) {
            return;
        }
        numColumns--;
        for (List<Double> row : rowData) {
            row.remove(columnToRemoveIndex);
        }
        if (fireEvent) {
            this.fireColumnRemoved(columnToRemoveIndex);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.MutableTable#removeColumn(int)
     */
    public void removeColumn(final int columnToRemoveIndex) {
        removeColumn(columnToRemoveIndex, true);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.IterableRows#getCurrentRow()
     */
    public int getCurrentRow() {
        return currentRow;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.IterableRows#isIterationMode()
     */
    public boolean isIterationMode() {
        return iterationMode;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.IterableRows#setIterationMode(boolean)
     */
    public void setIterationMode(boolean iterationMode) {
        this.iterationMode = iterationMode;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.IterableRows#setCurrentRow(int)
     */
    public void setCurrentRow(int currentRow) {
        this.currentRow = currentRow;
    }

    @Override
    public int getColumnCount() {
        return numColumns;
    }

    @Override
    public int getRowCount() {
        return numRows;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.table.IterableRows#update()
     */
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
        init();
        return this;
    }

}
