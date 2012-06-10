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

/**
 * Interface for tables (subclasses of SimbrainJTable) that have concept of
 * "current row" implemented which allows the table to iterated from row to row
 * when updated.
 *
 * @author jyoshimi
 *
 */
public interface MutableTable {

    // TODO: Assumes numeric data. Generalize to all forms of data.

    /**
     * Add a new row.
     *
     * @param value value for cells of new row
     */
    public void addRow(final double value);

    /**
     * Add a new column at the far right of the table.
     *
     * @param value value to add
     */
    public void addColumn(final double value);

    /**
     * Add a specified number of rows to the bottom of the table.
     *
     * @param rowsToAdd number of rows to add
     * @param number value for cells of new rows
     */
    public void addRows(final int rowsToAdd, final double number);

    /**
     * Adds a specified number of columns to the right of the table.
     *
     * @param colsToAdd number of columns to add.
     * @param defaultValue value for cells of new columns
     */
    public void addColumns(final int colsToAdd, final double defaultValue);

    /**
     * Insert a new row at the specified position.
     *
     * @param at row index for where to put the new row
     * @param value value for new row cells
     */
    public void insertRow(final int at, final double value);

    /**
     * Insert a new column at the specified position.
     *
     * @param at column index where column should be added
     * @param value value for cells of new column
     */
    public void insertColumn(final int at, final double value);

    /**
     * Remove a specified row.
     *
     * @param rowToRemoveIndex index of row to remove.
     */
    public void removeRow(final int rowToRemoveIndex);

    /**
     * Remove column at specified index.
     *
     * @param columnToRemoveIndex index of column to remove
     */
    public void removeColumn(final int columnToRemoveIndex);

    /**
     * Reset the table structure.
     *
     * @param rows number of rows
     * @param cols number of columns
     */
    public void reset(int rows, int cols);

    /**
     * Adds rows or columns.
     *
     * @param row number of rows to add.
     * @param col number of columns to add.
     * @param value to be added to the table.
     */
    public void addRowsColumns(final int row, final int col, final double value);

    /**
     * Adds or removes rows and columns. Does not change value of existing
     * cells. Sets new cells to a specified value.
     *
     * @param row Number of rows in table.
     * @param col Number of columns in table.
     * @param value to be used for any new columns or rows added to the table.
     */
    public void modifyRowsColumns(final int row, final int col,
            final double value);

}