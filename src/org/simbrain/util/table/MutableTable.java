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

/**
 * Superclass for tables which can be modified.
 *
 * @author jyoshimi
 *
 */
public abstract class MutableTable<T> extends SimbrainDataTable<T> {

    /**
     * Add a new row.
     *
     * @param value value for cells of new row
     * @param fireEvent whether to fire an update event or not
     */
    private void addRow(T value, boolean fireEvent) {
        rowData.add(createNewRow(value));
        if (fireEvent) {
            this.fireTableRowsInserted(getRowCount(), getRowCount());
        }
    }

    /**
     * Add a new row.
     *
     * @param value value for cells of new row
     */
    public void addRow(T value) {
        addRow(value, true);
    }

    /**
     * Add a specified number of rows to the bottom of the table.
     *
     * @param rowsToAdd number of rows to add
     * @param value value for cells of new rows
     */
    public void addRows(int rowsToAdd, T value) {
        modifyRowsColumns(rowsToAdd, 0, value);
    }

    /**
     * Add a specified number of rows to the bottom of the table.
     *
     * @param rowsToAdd number of rows to add
     */
    public void addRows(final int rowsToAdd) {
        addRows(rowsToAdd, getDefaultValue());
    }

    /**
     * Insert a new row at the specified position.
     *
     * @param at row index for where to put the new row
     * @param value value for new row cells
     */
    public void insertRow(int at, T value) {
        rowData.add(at, createNewRow(value));
        this.fireTableRowsInserted(at, at);
    }

    /**
     * Insert a new row at the specified position, using the default data.
     *
     * @param at row index for where to put the new row
     */
    public void insertRow(int at) {
        insertRow(at, getDefaultValue());
    }

    /**
     * Create a new row for the table, with a specified value.
     *
     * @param value value for columns of new row
     * @return the new row
     */
    protected List<T> createNewRow(final T value) {
        ArrayList<T> row = new ArrayList<T>();
        for (int i = 0; i < getLogicalColumnCount(); i++) {
            row.add(value);
        }
        return row;
    }

    /**
     * Create a new row for the table, with a specified value.
     *
     * @param value value for columns of new row
     * @param cols number of "logical" columns in a row for this table (i.e.
     *            actual number of columns in the data itself, without
     *            accounting for fist header row).
     * @return the new row
     */
    protected List<T> createNewRow(final T value, final int cols) {
        ArrayList<T> row = new ArrayList<T>();

        for (int i = 0; i < cols; i++) {
            row.add(value);
        }
        return row;
    }

    /**
     * Remove row with choice whether to fire an event or not.
     *
     * @param rowToRemoveIndex index of row to remove
     * @param fireEvent whether to fire an event or not
     */
    public void removeRow(final int rowToRemoveIndex, boolean fireEvent) {
        // Don't allow getRowCount() to go to 0
        if (getRowCount() <= 1) {
            return;
        }
        rowData.remove(rowToRemoveIndex);
        if (fireEvent) {
            this.fireTableRowsDeleted(rowToRemoveIndex, rowToRemoveIndex);
        }
    }

    /**
     * Remove a specified row.
     *
     * @param rowToRemoveIndex index of row to remove.
     */
    public void removeRow(int rowToRemoveIndex) {
        removeRow(rowToRemoveIndex, true);
    }

    /**
     * Add a new column.
     *
     * @param value value for cells of new column
     * @param fireEvent whether to fire an update event or not
     */
    private void addColumn(T value, boolean fireEvent) {
        for (List<T> row : rowData) {
            row.add(value);
        }
        if (fireEvent) {
            this.fireTableStructureChanged();
        }
    }

    /**
     * Add a new column at the far right of the table.
     *
     * @param value value to add
     */
    public void addColumn(T value) {
        addColumn(value, true);
    }

    /**
     * Adds a specified number of columns to the right of the table.
     *
     * @param colsToAdd number of columns to add.
     * @param value  value for cells of new columns
     */
    public void addColumns(int colsToAdd, T value) {
        modifyRowsColumns(0, colsToAdd, value);
    }

    /**
     * Adds a specified number of columns to the right of the table.
     *
     * @param colsToAdd number of columns to add.
     */
    public void addColumns(int colsToAdd) {
        addColumns(colsToAdd, getDefaultValue());
    }

    /**
     * Insert a new column at the specified position.
     *
     * @param at column index where column should be added
     * @param value value for cells of new column
     */
    public void insertColumn(int at, T value) {
        for (List<T> row : rowData) {
            row.add(at, value);
        }
        this.fireTableStructureChanged();
    }

    /**
     * Insert a new column at the specified position, using the default data.
     *
     * @param at column index for where to put the new row
     */
    public void insertColumn(int at) {
        insertColumn(at, getDefaultValue());
    }

    /**
     * Remove column with choice whether to fire an event or not.
     *
     * @param columnToRemoveIndex index of row to remove
     * @param fireEvent whether to fire an event or not
     */
    public void removeColumn(final int columnToRemoveIndex, boolean fireEvent) {
        // Don't allow no columns
        if (getColumnCount() <= 1) {
            return;
        }
        for (List<T> row : rowData) {
            row.remove(columnToRemoveIndex);
        }
        if (fireEvent) {
            this.fireTableStructureChanged();
        }
    }

    /**
     * Remove column at specified index.
     *
     * @param columnToRemoveIndex index of column to remove
     */
    public void removeColumn(int columnToRemoveIndex) {
        removeColumn(columnToRemoveIndex, true);
    }

    /**
     * Adds or removes rows (from the bottom) and columns (from the left). Does not change value of existing
     * cells. Sets new cells to a specified value.
     *
     * @param newNumRows logical number of rows in table.
     * @param newNumCols logical number of columns in table.
     * @param value to be used for any new columns or rows added to the table.
     */
    public void modifyRowsColumns(int newNumRows, int newNumCols, T value) {

        // Modify columns
        int currentNumCols = getLogicalColumnCount();
        if (newNumCols > currentNumCols) {
            for (int i = 0; i < newNumCols - currentNumCols; ++i) {
                addColumn(value, false);
            }
        } else if (newNumCols < currentNumCols) {
            for (int i = 0; i < currentNumCols - newNumCols; ++i) {
                removeColumn(getLogicalColumnCount() - 1, false);
            }
        }

        // Modify rows
        int currentNumRows = getRowCount();
        if (newNumRows > currentNumRows) {
            for (int i = 0; i < newNumRows - currentNumRows; ++i) {
                addRow(value, false);
            }
        } else if (newNumRows < currentNumRows) {
            for (int i = 0; i < currentNumRows - newNumRows; ++i) {
                removeRow(getRowCount() - 1, false);
            }
        }
        fireTableStructureChanged();
    }

    /**
     * Reset the table structure.
     *
     * @param rows number of rows
     * @param cols number of columns
     */
    public void reset(int rows, int cols) {
        rowData.clear();

        for (int i = 0; i < rows; i++) {
            rowData.add(createNewRow(getDefaultValue(), cols));
        }

        fireTableStructureChanged();
    }

    /**
     * Adds rows or columns.
     *
     * @param rows number of rows to add.
     * @param cols number of columns to add.
     * @param val value to be added to the table.
     */
    public void reset(final int rows, final int cols, final T val) {
        reset(rows, cols);
        fill(val);

    }

    /**
     * Check the integrity of the data.
     *
     * @param allowRowChanges whether rows should be editable
     * @param allowColumnChanges whether columns should be editable
     * @param values the value to check
     * @throws TableDataException exception if data are invalid
     */
    protected void checkData(boolean allowRowChanges,
            boolean allowColumnChanges, Object[][] values)
            throws TableDataException {
        if (!allowRowChanges && values.length != getRowCount()) {
            throw new TableDataException("Trying to import data with "
                    + values.length + " rows into a table with "
                    + getRowCount() + " rows.");
        } else if (!allowColumnChanges
                && values[0].length != getLogicalColumnCount()) {
            throw new TableDataException("Trying to import data with "
                    + values[0].length + " columns into a table with "
                    + getLogicalColumnCount() + " columns.");
        }
    }

}