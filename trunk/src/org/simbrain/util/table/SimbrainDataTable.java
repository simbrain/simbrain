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
 * Superclass for tables that can be viewed by a SimbrainJTable, and saved in a
 * reasonable, readable way with XStream. Currently numerical table and a
 * default implementation are subclasses.
 *
 * @param <T> the type of the data to be displayed.
 * @author jyoshimi
 */
public abstract class SimbrainDataTable<T> {

    /** The data. */
    protected final List<List<T>> rowData;

    /** Listeners. */
    private List<SimbrainTableListener> listeners;

    // Initialize fields
    {
        listeners = new ArrayList<SimbrainTableListener>();
        rowData = new ArrayList<List<T>>();
    }

    /**
     * Returns the default cell value for a given table type.
     *
     * @return the default value.
     */
    abstract T getDefaultValue();

    /**
     * Returns the datatype (T) for a given table type.
     *
     * @return the data type.
     */
    abstract Class<?> getDataType();

    /**
     * Set the value at specific position in the table.
     *
     * @param row row index
     * @param col column index
     * @param value value to add
     */
    public void setValue(int row, int col, T value) {
        setValue(row, col, value, true);
    }

    /**
     * Set the value at specific position in the table, and specify whether to
     * fire a changed event (false useful when a lot of values need to be
     * changed at once and it would waste time to update the GUI for every such
     * change).
     *
     * @param row row index
     * @param column column index
     * @param value value to add
     * @param fireEvent true if an event should be fired, false otherwise.
     */
    public void setValue(final int row, final int column, final T value,
            final boolean fireEvent) {

        rowData.get(row).set(column, value);
        if (fireEvent) {
            fireCellDataChanged(row, column);
        }
    }

    /**
     * Get the value of a specific cell in the table.
     *
     * @param row the row index
     * @param col the column index
     * @return the value at that cell
     */
    public T getValue(int row, int col) {
        return rowData.get(row).get(col);
    }

    /**
     * Returns the number of columns in the dataset. Note the same as columns in
     * the simbrainjtable, which has an extra row and column for headers.
     *
     * @return the columns in the dataset.
     */
    public int getColumnCount() {
        if (rowData.size() > 0) {
            return rowData.get(0).size();
        } else {
            return 0;
        }
    }

    /**
     * Returns the number of rows in the dataset.
     *
     * @return the rows in the dataset.
     */
    public int getRowCount() {
        return rowData.size();
    }

    /**
     * Create a new row for the table, with a specified value.
     *
     * @param value value for columns of new row
     * @return the new row
     */
    protected List<T> getNewRow(final T value) {
        ArrayList<T> row = new ArrayList<T>();
        for (int i = 0; i < getColumnCount(); i++) {
            row.add(value);
        }
        return row;
    }

    /**
     * Create a new row for the table, with a specified value.
     *
     * @param value value for columns of new row
     * @param cols number of columns in a row for this table
     * @return the new row
     */
    protected List<T> getNewRow(final T value, final int cols) {
        ArrayList<T> row = new ArrayList<T>();

        for (int i = 0; i < cols; i++) {
            row.add(value);
        }
        return row;
    }

    /**
     * Fills the table with the given value.
     *
     * @param value value to fill the table with.
     */
    public void fill(final T value) {
        for (int i = 0; i < this.getRowCount(); i++) {
            for (int j = 0; j < this.getColumnCount(); j++) {
                setValue(i, j, value, false);
            }
        }
        this.fireTableDataChanged();
    }

    /**
     * Returns a string array representation of the table, useful in csv
     * parsing.
     *
     * @return string array version of table
     */
    public String[][] asStringArray() {
        String stringArray[][] = new String[getRowCount()][getColumnCount()];
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getColumnCount(); j++) {
                stringArray[i][j] = "" + getValue(i, j);
            }
        }
        return stringArray;
    }

    /**
     * Returns the contents of the table as a flat list.
     *
     * @return contents as a list.
     */
    public List<T> asFlatList() {
        List<T> list = new ArrayList<T>();
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getColumnCount(); j++) {
                list.add(getValue(i, j));
            }
        }
        return list;
    }

    /**
     * Add a table listener.
     *
     * @param listener listener to add
     */
    public void addListener(SimbrainTableListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<SimbrainTableListener>();
        }
        listeners.add(listener);
    }

    /**
     * Remove a table listener.
     *
     * @param listener listener to remove
     */
    public void removeListener(SimbrainTableListener listener) {
        listeners.remove(listener);
    }

    /**
     * Fire column added event.
     *
     * @param index index of column to add
     */
    public void fireColumnAdded(final int index) {
        for (SimbrainTableListener listener : listeners) {
            listener.columnAdded(index);
        }
    }

    /**
     * Fire row added event.
     *
     * @param index index of column to add
     */
    public void fireRowAdded(final int index) {
        for (SimbrainTableListener listener : listeners) {
            listener.rowAdded(index);
        }
    }

    /**
     * Fire column removed event.
     *
     * @param index index of column to remove
     */
    public void fireColumnRemoved(final int index) {
        for (SimbrainTableListener listener : listeners) {
            listener.columnRemoved(index);
        }
    }

    /**
     * Fire row removed event.
     *
     * @param index index of row to remove
     */
    public void fireRowRemoved(final int index) {
        for (SimbrainTableListener listener : listeners) {
            listener.rowRemoved(index);
        }
    }

    /**
     * Fire table data changed event. Only call when all data has changed. When
     * a specific bit of data has changed call celldata changed.
     */
    public void fireTableDataChanged() {
        for (SimbrainTableListener listener : listeners) {
            listener.tableDataChanged();
        }
    }

    /**
     * Fire cell data changed event.
     *
     * @param row row index
     * @param column column index
     */
    public void fireCellDataChanged(int row, int column) {
        for (SimbrainTableListener listener : listeners) {
            listener.cellDataChanged(row, column);
        }
    }

    /**
     * Fire table structure changed event.
     */
    public void fireTableStructureChanged() {
        for (SimbrainTableListener listener : listeners) {
            listener.tableStructureChanged();
        }
    }

}
