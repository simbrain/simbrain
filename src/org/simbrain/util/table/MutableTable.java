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
        rowData.add(getNewRow(value));
        if (fireEvent) {
            this.fireRowAdded(getRowCount() - 1);
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
        rowData.add(at, getNewRow(value));
        this.fireRowAdded(at);
    }

    /**
     * Insert a new row at the specified position,
     * using the default data.
     *
     * @param at row index for where to put the new row
     */
    public void insertRow(int at) {
        insertRow(at, getDefaultValue());
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
            this.fireRowRemoved(rowToRemoveIndex);
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
            this.fireColumnAdded(getColumnCount() - 1);
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
     * @param defaultValue value for cells of new columns
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
        this.fireColumnAdded(at);
    }

    /**
     * Insert a new column at the specified position,
     * using the default data.
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
            this.fireColumnRemoved(columnToRemoveIndex);
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
     * Adds or removes rows and columns. Does not change value of existing
     * cells. Sets new cells to a specified value.
     *
     * @param row Number of rows in table.
     * @param col Number of columns in table.
     * @param value to be used for any new columns or rows added to the table.
     */
    public void modifyRowsColumns(int row, int col, T value) {
        int currentColNum = getColumnCount();
        int currentRowNum = getRowCount();
        if (col > currentColNum) {
            for (int i = 0; i < col - currentColNum; ++i) {
                addColumn(value, false);
            }
        } else if (col < currentColNum) {
            for (int i = 0; i < currentColNum - col; ++i) {
                removeColumn(getColumnCount() - 1, false);
            }
        }

        if (row > currentRowNum) {
            for (int i = 0; i < row - currentRowNum; ++i) {
                addRow(value, false);
            }
        } else if (row < currentRowNum) {
            for (int i = 0; i < currentRowNum - row; ++i) {
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
            rowData.add(getNewRow(getDefaultValue(), cols));
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

}