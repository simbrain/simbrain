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
 */
public class TableModel implements CouplingContainer {

    /** Table Model Delegate. */
    private DefaultTableModel model;

    /** Default initial number of rows. */
    private static final int DEFAULT_ROW_COUNT = 5;

    /** Default initial number of columns. */
    private static final int DEFAULT_COLUMN_COUNT = 6;

    /** Current row. */
    private int currentRow = 0;

    /** Randomization upper bound. */
    private int upperBound = 1;

    /** Randomization lower bound. */
    private int lowerBound = 0;

    /** Iteration mode. */
    private boolean iterationMode = false;

    /** Use last column for iteration. */
    private boolean lastColumnBasedIteration = false;

    /** List of couplings. */
    private ArrayList<Coupling> couplingList = new ArrayList<Coupling>();

    /** List of consumers. */
    private ArrayList<Consumer> consumers = new ArrayList<Consumer>();

    /** List of producers. */
    private ArrayList<Producer> producers = new ArrayList<Producer>();

    /** Persistable form of matrix data. */
    private String[][] stringMatrixRepresentation;


    /**
     * Create a new table model for the specified data world.
     *
     * @param dataWorld data world
     */
    public TableModel(final DataWorld dataWorld) {

        model = new DefaultTableModel();

        for (int i = 1; i < DEFAULT_COLUMN_COUNT; i++) {
            model.addColumn(Integer.toString(i));
        }

        for (int i = 1; i < DEFAULT_ROW_COUNT; i++) {
            model.addRow(newRow());
        }
    }

    public void preSaveInit() {
        stringMatrixRepresentation = new String[getModel().getRowCount()][getModel().getColumnCount()];
                for (int i = 0; i < getModel().getRowCount(); i++) {
                    for (int j = 0; j < getModel().getColumnCount(); j++) {
                        stringMatrixRepresentation[i][j] = new String("" + getModel().getValueAt(i, j));
                    }
                }
    }

    public void postOpenInit() {
        model = new DefaultTableModel(stringMatrixRepresentation.length, stringMatrixRepresentation[0].length);
        for (int i = 0; i < stringMatrixRepresentation.length; i++) {
            for (int j = 0; j < stringMatrixRepresentation[0].length; j++) {
                model.setValueAt(stringMatrixRepresentation[i][j], i, j);
            }
        }
        Vector columnNames = new Vector();
        for (int i = 0; i < stringMatrixRepresentation[0].length; i++) {
            columnNames.add(i + 1);
        }
        model.setColumnIdentifiers(columnNames);
        consumers = new ArrayList<Consumer>();
        producers = new ArrayList<Producer>();
        couplingList = new ArrayList<Coupling>();
        for (int i = 0; i < this.getModel().getColumnCount(); i++) {
            consumers.add(new ConsumingColumn(this, i));
            producers.add(new ProducingColumn(this, i));
        }
    }
    /**
     * Returns the value at the specified column and the current row.
     *
     * @param columnIndex the index
     * @return the value at current row / index.
     */
    public Double getValueAt(final int columnIndex) {
        return Double.parseDouble("" + model.getValueAt(currentRow, columnIndex));
    }

    /**
     * Sets the specified value at the specified column and current row.
     *
     * @param columnIndex the index
     * @param value the value to set
     */
    public void setValueAt(final int columnIndex, final Double value) {
        model.setValueAt(value, currentRow, columnIndex);
    }

    /**
     * Return a new vector to be used in addRow.
     *
     * @return a new vector to be used in addRow
     */
    public Vector newRow() {
        Vector row = new Vector(model.getColumnCount());
        for (int i = 0; i < model.getColumnCount(); i++) {
            row.add(i, new Double(0));
        }
        return row;
    }

    /**
     * Fills the table with zeros.
     */
    public void zeroFill() {
        for (int i = 1; i < model.getColumnCount(); i++) {
            for (int j = 0; j < model.getRowCount(); j++) {
                model.setValueAt(new Double(0), j, i);
            }
        }
    }

    /**
     * Same as zerofill, but only fills the last column.
     */
    public void zeroFillNew() {
        for (int j = 0; j < model.getRowCount(); j++) {
            model.setValueAt(new Double(0), j, model.getColumnCount() - 1);
        }
    }

    /**
     * Clear the table.
     */
    public void removeAllRows() {
        for (int i = model.getRowCount(); i > 0; --i) {
            model.removeRow(i - 1);
        }
    }

    /**
     * Add a matrix of string data to the table, as doubles.
     *
     * @param data the matrix of string doubles to add
     */
    public void addMatrix(final String[][] data) {
        removeAllRows();

        int numCols = data[0].length;
        model.addColumn("");

        for (int i = 0; i < numCols; i++) {
            model.addColumn(Integer.toString(i));
        }

        for (int i = 0; i < data.length; i++) {
            Vector row = new Vector(data[i].length + 1);
            for (int j = 0; j < data[i].length; j++) {
                row.add(j , Double.valueOf((String) data[i][j]));
            }

            model.addRow(row);
        }
    }

    /**
     * Overrides superclass to provide coupling support.
     *
     * @param column passed to superclass.
     */
    public void addColumn(final String column) {
        model.addColumn(column);
        consumers.add(new ConsumingColumn(this, model.getColumnCount()));
        producers.add(new ProducingColumn(this, model.getColumnCount()));
        model.fireTableStructureChanged();
        model.fireTableDataChanged();
    }

    /**
     * Remove a column at the specified point.
     *
     * @param index column to remove
     */
    public void removeColumn(final int index) {
        consumers.remove(index);
        producers.remove(index);
        for (Iterator i = model.getDataVector().iterator(); i.hasNext(); ) {
            Vector row = (Vector) i.next();
            row.remove(index);
        }
        zeroFill();
        model.fireTableStructureChanged();
        model.fireTableDataChanged();
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

    /**
     * Randomizes the values.
     *
     */
    public void randomize() {

        for (int i = 0; i < model.getColumnCount(); i++) {
            for (int j = 0; j < model.getRowCount(); j++) {
                model.setValueAt(randomInteger(), j, i);
            }
        }
    }

    /**
     * @return A random integer.
     */
    public Double randomInteger() {
        if (upperBound >= lowerBound) {
            double drand = Math.random();
            drand = (drand * (upperBound - lowerBound)) + lowerBound;

            Double element = new Double(drand);

            return element;
        }

        return new Double(0);
    }

    /**
     * @return The lower bound.
     */
    public int getLowerBound() {
        return lowerBound;
    }

    /**
     * Sets the lower bound value.
     *
     * @param lowerBound Value to set
     */
    public void setLowerBound(final int lowerBound) {
        this.lowerBound = lowerBound;
    }

    /**
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
     * @return Returns the iterationMode.
     */
    public boolean isIterationMode() {
        return iterationMode;
    }

    /**
     * @param iterationMode The iterationMode to set.
     */
    public void setIterationMode(final boolean iterationMode) {
        this.iterationMode = iterationMode;
    }

    /**
     * @return Returns the columnIteration.
     */
    public boolean isLastColumnBasedIteration() {
        return lastColumnBasedIteration;
    }

    /**
     * @param columnIteration The columnIteration to set.
     */
    public void setLastColumnBasedIteration(final boolean columnIteration) {
        lastColumnBasedIteration = columnIteration;
    }

    /**
     * @return the tableModel
     */
    public DefaultTableModel getModel() {
        return model;
    }

}
