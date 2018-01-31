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
import java.util.Collections;
import java.util.List;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.simbrain.util.Utils;

/**
 * Superclass for tables that can be viewed by a SimbrainJTable, and saved in a
 * reasonable, readable way with XStream.
 *
 * All data are backed to a list of lists. This data structure can then be
 * converted to other data structures (e.g. a 2d array of doubles) as needed.
 * This is not as fast as alternatives but it's pretty fast and so suitable for
 * most purposes.
 *
 * This class can also be subclassed and relevant methods overridden for an
 * immutable, view type setup. For an example see WeightMatrixViewer.
 *
 * Note that the first column of all tables is a special header column that
 * displays row number or other information. For convenience and ease of use,
 * special "logical" methods are provided that use the indices of the data
 * itself(i.e. column 0 is the first column of the data, not the special
 * row-header column).
 *
 * @param <T> the type of the data to be displayed.
 * @author Jeff Yoshimi
 * @see org.simbrain.util.table.SimbrainJTable
 * @see org.simbrain.network.gui.WeightMatrixViewer
 */
public abstract class SimbrainDataTable<T> extends AbstractTableModel {

    /**
     * The data. For use with mutable tables. Subclasses can choose not to make
     * use of this (e.g. WeightMatrixViewer)
     */
    protected final List<List<T>> rowData;

    // Initialize data
    {
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
     * Custom column headings. Only used if set, otherwise default column
     * headings (1...n) used.
     */
    private List<String> columnHeadings;

    /** Whether to display column headings. */
    private boolean displayColumnHeadings = true;

    @Override
    public void setValueAt(Object val, int rowIndex, int columnIndex) {
        setValue(rowIndex, columnIndex, (T) val);
    }

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

        if (column == 0) {
            return; // Can't adjust first "header" column.
        }
        setLogicalValue(row, column - 1, value, fireEvent);
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (column == 0) {
            // This is taken care of by the CustomCellRenderer.
            return null;
        } else {
            // -1 To account for header column
            return getLogicalValueAt(row, column - 1);
        }
    }

    /**
     * Set the value at specific position in the data underlying the table, and
     * specify whether to fire a changed event (false useful when a lot of
     * values need to be changed at once and it would waste time to update the
     * GUI for every such change).
     *
     * @param row row index in the "logical" data
     * @param column column index in the "logical" data
     * @param value value to add
     * @param fireEvent true if an event should be fired, false otherwise.
     */
    public void setLogicalValue(final int row, final int column, final T value,
            final boolean fireEvent) {
        rowData.get(row).set(column, value);
        if (fireEvent) {
            this.fireTableCellUpdated(row, column);
        }

    }

    /**
     * Get the value of a specific cell in the data structure backing this table
     * data structure.
     *
     * @param row the row index
     * @param col the column index
     * @return the value at that cell
     */
    public T getLogicalValueAt(int row, int col) {
        return rowData.get(row).get(col);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Double.class;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        if (column == 0) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (!displayColumnHeadings) {
            return null;
        }
        if (columnIndex == 0) {
            return "#";
        } else {
            //System.out.println(columnHeadings + "," + columnIndex);
            if (columnHeadings != null) {
                return columnHeadings.get(columnIndex - 1);
            } else {
                return "" + columnIndex;
            }
        }
    }

    /**
     * Returns the number of columns in the underlying data, which is one less
     * than the number of columns in the table data.
     *
     * @return the number of columns in the data
     */
    public int getLogicalColumnCount() {
        if (rowData.size() > 0) {
            return rowData.get(0).size();
        } else {
            return 0;
        }
    }

    @Override
    public int getColumnCount() {
        if (rowData.size() > 0) {
            return rowData.get(0).size() + 1;
        } else {
            return 0;
        }
    }

    @Override
    public int getRowCount() {
        return rowData.size();
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
     * Shuffle the rows of the dataset.
     */
    public void shuffle() {
        Collections.shuffle(rowData);
        fireTableDataChanged();
    }

    /**
     * Returns a string array representation of the table.
     *
     * @return string array version of table
     */
    public String[][] asStringArray() {
        String stringArray[][] = new String[getRowCount()][getLogicalColumnCount()];
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getLogicalColumnCount(); j++) {
                stringArray[i][j] = "" + getLogicalValueAt(i, j);
            }
            // System.out.println(Arrays.toString(stringArray[i]));
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
                list.add(getLogicalValueAt(i, j));
            }
        }
        return list;
    }

    /**
     * @param columnHeadings the columnHeadings to set
     */
    public void setColumnHeadings(List<String> columnHeadings) {
        // System.out.println("setColumnHeadings " +
        // Arrays.asList(columnHeadings));
        this.columnHeadings = columnHeadings;
        this.fireTableStructureChanged();
    }

    /**
     * TODO: This only works for setting from true to false. Setting from false
     * back to true does not work.
     *
     * @param displayColumnHeadings the displayColumnHeadings to set
     */
    public void setDisplayColumnHeadings(boolean displayColumnHeadings) {
        this.displayColumnHeadings = displayColumnHeadings;
    }

    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = Utils.getSimbrainXStream();
        xstream.omitField(AbstractTableModel.class, "listenerList");
        return xstream;
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        if (listenerList == null) {
            listenerList = new EventListenerList();
        }
        super.addTableModelListener(l);
    }

}
