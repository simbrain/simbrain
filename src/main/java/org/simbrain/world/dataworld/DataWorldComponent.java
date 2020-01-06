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

import org.simbrain.util.table.NumericTable;
import org.simbrain.workspace.*;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <b>DataWorldComponent</b> is a data table which other Simbrain components can
 * use.
 */
public class DataWorldComponent extends WorkspaceComponent implements AttributeContainer {


    /**
     * Table model.
     */
    private NumericTable dataTable;

    /**
     * List of column {@link TableColumn} object that can be used
     * to couple to the current row of a specific column.
     */
    private List<TableColumn> tableColumns = new ArrayList<>();

    /**
     * Recreates an instance of this class from a saved component.
     *
     * @param input  input stream
     * @param name   name of file
     * @param format format of file
     * @return new component
     */
    public static DataWorldComponent open(InputStream input, String name, String format) {
        // TODO: Use format to determine how to open this.
        NumericTable model = (NumericTable) NumericTable.getXStream().fromXML(input);
        return DataWorldComponent.createDataWorld(model, name);
    }

    /**
     * Create the data world component.
     *
     * @param data the underlying data
     * @param name the title for this world
     * @return the created component.
     */
    public static DataWorldComponent createDataWorld(NumericTable data, String name) {
        DataWorldComponent component = new DataWorldComponent(data, name);
        component.initializeModelListener();
        return component;
    }

    /**
     * Construct data world from a model. Used (for example) in deserializing.
     *
     * @param dataTable the underlying data
     * @param name      the name of this component
     */
    private DataWorldComponent(NumericTable dataTable, String name) {
        super(name);
        this.dataTable = dataTable;
        updateTableColumns();
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        // Workspace object is not available in the constructor.
        super.setWorkspace(workspace);

        getWorkspace().getCouplingManager().getEvents().onCouplingAdded(c -> {
            if (c.getConsumer().getBaseObject() == DataWorldComponent.this) {
                if (c.getProducer().getLabelArray() != null) {
                    dataTable.setColumnHeadings(Arrays.asList(c.getProducer().getLabelArray()));
                }
            }
        });
    }

    /**
     * Initialize consumers and producers.
     */
    private void initializeModelListener() {
        dataTable.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent evt) {
                // TODO. Below is intended to capture column inserts and
                // deletes, though it captures other stuff too.
                if (dataTable.getColumnCount() != tableColumns.size()) {
                    updateTableColumns();
                }
                setChangedSinceLastSave(true);
            }
        });
    }

    /**
     * Update the list of {@link TableColumn} objects.
     */
    private void updateTableColumns() {
        while (dataTable.getLogicalColumnCount() < tableColumns.size()) {
            tableColumns.remove(tableColumns.size() - 1);
        }
        while (dataTable.getLogicalColumnCount() > tableColumns.size()) {
            TableColumn column = new TableColumn(tableColumns.size());
            tableColumns.add(column);
        }
    }

    /**
     * Return an id for the table.
     */
    public String getId() {
        return "DataTable";
    }

    @Override
    public List<AttributeContainer> getAttributeContainers() {
        List<AttributeContainer> models = new ArrayList<>();
        models.add(this);
        models.addAll(tableColumns);
        return models;
    }

    /**
     * Returns the data model for this component.
     */
    public NumericTable getDataModel() {
        return dataTable;
    }

    @Override
    public void save(final OutputStream output, final String format) {
        NumericTable.getXStream().toXML(dataTable, output);
    }

    @Override
    public AttributeContainer getAttributeContainer(String objectKey) {
        if (objectKey.equals("DataTable")) {
            return this;
        } else {
            return null;
        }
    }

    @Override
    public void update() {
        dataTable.updateCurrentRow();
        this.getEvents().fireComponentUpdated();
    }

    @Override
    public void closing() {
    }

    @Override
    public String getXML() {
        return NumericTable.getXStream().toXML(dataTable);
    }

    @Consumable()
    public void setRow(double[] values) {
        dataTable.setVectorCurrentRow(values);
    }

    @Producible()
    public double[] getRow() {
        return dataTable.getVectorCurrentRow();
    }

    public TableColumn getTableColumn(int index) {
        return tableColumns.get(index);
    }

    /**
     * TableColumn writes to a specific column of the outer DataWorld.
     */
    public class TableColumn implements AttributeContainer {

        /**
         * The index of the column.
         */
        private int index;

        /**
         * Construct a TableColumn.
         *
         * @param index index of the column to set.
         */
        public TableColumn(int index) {
            this.index = index;
        }

        @Override
        public String getId() {
            return "Column " + (index+1);
        }

        /**
         * Return the value of the current cell of the column.
         */
        @Producible()
        public double getValue() {
            return dataTable.getValueCurrentRow(index);
        }

        /**
         * Set the value of the current cell of the column.
         */
        @Consumable()
        public void setValue(double value) {
            dataTable.setValueCurrentRow(index, value);
        }

    }
}
