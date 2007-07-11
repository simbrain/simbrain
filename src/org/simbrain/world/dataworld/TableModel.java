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
package org.simbrain.world.dataworld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.CouplingContainer;
import org.simbrain.workspace.Producer;

/**
 * <b>TableModel</b> extends DefaultTableModel so that the addRow and addColumn
 * commands are available.
 *
 * @author rbartley
 */
public class TableModel extends DefaultTableModel implements CouplingContainer {

    /** Default initial number of rows. */
    private static final int DEFAULT_ROW_COUNT = 5;

    /** Default initial number of columns. */
    private static final int DEFAULT_COLUMN_COUNT = 6;

    /** Current row. */
    private int currentRow = 0;

    /** List of couplings. */
    private ArrayList<Coupling> couplingList = new ArrayList<Coupling>();

    /** List of consumers. */
    private ArrayList<Consumer> consumers = new ArrayList<Consumer>();

    /** List of producers. */
    private ArrayList<Producer> producers = new ArrayList<Producer>();


    /**
     * Create a new table model for the specified data world.
     *
     * @param dataWorld data world
     */
    public TableModel(final DataWorld dataWorld) {

        for (int i = 1; i < DEFAULT_COLUMN_COUNT; i++) {
            this.addColumn(Integer.toString(i));
        }

        for (int i = 1; i < DEFAULT_ROW_COUNT; i++) {
            this.addRow(newRow());
        }
    }

    /**
     * Returns the value at the specified column and the current row.
     *
     * @param columnIndex the index
     * @return the value at current row / index.
     */
    public Double getValueAt(final int columnIndex) {
        return Double.parseDouble("" + this.getValueAt(currentRow, columnIndex));
    }

    /**
     * Sets the specified value at the specified column and current row.
     *
     * @param columnIndex the index
     * @param value the value to set
     */
    public void setValueAt(final int columnIndex, final Double value) {
        this.setValueAt(value, currentRow, columnIndex);
    }

    /**
     * Create a new table model with the specified data.
     *
     * @param data data
     */
    public TableModel(final String[][] data) {
        this.addColumn("");

        int numCols = data[0].length + 1;

        for (int i = 1; i < numCols; i++) {
            this.addColumn(Integer.toString(i));
        }

        for (int i = 0; i < data.length; i++) {
            Vector row = new Vector(data[i].length + 1);
            //row.add(0, new JButton("Send"));
            row.add(0, "Send");

            for (int j = 0; j < data[i].length; j++) {
                row.add(j + 1, Double.valueOf((String) data[i][j]));
            }

            addRow(row);
        }
    }

    /**
     * Return a new vector to be used in addRow.
     *
     * @return a new vector to be used in addRow
     */
    public Vector newRow() {
        Vector row = new Vector(this.getColumnCount());
        for (int i = 0; i < this.getColumnCount(); i++) {
            row.add(i, new Double(0));
        }

        return row;
    }

    /**
     * Fills the table with zeros.
     */
    public void zeroFill() {
        for (int i = 1; i < this.getColumnCount(); i++) {
            for (int j = 0; j < this.getRowCount(); j++) {
                this.setValueAt(new Double(0), j, i);
            }
        }
    }

    /**
     * Same as zerofill, but only fills the last column.
     */
    public void zeroFillNew() {
        for (int j = 0; j < this.getRowCount(); j++) {
            this.setValueAt(new Double(0), j, this.getColumnCount() - 1);
        }
    }

    /**
     * Clear the table.
     */
    public void removeAllRows() {
        for (int i = this.getRowCount(); i > 0; --i) {
            this.removeRow(i - 1);
        }
    }

    /**
     * Add a matrix of string data to the table, as doubles.
     *
     * @param data the matrix of string doubles to add
     */
    public void addMatrix(final String[][] data) {
        removeAllRows();

        int numCols = data[0].length + 1;
        this.addColumn("");

        for (int i = 1; i < (numCols - 1); i++) {
            this.addColumn(Integer.toString(i));
        }

        for (int i = 0; i < data.length; i++) {
            Vector row = new Vector(data[i].length + 1);
            //row.add(0, new JButton("Send"));
            row.add(0, "Send");

            for (int j = 0; j < data[i].length; j++) {
                row.add(j + 1, Double.valueOf((String) data[i][j]));
            }

            addRow(row);
        }
    }

    /**
     * Overrides superclass to provide coupling support.
     *
     * @param column passed to superclass.
     */
    public void addColumn(final String column) {
        super.addColumn(column);
        consumers.add(new ConsumingColumn(this, this.getColumnCount()));
        producers.add(new ProducingColumn(this, this.getColumnCount()));
    }

    /**
     * Remove a column at the specified point.
     *
     * @param index column to remove
     */
    public void removeColumn(final int index) {
        this.getColumnIdentifiers().remove(index);
        consumers.remove(index);
        producers.remove(index);
        for (Iterator i = this.getDataVector().iterator(); i.hasNext(); ) {
            Vector row = (Vector) i.next();
            row.remove(index);
        }
        this.zeroFill();
        this.fireTableStructureChanged();
        this.fireTableDataChanged();
    }

    /** @see DefaultTableModel */
    public boolean isCellEditable(final int row, final int column) {
        return true;
    }

    /**
     * Return a vector of column identifiers.
     *
     * @return a vector of column identifiers
     */
    public Vector getColumnIdentifiers() {
        // TODO:  returning a reference to something in superclass?
        return this.columnIdentifiers;
    }

    public List<Consumer> getConsumers() {
        return consumers;
    }

    public List<Coupling> getCouplings() {
        return couplingList;
    }

    public List<Producer> getProducers() {
        return producers;
    }

    /**
     * @return the currentRow
     */
    public int getCurrentRow() {
        return currentRow;
    }

    /**
     * @param currentRow the currentRow to set
     */
    public void setCurrentRow(int currentRow) {
        this.currentRow = currentRow;
    }
}
